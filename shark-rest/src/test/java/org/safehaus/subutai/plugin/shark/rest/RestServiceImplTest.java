package org.safehaus.subutai.plugin.shark.rest;


import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.safehaus.subutai.common.tracker.OperationState;
import org.safehaus.subutai.common.tracker.TrackerOperationView;
import org.safehaus.subutai.common.util.JsonUtil;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.shark.api.Shark;
import org.safehaus.subutai.plugin.shark.api.SharkClusterConfig;
import org.safehaus.subutai.plugin.spark.api.Spark;
import org.safehaus.subutai.plugin.spark.api.SparkClusterConfig;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;


@RunWith( MockitoJUnitRunner.class )
public class RestServiceImplTest
{
    private RestServiceImpl restService;
    @Mock
    Shark shark;
    @Mock
    Tracker tracker;
    @Mock
    SharkClusterConfig sharkClusterConfig;
    @Mock
    TrackerOperationView trackerOperationView;
    @Mock
    Spark sparkManager;
    @Mock
    SparkClusterConfig sparkClusterConfig;


    @Before
    public void setUp()
    {

        sharkClusterConfig = new SharkClusterConfig();
        sparkClusterConfig = new SparkClusterConfig();
        restService = new RestServiceImpl( shark );
        restService.setTracker( tracker );
        restService.setSparkManager( sparkManager );
        when( shark.getCluster( anyString() )).thenReturn( sharkClusterConfig );
        when( tracker.getTrackerOperation( anyString(), any( UUID.class ) ) ).thenReturn( trackerOperationView );
        when( trackerOperationView.getState() ).thenReturn( OperationState.SUCCEEDED );
        List<UUID> myList = Lists.newArrayList();
        myList.add( UUID.randomUUID() );
        sparkClusterConfig.getAllNodesIds().addAll( myList );
        when( sparkManager.getCluster( anyString() )).thenReturn( sparkClusterConfig );

    }


    @Test
    public void testListClusters()
    {
        List<SharkClusterConfig> myList = Lists.newArrayList();
        myList.add( sharkClusterConfig );
        when( shark.getClusters() ).thenReturn( myList );

        Response response = restService.listClusters();

        // assertions
        assertEquals( Response.Status.OK.getStatusCode(), response.getStatus() );
    }


    @Test
    public void testGetCluster()
    {
        Response response = restService.getCluster( "test" );

        assertEquals( Response.Status.OK.getStatusCode(), response.getStatus() );
    }


    @Test
    public void testInstallCluster()
    {
        Response response = restService.installCluster( "test", "test" );

        // assertions
        assertEquals( Response.Status.OK.getStatusCode(), response.getStatus() );
    }


    @Test
    public void testUninstallCluster()
    {
        Response response = restService.uninstallCluster( "test" );

        // assertions
        assertEquals( Response.Status.OK.getStatusCode(), response.getStatus() );
    }


    @Test
    public void testAddNode()
    {
        Response response = restService.addNode( "test", "test" );

        // assertions
        assertEquals( Response.Status.OK.getStatusCode(), response.getStatus() );
    }


    @Test
    public void testDestroyNode()
    {
        Response response = restService.destroyNode( "test", "test" );

        // assertions
        assertEquals( Response.Status.OK.getStatusCode(), response.getStatus() );
    }


    @Test
    public void testActualizeMasterIP()
    {
        Response response = restService.actualizeMasterIP( "test" );

        // assertions
        assertEquals( Response.Status.OK.getStatusCode(), response.getStatus() );
    }
}