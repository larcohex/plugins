package io.subutai.plugin.shark.impl;


import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.tracker.TrackerOperation;
import io.subutai.common.util.CollectionUtil;
import io.subutai.core.plugincommon.api.ClusterException;
import io.subutai.core.plugincommon.api.ClusterSetupException;
import io.subutai.core.plugincommon.api.ClusterSetupStrategy;
import io.subutai.core.plugincommon.api.ConfigBase;
import io.subutai.plugin.shark.api.SharkClusterConfig;
import io.subutai.plugin.spark.api.SparkClusterConfig;


public class SetupStrategyOverSpark implements ClusterSetupStrategy
{

    private final Environment environment;
    private final SharkImpl manager;
    private final SharkClusterConfig config;
    private final TrackerOperation trackerOperation;

    private SparkClusterConfig sparkConfig;
    private EnvironmentContainerHost sparkMaster;
    private Set<EnvironmentContainerHost> allNodes;
    private Set<EnvironmentContainerHost> nodesToInstallShark;


    public SetupStrategyOverSpark( Environment environment, SharkImpl manager, SharkClusterConfig config,
                                   TrackerOperation trackerOperation )
    {

        Preconditions.checkNotNull( environment );
        Preconditions.checkNotNull( manager );
        Preconditions.checkNotNull( config );
        Preconditions.checkNotNull( trackerOperation );

        this.environment = environment;
        this.manager = manager;
        this.config = config;
        this.trackerOperation = trackerOperation;
    }


    private void check() throws ClusterSetupException
    {

        if ( manager.getCluster( config.getClusterName() ) != null )
        {
            throw new ClusterSetupException( String.format( "Cluster %s already exists", config.getClusterName() ) );
        }
        sparkConfig = manager.getSparkManager().getCluster( config.getSparkClusterName() );
        if ( sparkConfig == null )
        {
            throw new ClusterSetupException(
                    String.format( "Underlying Spark cluster '%s' not found.", config.getSparkClusterName() ) );
        }


        if ( CollectionUtil.isCollectionEmpty( config.getNodeIds() ) )
        {
            throw new ClusterSetupException( "Invalid Shark node ids" );
        }

        final Set<EnvironmentContainerHost> sparkSlaves;
        try
        {
            sparkSlaves = environment.getContainerHostsByIds( Sets.newHashSet( sparkConfig.getSlaveIds() ) );
        }
        catch ( ContainerHostNotFoundException e )
        {
            throw new ClusterSetupException( "Spark slave nodes not found" );
        }


        if ( sparkSlaves.size() < sparkConfig.getSlaveIds().size() )
        {
            throw new ClusterSetupException( "Fewer Spark nodes found in environment than exist in Spark cluster" );
        }

        for ( EnvironmentContainerHost slave : sparkSlaves )
        {
            if ( !slave.isConnected() )
            {
                throw new ClusterSetupException(
                        String.format( "Container %s is not connected", slave.getHostname() ) );
            }
        }

        try
        {
            sparkMaster = environment.getContainerHostById( sparkConfig.getMasterNodeId() );
        }
        catch ( ContainerHostNotFoundException e )
        {
            throw new ClusterSetupException( "Spark master not found" );
        }


        if ( !sparkMaster.isConnected() )
        {
            throw new ClusterSetupException( "Spark master is not connected" );
        }

        allNodes = Sets.newHashSet( sparkSlaves );
        allNodes.add( sparkMaster );

        //check if node belongs to some existing spark cluster
        List<SharkClusterConfig> sparkClusters = manager.getClusters();
        for ( EnvironmentContainerHost node : allNodes )
        {
            for ( SharkClusterConfig cluster : sparkClusters )
            {
                if ( cluster.getNodeIds().contains( node.getId() ) )
                {
                    throw new ClusterSetupException(
                            String.format( "Node %s already belongs to Shark cluster %s", node.getHostname(),
                                    cluster.getClusterName() ) );
                }
            }
        }


        nodesToInstallShark = Sets.newHashSet();

        trackerOperation.addLog( "Checking prerequisites..." );
        RequestBuilder checkCommand = manager.getCommands().getCheckInstalledCommand();
        for ( EnvironmentContainerHost node : allNodes )
        {

            CommandResult result = executeCommand( node, checkCommand );
            if ( !result.getStdOut().contains( Commands.PACKAGE_NAME ) )
            {
                nodesToInstallShark.add( node );
            }
        }
    }


    private void configure() throws ClusterSetupException
    {

        if ( !nodesToInstallShark.isEmpty() )
        {
            trackerOperation.addLog( "Installing Shark..." );

            //install shark
            RequestBuilder installCommand = manager.getCommands().getInstallCommand();
            for ( EnvironmentContainerHost node : nodesToInstallShark )
            {
                executeCommand( node, installCommand );
            }
        }


        trackerOperation.addLog( "Setting master IP..." );

        RequestBuilder setMasterIpCommand = manager.getCommands().getSetMasterIPCommand( sparkMaster );
        for ( EnvironmentContainerHost node : allNodes )
        {
            executeCommand( node, setMasterIpCommand );
        }

        trackerOperation.addLog( "Saving cluster info..." );

        config.getNodeIds().clear();
        config.getNodeIds().addAll( sparkConfig.getAllNodesIds() );
        config.setEnvironmentId( environment.getId() );

        try
        {
            manager.saveConfig( config );
        }
        catch ( ClusterException e )
        {
            throw new ClusterSetupException( e );
        }
    }


    @Override
    public ConfigBase setup() throws ClusterSetupException
    {
        check();

        configure();


        return config;
    }


    public CommandResult executeCommand( EnvironmentContainerHost host, RequestBuilder command )
            throws ClusterSetupException
    {

        CommandResult result;
        try
        {
            result = host.execute( command );
        }
        catch ( CommandException e )
        {
            throw new ClusterSetupException( e );
        }
        if ( !result.hasSucceeded() )
        {
            throw new ClusterSetupException( String.format( "Error on container %s: %s", host.getHostname(),
                    result.hasCompleted() ? result.getStdErr() : "Command timed out" ) );
        }
        return result;
    }
}

