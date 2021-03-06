package io.subutai.plugin.sqoop.rest;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import io.subutai.common.tracker.OperationState;
import io.subutai.common.tracker.TrackerOperationView;
import io.subutai.common.util.JsonUtil;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.sqoop.api.DataSourceType;
import io.subutai.plugin.sqoop.api.Sqoop;
import io.subutai.plugin.sqoop.api.SqoopConfig;
import io.subutai.plugin.sqoop.api.setting.ExportSetting;
import io.subutai.plugin.sqoop.api.setting.ImportSetting;


public class RestServiceImpl implements RestService
{
    private static final String OPERATION_ID = "OPERATION_ID";
    private Tracker tracker;
    private Sqoop sqoopManager;


    public void setSqoopManager( Sqoop sqoopManager )
    {
        this.sqoopManager = sqoopManager;
    }


    public Tracker getTracker()
    {
        return tracker;
    }


    public void setTracker( final Tracker tracker )
    {
        this.tracker = tracker;
    }


    public Response getClusters()
    {
        List<SqoopConfig> configs = sqoopManager.getClusters();
        ArrayList<String> clusterNames = Lists.newArrayList();

        for ( SqoopConfig config : configs )
        {
            clusterNames.add( config.getClusterName() );
        }

        String clusters = JsonUtil.GSON.toJson( clusterNames );
        return Response.status( Response.Status.OK ).entity( clusters ).build();
    }


    public Response getCluster( String clusterName )
    {
        SqoopConfig config = sqoopManager.getCluster( clusterName );
        if ( config == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( clusterName + "cluster not found" )
                           .build();
        }

        String cluster = JsonUtil.GSON.toJson( config );
        return Response.status( Response.Status.OK ).entity( cluster ).build();
    }


    public Response installCluster( String clusterName, String hadoopClusterName, String nodes )
    {
        Set<String> uuidSet = new HashSet<>();
        SqoopConfig config = new SqoopConfig();
        config.setClusterName( clusterName );
        config.setHadoopClusterName( hadoopClusterName );

        String[] arr = nodes.replaceAll( "\\s+", "" ).split( "," );
        Collections.addAll( uuidSet, arr );
        config.setNodes( uuidSet );

        UUID uuid = sqoopManager.installCluster( config );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response uninstallCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( sqoopManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = sqoopManager.uninstallCluster( clusterName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    public Response destroyNode( String clusterName, String hostName )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( hostName );
        if ( sqoopManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = sqoopManager.destroyNode( clusterName, hostName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    public Response importData( String config )
    {
        TrimmedImportSettings trimmedImportSettings = JsonUtil.fromJson( config, TrimmedImportSettings.class );
        ImportSetting importSettings = new ImportSetting();
        importSettings.setClusterName( trimmedImportSettings.getClusterName() );
        importSettings.setPassword( trimmedImportSettings.getPassword() );
        importSettings.setUsername( trimmedImportSettings.getUsername() );
        importSettings.setConnectionString( trimmedImportSettings.getConnectionString() );
        importSettings.setType( DataSourceType.valueOf( trimmedImportSettings.getDataSourceType() ) );
        importSettings.setTableName( trimmedImportSettings.getTableName() );
        importSettings.setHostname( trimmedImportSettings.getHostname() );
        UUID uuid = sqoopManager.importData( importSettings );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    public Response exportData( String config )
    {
        TrimmedExportSettings trimmedExportSettings = JsonUtil.fromJson( config, TrimmedExportSettings.class );
        ExportSetting exportSetting = new ExportSetting();
        exportSetting.setClusterName( trimmedExportSettings.getClusterName() );
        exportSetting.setPassword( trimmedExportSettings.getPassword() );
        exportSetting.setUsername( trimmedExportSettings.getUsername() );
        exportSetting.setConnectionString( trimmedExportSettings.getConnectionString() );
        exportSetting.setTableName( trimmedExportSettings.getTableName() );
        exportSetting.setHostname( trimmedExportSettings.getHostname() );
        exportSetting.setHdfsPath( trimmedExportSettings.getHdfsFilePath() );
        UUID uuid = sqoopManager.exportData( exportSetting );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    private OperationState waitUntilOperationFinish( UUID uuid )
    {
        OperationState state = null;
        long start = System.currentTimeMillis();
        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( SqoopConfig.PRODUCT_KEY, uuid );
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
            if ( System.currentTimeMillis() - start > ( 90 * 1000 ) )
            {
                break;
            }
        }
        return state;
    }


    private Response createResponse( UUID uuid, OperationState state )
    {
        TrackerOperationView po = tracker.getTrackerOperation( SqoopConfig.PRODUCT_KEY, uuid );
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
}
