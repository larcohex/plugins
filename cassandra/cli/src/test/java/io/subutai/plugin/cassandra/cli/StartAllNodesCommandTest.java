package io.subutai.plugin.cassandra.cli;


import java.io.IOException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import io.subutai.common.tracker.OperationState;
import io.subutai.common.tracker.TrackerOperationView;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.cassandra.api.Cassandra;
import io.subutai.plugin.cassandra.api.CassandraClusterConfig;
import io.subutai.plugin.cassandra.cli.StartAllNodesCommand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StartAllNodesCommandTest
{
    private StartAllNodesCommand startAllNodesCommand;
    @Mock
    Cassandra cassandra;
    @Mock
    Tracker tracker;
    @Mock
    TrackerOperationView trackerOperationView;


    @Before
    public void setUp() 
    {
        startAllNodesCommand = new StartAllNodesCommand();
    }

    @Test
    public void testGetCassandraManager() 
    {
        startAllNodesCommand.setCassandraManager(cassandra);
        startAllNodesCommand.getCassandraManager();

        // assertions
        assertNotNull(startAllNodesCommand.getCassandraManager());
        assertEquals(cassandra,startAllNodesCommand.getCassandraManager());

    }

    @Test
    public void testSetCassandraManager() 
    {
        startAllNodesCommand.setCassandraManager(cassandra);
        startAllNodesCommand.getCassandraManager();

        // assertions
        assertNotNull(startAllNodesCommand.getCassandraManager());
        assertEquals(cassandra,startAllNodesCommand.getCassandraManager());

    }

    @Test
    public void testGetTracker() 
    {
        startAllNodesCommand.setTracker(tracker);
        startAllNodesCommand.getTracker();

        // assertions
        assertNotNull(startAllNodesCommand.getTracker());
        assertEquals(tracker,startAllNodesCommand.getTracker());

    }

    @Test
    public void testSetTracker() 
    {
        startAllNodesCommand.setTracker(tracker);
        startAllNodesCommand.getTracker();

        // assertions
        assertNotNull(startAllNodesCommand.getTracker());
        assertEquals(tracker,startAllNodesCommand.getTracker());

    }

    @Test
    public void testDoExecute() throws IOException
    {
        UUID uuid = new UUID(50,50);
        startAllNodesCommand.setTracker(tracker);
        startAllNodesCommand.setCassandraManager(cassandra);
        when(cassandra.startCluster(anyString())).thenReturn(uuid);
        when(tracker.getTrackerOperation(CassandraClusterConfig.PRODUCT_KEY, uuid)).thenReturn(trackerOperationView);
        when(trackerOperationView.getLog()).thenReturn("test");

        startAllNodesCommand.doExecute();

        // assertions
        assertNotNull(tracker.getTrackerOperation(CassandraClusterConfig.PRODUCT_KEY,uuid));
        verify(cassandra).startCluster(anyString());
        assertNotEquals(OperationState.RUNNING,trackerOperationView.getState());
    }

    @Test
    public void testDoExecuteWhenTrackerOperationViewIsNull() throws IOException
    {
        UUID uuid = new UUID(50,50);
        startAllNodesCommand.setTracker(tracker);
        startAllNodesCommand.setCassandraManager(cassandra);
        when(cassandra.startCluster(anyString())).thenReturn(uuid);
        when(tracker.getTrackerOperation(CassandraClusterConfig.PRODUCT_KEY,uuid)).thenReturn(null);

        startAllNodesCommand.doExecute();

        // assertions
        assertNull(tracker.getTrackerOperation(CassandraClusterConfig.PRODUCT_KEY,uuid));
        verify(cassandra).startCluster(anyString());
    }

}