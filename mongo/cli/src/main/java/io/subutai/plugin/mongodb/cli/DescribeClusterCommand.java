package io.subutai.plugin.mongodb.cli;


import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import io.subutai.plugin.mongodb.api.Mongo;
import io.subutai.plugin.mongodb.api.MongoClusterConfig;


@Command( scope = "mongo", name = "describe-cluster", description = "Shows the details of the Mongo cluster." )
public class DescribeClusterCommand extends OsgiCommandSupport
{

    @Argument( index = 0, name = "clusterName", description = "The name of the cluster.", required = true,
            multiValued = false )
    String clusterName = null;
    private Mongo mongoManager;


    protected Object doExecute()
    {
        MongoClusterConfig config = mongoManager.getCluster( clusterName );
        if ( config != null )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "Cluster name: " ).append( config.getClusterName() ).append( "\n" );
            System.out.println( sb.toString() );
        }
        else
        {
            System.out.println( "No clusters found..." );
        }

        return null;
    }


    public void setMongoManager( final Mongo mongoManager )
    {
        this.mongoManager = mongoManager;
    }
}
