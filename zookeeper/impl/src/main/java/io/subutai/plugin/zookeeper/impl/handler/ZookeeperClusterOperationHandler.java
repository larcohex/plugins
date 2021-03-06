package io.subutai.plugin.zookeeper.impl.handler;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.CommandUtil;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.ContainerHost;
import io.subutai.common.peer.Host;
import io.subutai.core.environment.api.exception.EnvironmentDestructionException;
import io.subutai.core.plugincommon.api.ClusterException;
import io.subutai.core.plugincommon.api.ClusterOperationHandlerInterface;
import io.subutai.core.plugincommon.api.ClusterOperationType;
import io.subutai.core.plugincommon.api.ClusterSetupException;
import io.subutai.core.plugincommon.api.ClusterSetupStrategy;
import io.subutai.plugin.zookeeper.api.SetupType;
import io.subutai.plugin.zookeeper.api.ZookeeperClusterConfig;
import io.subutai.plugin.zookeeper.impl.Commands;
import io.subutai.plugin.zookeeper.impl.ZookeeperImpl;
import io.subutai.plugin.zookeeper.impl.ZookeeperOverHadoopSetupStrategy;


/**
 * This class handles operations that are related to whole cluster.
 */
public class ZookeeperClusterOperationHandler
        extends AbstractPluginOperationHandler<ZookeeperImpl, ZookeeperClusterConfig>
        implements ClusterOperationHandlerInterface
{
    private static final Logger LOG = LoggerFactory.getLogger( ZookeeperClusterOperationHandler.class.getName() );
    private ClusterOperationType operationType;
    private ZookeeperClusterConfig zookeeperClusterConfig;
    private CommandUtil commandUtil;


    public ZookeeperClusterOperationHandler( final ZookeeperImpl manager, final ZookeeperClusterConfig config,
                                             final ClusterOperationType operationType )
    {
        super( manager, config );
        this.operationType = operationType;
        this.zookeeperClusterConfig = config;
        trackerOperation = manager.getTracker().createTrackerOperation( config.getProductKey(),
                String.format( "Running %s operation on %s...", operationType, clusterName ) );
        this.commandUtil = new CommandUtil();
    }


    public ZookeeperClusterOperationHandler( final ZookeeperImpl manager,
                                             final ZookeeperClusterConfig zookeeperClusterConfig, final String hostName,
                                             final ClusterOperationType operationType )
    {
        super( manager, zookeeperClusterConfig );
        this.operationType = operationType;
        this.zookeeperClusterConfig = zookeeperClusterConfig;
        trackerOperation = manager.getTracker().createTrackerOperation( zookeeperClusterConfig.getProductKey(),
                String.format( "Running %s operation on %s...", operationType, clusterName ) );
        this.commandUtil = new CommandUtil();
    }


    public void run()
    {
        Preconditions.checkNotNull( zookeeperClusterConfig, "Configuration is null !!!" );
        runOperationOnContainers( operationType );
    }


    @Override
    public void runOperationOnContainers( ClusterOperationType clusterOperationType )
    {
        Environment environment;
        List<CommandResult> commandResultList = new ArrayList<>();
        try
        {
            switch ( clusterOperationType )
            {
                case INSTALL:
                    setupCluster();
                    break;
                case UNINSTALL:
                    destroyCluster();
                    break;
                case START_ALL:
                    environment = manager.getEnvironmentManager()
                                         .loadEnvironment( zookeeperClusterConfig.getEnvironmentId() );
                    for ( ContainerHost containerHost : environment.getContainerHosts() )
                    {
                        if ( config.getNodes().contains( containerHost.getId() ) )
                        {
                            commandResultList.add( executeCommand( containerHost, Commands.getStartCommand() ) );
                        }
                    }
                    break;
                case STOP_ALL:
                    environment = manager.getEnvironmentManager()
                                         .loadEnvironment( zookeeperClusterConfig.getEnvironmentId() );
                    for ( ContainerHost containerHost : environment.getContainerHosts() )
                    {
                        if ( config.getNodes().contains( containerHost.getId() ) )
                        {
                            commandResultList.add( executeCommand( containerHost, Commands.getStopCommand() ) );
                        }
                    }
                    break;
                case STATUS_ALL:
                    environment = manager.getEnvironmentManager()
                                         .loadEnvironment( zookeeperClusterConfig.getEnvironmentId() );
                    for ( ContainerHost containerHost : environment.getContainerHosts() )
                    {
                        if ( config.getNodes().contains( containerHost.getId() ) )
                        {
                            commandResultList.add( executeCommand( containerHost, Commands.getStatusCommand() ) );
                        }
                    }
                    break;
            }
        }
        catch ( EnvironmentNotFoundException e )
        {
            trackerOperation.addLogFailed(
                    String.format( "Environment with id: %s not found", zookeeperClusterConfig.getEnvironmentId() ) );
        }
        logResults( trackerOperation, commandResultList );
    }


    @Override
    public void setupCluster()
    {
        try
        {
            if ( Strings.isNullOrEmpty( zookeeperClusterConfig.getClusterName() ) )
            {
                trackerOperation.addLogFailed( "Malformed configuration" );
                return;
            }

            if ( manager.getCluster( clusterName ) != null )
            {
                trackerOperation.addLogFailed( String.format( "Cluster with name '%s' already exists", clusterName ) );
                return;
            }

            try
            {
                Environment env;
                try
                {
                    env = manager.getEnvironmentManager().loadEnvironment( zookeeperClusterConfig.getEnvironmentId() );
                }
                catch ( EnvironmentNotFoundException e )
                {
                    throw new ClusterException( String.format( "Could not find environment of Hadoop cluster by id %s",
                            zookeeperClusterConfig.getEnvironmentId() ) );
                }

                ClusterSetupStrategy clusterSetupStrategy =
                        manager.getClusterSetupStrategy( env, zookeeperClusterConfig, trackerOperation );
                clusterSetupStrategy.setup();

                trackerOperation.addLogDone( String.format( "Cluster %s set up successfully", clusterName ) );
            }
            catch ( ClusterSetupException e )
            {
                trackerOperation.addLogFailed(
                        String.format( "Failed to setup %s cluster %s : %s", zookeeperClusterConfig.getProductKey(),
                                clusterName, e.getMessage() ) );
                LOG.error( String.format( "Failed to setup %s cluster %s : %s", zookeeperClusterConfig.getProductKey(),
                        clusterName, e.getMessage() ), e );
            }
        }
        catch ( ClusterException e )
        {
            LOG.error( "Error in setupCluster", e );
            trackerOperation.addLogFailed( String.format( "Failed to setup cluster : %s", e.getMessage() ) );
        }
    }


    @Override
    public void destroyCluster()
    {
        ZookeeperClusterConfig config = manager.getCluster( clusterName );
        if ( config == null )
        {
            trackerOperation.addLogFailed(
                    String.format( "Cluster with name %s does not exist. Operation aborted", clusterName ) );
            return;
        }

        // stop all nodes before removing zookeeper
        manager.stopAllNodes( clusterName );

        try
        {
            if ( config.getSetupType() == SetupType.OVER_HADOOP || config.getSetupType() == SetupType.OVER_ENVIRONMENT )
            {
                trackerOperation.addLog( "Uninstalling zookeeper from nodes" );
                Environment zookeeperEnvironment =
                        manager.getEnvironmentManager().loadEnvironment( config.getEnvironmentId() );

                Set<Host> hostSet =
                        ZookeeperOverHadoopSetupStrategy.getHosts( config.getNodes(), zookeeperEnvironment );

                CommandUtil.HostCommandResults results = commandUtil
                        .executeParallel( new RequestBuilder( Commands.getUninstallCommand() ), hostSet );
                Set <CommandUtil.HostCommandResult> resultSet = results.getCommandResults();
                Map<Host, CommandResult> resultMap = Maps.newConcurrentMap();
                for ( CommandUtil.HostCommandResult result : resultSet)
                {
                    resultMap.put (result.getHost(), result.getCommandResult());
                }
                if ( ZookeeperOverHadoopSetupStrategy.isAllSuccessful( resultMap, hostSet ) )
                {
                    trackerOperation.addLog( "Zookeeper is uninstalled from all containers successfully" );
                }
            }
            else
            {
                trackerOperation.addLog( "Destroying environment..." );
                manager.getEnvironmentManager().destroyEnvironment( config.getEnvironmentId(), true );
            }

            manager.getPluginDAO().deleteInfo( config.getProductKey(), config.getClusterName() );
            trackerOperation.addLogDone( "Cluster destroyed" );
        }
        catch ( EnvironmentDestructionException | EnvironmentNotFoundException e )
        {
            trackerOperation.addLogFailed( String.format( "Error running command, %s", e.getMessage() ) );
            LOG.error( e.getMessage(), e );
        }
    }
}
