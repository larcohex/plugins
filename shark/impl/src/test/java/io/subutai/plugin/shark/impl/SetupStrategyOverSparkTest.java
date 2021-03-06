package io.subutai.plugin.shark.impl;


import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.environment.Environment;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.settings.Common;
import io.subutai.common.tracker.TrackerOperation;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.core.plugincommon.api.ClusterException;
import io.subutai.core.plugincommon.api.ClusterSetupException;
import io.subutai.core.plugincommon.api.PluginDAO;
import io.subutai.plugin.shark.api.SharkClusterConfig;
import io.subutai.plugin.spark.api.Spark;
import io.subutai.plugin.spark.api.SparkClusterConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class SetupStrategyOverSparkTest
{
    SetupStrategyOverSpark setupStrategyOverSpark;
    private SharkImpl sharkImpl;
    private SharkClusterConfig sharkClusterConfig;
    private Tracker tracker;
    private TrackerOperation trackerOperation;
    private EnvironmentManager environmentManager;
    private Environment environment;
    private EnvironmentContainerHost containerHost;
    private RequestBuilder requestBuilder;
    private CommandResult commandResult;
    private String id;
    private Spark spark;
    private SparkClusterConfig sparkClusterConfig;
    private Commands commands;
    private PluginDAO pluginDAO;
    public static final String PACKAGE_NAME = Common.PACKAGE_PREFIX + SharkClusterConfig.PRODUCT_KEY.toLowerCase();


    @Before
    public void setUp()
    {
        pluginDAO = mock( PluginDAO.class );
        commands = mock( Commands.class );
        sparkClusterConfig = mock( SparkClusterConfig.class );
        spark = mock( Spark.class );
        id = UUID.randomUUID().toString();
        commandResult = mock( CommandResult.class );
        requestBuilder = mock( RequestBuilder.class );
        containerHost = mock( EnvironmentContainerHost.class );
        environment = mock( Environment.class );
        environmentManager = mock( EnvironmentManager.class );
        trackerOperation = mock( TrackerOperation.class );
        tracker = mock( Tracker.class );
        sharkImpl = mock( SharkImpl.class );
        sharkClusterConfig = mock( SharkClusterConfig.class );
        when( sharkImpl.getTracker() ).thenReturn( tracker );
        when( tracker.createTrackerOperation( anyString(), anyString() ) ).thenReturn( trackerOperation );

        setupStrategyOverSpark =
                new SetupStrategyOverSpark( environment, sharkImpl, sharkClusterConfig, trackerOperation );
    }


    @Test
    public void testSetup() throws Exception
    {
        // mock check method
        when( sharkImpl.getSparkManager() ).thenReturn( spark );
        when( spark.getCluster( anyString() ) ).thenReturn( sparkClusterConfig );

        Set<EnvironmentContainerHost> mySet = mock( Set.class );
        mySet.add( containerHost );
        when( containerHost.getId() ).thenReturn( id );

        EnvironmentContainerHost[] arr = new EnvironmentContainerHost[1];
        arr[0] = containerHost;

        when( environment.getContainerHostsByIds( any( Set.class ) ) ).thenReturn( mySet );
        Iterator<EnvironmentContainerHost> iterator = mock( Iterator.class );
        when( mySet.iterator() ).thenReturn( iterator );
        when( iterator.hasNext() ).thenReturn( true ).thenReturn( false );
        when( iterator.next() ).thenReturn( containerHost );
        when( mySet.size() ).thenReturn( 1 );

        when( containerHost.isConnected() ).thenReturn( true );
        when( environment.getContainerHostById( any( String.class ) ) ).thenReturn( containerHost );
        when( mySet.toArray() ).thenReturn( arr );

        when( sharkImpl.getCommands() ).thenReturn( commands );
        when( commands.getCheckInstalledCommand() ).thenReturn( requestBuilder );
        when( containerHost.execute( requestBuilder ) ).thenReturn( commandResult );
        when( commandResult.hasSucceeded() ).thenReturn( true );
        setupStrategyOverSpark.executeCommand( containerHost, requestBuilder );
        when( commandResult.getStdOut() ).thenReturn( "test" );

        // mock configure method
        Set<String> myUUID = mock( Set.class );
        myUUID.add( id );
        when( sharkClusterConfig.getNodeIds() ).thenReturn( myUUID );
        when( myUUID.addAll( anyListOf( String.class ) ) ).thenReturn( true );
        when( environment.getId() ).thenReturn( id );
        when( commands.getSetMasterIPCommand( containerHost ) ).thenReturn( requestBuilder );
        when( sharkImpl.getPluginDao() ).thenReturn( pluginDAO );
        when( pluginDAO.saveInfo( anyString(), anyString(), any() ) ).thenReturn( true );

        when( commands.getInstallCommand() ).thenReturn( requestBuilder );

        setupStrategyOverSpark.setup();

        assertNotNull( environment );
        assertNotNull( containerHost );
        assertNotNull( sparkClusterConfig );
        assertNotNull( commandResult );
        assertEquals( id, containerHost.getId() );
        assertTrue( containerHost.isConnected() );
        assertTrue( pluginDAO.saveInfo( anyString(), anyString(), any() ) );
    }


    @Test
    public void testExecuteCommand() throws CommandException, ClusterException, ClusterSetupException
    {
        when( containerHost.execute( requestBuilder ) ).thenReturn( commandResult );
        when( commandResult.hasSucceeded() ).thenReturn( true );
        setupStrategyOverSpark.executeCommand( containerHost, requestBuilder );

        assertTrue( commandResult.hasSucceeded() );
        assertEquals( commandResult, setupStrategyOverSpark.executeCommand( containerHost, requestBuilder ) );
        assertEquals( commandResult, containerHost.execute( requestBuilder ) );
        assertNotNull( setupStrategyOverSpark.executeCommand( containerHost, requestBuilder ) );
    }


    @Test( expected = NullPointerException.class )
    public void shouldThorwsNullPointerExceptionInConstructor()
    {
        setupStrategyOverSpark = new SetupStrategyOverSpark( null, null, null, null );
    }
}