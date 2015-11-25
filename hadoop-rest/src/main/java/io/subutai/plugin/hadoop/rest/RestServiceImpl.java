package io.subutai.plugin.hadoop.rest;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Response;

import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.ContainerHost;
import io.subutai.common.tracker.OperationState;
import io.subutai.common.tracker.TrackerOperationView;
import io.subutai.common.util.CollectionUtil;
import io.subutai.common.util.JsonUtil;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.common.api.ClusterException;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.hadoop.api.HadoopClusterConfig;
import io.subutai.plugin.hadoop.rest.pojo.ContainerPojo;
import io.subutai.plugin.hadoop.rest.pojo.HadoopPojo;


public class RestServiceImpl implements RestService
{

    private Hadoop hadoopManager;
    private Tracker tracker;
    private EnvironmentManager environmentManager;


    @Override
    public Response listClusters()
    {
        List<HadoopClusterConfig> hadoopClusterConfigList = hadoopManager.getClusters();
        ArrayList<String> clusterNames = new ArrayList<>();

        for ( HadoopClusterConfig hadoopClusterConfig : hadoopClusterConfigList )
        {
            clusterNames.add( hadoopClusterConfig.getClusterName() );
        }


        String clusters = JsonUtil.GSON.toJson( clusterNames );
        return Response.status( Response.Status.OK ).entity( clusters ).build();
    }


    @Override
    public Response getCluster( final String clusterName )
    {
        HadoopClusterConfig config = hadoopManager.getCluster( clusterName );

        String cluster = JsonUtil.GSON.toJson( updateStatus( config ) );

        return Response.status( Response.Status.OK ).entity( cluster ).build();
    }


    @Override
    public Response configureCluster( final String config )
    {
        TrimmedHadoopConfig trimmedHadoopConfig = JsonUtil.fromJson( config, TrimmedHadoopConfig.class );
        HadoopClusterConfig hadoopConfig = new HadoopClusterConfig();
        hadoopConfig.setClusterName( trimmedHadoopConfig.getClusterName() );
        hadoopConfig.setDomainName( trimmedHadoopConfig.getDomainName() );
        hadoopConfig.setEnvironmentId( trimmedHadoopConfig.getEnvironmentId() );
        hadoopConfig.setJobTracker( trimmedHadoopConfig.getJobTracker() );
        hadoopConfig.setNameNode( trimmedHadoopConfig.getNameNode() );
        hadoopConfig.setSecondaryNameNode( trimmedHadoopConfig.getSecNameNode() );

        if ( !CollectionUtil.isCollectionEmpty( trimmedHadoopConfig.getSlaves() ) )
        {
            Set<String> slaveNodes = new HashSet<>();
            for ( String node : trimmedHadoopConfig.getSlaves() )
            {
                slaveNodes.add( node );
            }
            hadoopConfig.getDataNodes().addAll( slaveNodes );
            hadoopConfig.getTaskTrackers().addAll( slaveNodes );
        }

        UUID uuid = hadoopManager.installCluster( hadoopConfig );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response uninstallCluster( String clusterName )
    {
        UUID uuid = hadoopManager.uninstallCluster( clusterName );

        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response startNameNode( String clusterName )
    {
        HadoopClusterConfig hadoopClusterConfig = hadoopManager.getCluster( clusterName );
        UUID uuid = hadoopManager.startNameNode( hadoopClusterConfig );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response stopNameNode( String clusterName )
    {
        HadoopClusterConfig hadoopClusterConfig = hadoopManager.getCluster( clusterName );
        UUID uuid = hadoopManager.stopNameNode( hadoopClusterConfig );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response statusNameNode( String clusterName )
    {
        HadoopClusterConfig hadoopClusterConfig = hadoopManager.getCluster( clusterName );
        UUID uuid = hadoopManager.statusNameNode( hadoopClusterConfig );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response statusSecondaryNameNode( String clusterName )
    {
        HadoopClusterConfig hadoopClusterConfig = hadoopManager.getCluster( clusterName );
        UUID uuid = hadoopManager.statusSecondaryNameNode( hadoopClusterConfig );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response startJobTracker( String clusterName )
    {
        HadoopClusterConfig hadoopClusterConfig = hadoopManager.getCluster( clusterName );
        UUID uuid = hadoopManager.startJobTracker( hadoopClusterConfig );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response stopJobTracker( String clusterName )
    {
        HadoopClusterConfig hadoopClusterConfig = hadoopManager.getCluster( clusterName );
        UUID uuid = hadoopManager.stopJobTracker( hadoopClusterConfig );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response statusJobTracker( String clusterName )
    {
        HadoopClusterConfig hadoopClusterConfig = hadoopManager.getCluster( clusterName );
        UUID uuid = hadoopManager.statusJobTracker( hadoopClusterConfig );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response addNode( String clusterName )
    {
        UUID uuid = hadoopManager.addNode( clusterName );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response statusDataNode( String clusterName, String hostname )
    {
        UUID uuid = hadoopManager.statusDataNode( hadoopManager.getCluster( clusterName ), hostname );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response statusTaskTracker( String clusterName, String hostname )
    {
        HadoopClusterConfig hadoopClusterConfig = hadoopManager.getCluster( clusterName );
        UUID uuid = hadoopManager.statusTaskTracker( hadoopClusterConfig, hostname );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response autoScaleCluster( final String clusterName, final boolean scale )
    {
        String message = "enabled";
        HadoopClusterConfig config = hadoopManager.getCluster( clusterName );
        config.setAutoScaling( scale );
        try
        {
            hadoopManager.saveConfig( config );
        }
        catch ( ClusterException e )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( "Auto scale cannot set successfully" ).build();
        }
        if ( !scale )
        {
            message = "disabled";
        }

        return Response.status( Response.Status.OK ).entity( "Auto scale is " + message + " successfully" ).build();
    }


    public Hadoop getHadoopManager()
    {
        return hadoopManager;
    }


    public void setHadoopManager( Hadoop hadoopManager )
    {
        this.hadoopManager = hadoopManager;
    }


    public Tracker getTracker()
    {
        return tracker;
    }


    public void setTracker( final Tracker tracker )
    {
        this.tracker = tracker;
    }


    public EnvironmentManager getEnvironmentManager()
    {
        return environmentManager;
    }


    public void setEnvironmentManager( final EnvironmentManager environmentManager )
    {
        this.environmentManager = environmentManager;
    }


    private Response createResponse( UUID uuid, OperationState state )
    {
        TrackerOperationView po = tracker.getTrackerOperation( HadoopClusterConfig.PRODUCT_NAME, uuid );
        if ( state == OperationState.FAILED )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( po.getLog() ).build();
        }
        else if ( state == OperationState.SUCCEEDED )
        {
            return Response.status( Response.Status.OK ).entity( po.getLog() ).build();
        }
        else
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( "Timeout" ).build();
        }
    }


    private OperationState waitUntilOperationFinish( UUID uuid )
    {
        OperationState state = null;
        long start = System.currentTimeMillis();
        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( HadoopClusterConfig.PRODUCT_NAME, uuid );
            if ( po != null )
            {
                if ( po.getState() != OperationState.RUNNING )
                {
                    state = po.getState();
                    break;
                }
            }
            try
            {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException ex )
            {
                break;
            }
            if ( System.currentTimeMillis() - start > ( 60 * 1000 ) )
            {
                break;
            }
        }
        return state;
    }


    private HadoopPojo updateStatus( HadoopClusterConfig config )
    {
        HadoopPojo pojo = new HadoopPojo( config );

        try
        {
            Environment environment = environmentManager.loadEnvironment( config.getEnvironmentId() );

            UUID uuid = hadoopManager.statusNameNode( config );
            ContainerHost ch = environment.getContainerHostById( pojo.getNameNode().getUuid() );
            pojo.getNameNode().setHostname( ch.getHostname() );
            pojo.getNameNode().setIp( ch.getIpByInterfaceName( "eth0" ) );
            pojo.getNameNode().setStatus( parseStatus( uuid ) );

            uuid = hadoopManager.statusSecondaryNameNode( config );
            ch = environment.getContainerHostById( pojo.getSecondaryNameNode().getUuid() );
            pojo.getSecondaryNameNode().setHostname( ch.getHostname() );
            pojo.getSecondaryNameNode().setIp( ch.getIpByInterfaceName( "eth0" ) );
            pojo.getSecondaryNameNode().setStatus( parseStatus( uuid ) );

            uuid = hadoopManager.statusJobTracker( config );
            ch = environment.getContainerHostById( pojo.getJobTracker().getUuid() );
            pojo.getJobTracker().setHostname( ch.getHostname() );
            pojo.getJobTracker().setIp( ch.getIpByInterfaceName( "eth0" ) );
            pojo.getJobTracker().setStatus( parseStatus( uuid ) );

            for ( ContainerPojo container : pojo.getAllDataNodeAgent() )
            {
                ch = environment.getContainerHostById( container.getUuid() );
                container.setHostname( ch.getHostname() );
                container.setIp( ch.getIpByInterfaceName( "eth0" ) );

                uuid = hadoopManager.statusDataNode( config, container.getHostname() );
                container.setStatus( parseStatus( uuid ) );
            }

            for ( ContainerPojo container : pojo.getAllTaskTrackerNodeAgents() )
            {
                ch = environment.getContainerHostById( container.getUuid() );
                container.setHostname( ch.getHostname() );
                container.setIp( ch.getIpByInterfaceName( "eth0" ) );

                uuid = hadoopManager.statusTaskTracker( config, container.getHostname() );
                container.setStatus( parseStatus( uuid ) );
            }
        }
        catch ( ContainerHostNotFoundException | EnvironmentNotFoundException e )
        {
            e.printStackTrace();
        }

        return pojo;
    }


    private String parseStatus( UUID uuid )
    {
        String log = "";
        long start = System.currentTimeMillis();
        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( HadoopClusterConfig.PRODUCT_NAME, uuid );
            if ( po != null )
            {
                if ( po.getState() != OperationState.RUNNING )
                {
                    String temp = po.getLog();
                    if ( temp.contains( "STOPPED" ) )
                    {
                        log = "STOPPED";
                    }
                    else if ( temp.contains( "RUNNING" ) )
                    {
                        log = "RUNNING";
                    }
                    else
                    {
                        log = "UNKNOWN";
                    }
                    break;
                }
            }
            try
            {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException ex )
            {
                break;
            }
            if ( System.currentTimeMillis() - start > ( 60 * 1000 ) )
            {
                break;
            }
        }
        return log;
    }
}
