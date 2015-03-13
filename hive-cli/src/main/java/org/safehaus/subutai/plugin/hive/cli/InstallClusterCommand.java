package org.safehaus.subutai.plugin.hive.cli;


import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.hadoop.api.Hadoop;
import org.safehaus.subutai.plugin.hive.api.Hive;
import org.safehaus.subutai.plugin.hive.api.HiveConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;


/**
 * sample command : hive:install-cluster test \ {cluster name} test \ { hadoop cluster name } hadoop1 \ { server } [
 * hadoop1, hadoop2 ] \ { list of client machines }
 */
@Command( scope = "hive", name = "install-cluster", description = "Command to install Hive cluster" )
public class InstallClusterCommand extends OsgiCommandSupport
{

    @Argument( index = 0, name = "clusterName", description = "The name of the cluster.", required = true,
            multiValued = false )
    String clusterName = null;

    @Argument( index = 1, name = "hadoopClusterName", description = "The name of hadoop cluster.", required = true,
            multiValued = false )
    String hadoopClusterName = null;

    @Argument( index = 2, name = "server", description = "The hostname of server container", required = true,
            multiValued = false )
    String server = null;

    @Argument( index = 3, name = "clients", description = "The hostname list of client nodes", required = true,
            multiValued = false )
    String clients[] = null;

    private static final Logger LOG = LoggerFactory.getLogger( InstallClusterCommand.class.getName() );
    private Hive hiveManager;
    private Hadoop hadoopManager;
    private Tracker tracker;


    protected Object doExecute() throws IOException
    {
        HiveConfig config = new HiveConfig();
        config.setClusterName( clusterName );
        config.setHadoopClusterName( hadoopClusterName );
        config.setEnvironmentId( hadoopManager.getCluster( hadoopClusterName ).getEnvironmentId() );
        config.setServer( UUID.fromString( server ) );

        Set<UUID> nodeSet = new HashSet<>();
        for ( String uuid : clients )
        {
            nodeSet.add( UUID.fromString( uuid ) );
        }
        config.setClients( nodeSet );

        System.out.println( "Installing hive cluster..." );
        UUID uuid = hiveManager.installCluster( config );
        System.out.println( "Install operation is " + StartClusterCommand.waitUntilOperationFinish( tracker, uuid ) );

        return null;
    }


    public Hive getHiveManager()
    {
        return hiveManager;
    }


    public void setHiveManager( final Hive hiveManager )
    {
        this.hiveManager = hiveManager;
    }


    public Tracker getTracker()
    {
        return tracker;
    }


    public void setTracker( final Tracker tracker )
    {
        this.tracker = tracker;
    }


    public Hadoop getHadoopManager()
    {
        return hadoopManager;
    }


    public void setHadoopManager( final Hadoop hadoopManager )
    {
        this.hadoopManager = hadoopManager;
    }
}