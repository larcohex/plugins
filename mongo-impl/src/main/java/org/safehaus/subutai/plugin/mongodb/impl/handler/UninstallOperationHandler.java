package org.safehaus.subutai.plugin.mongodb.impl.handler;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.safehaus.subutai.common.command.CommandException;
import org.safehaus.subutai.common.command.CommandResult;
import org.safehaus.subutai.common.command.RequestBuilder;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.core.environment.api.exception.EnvironmentDestroyException;
import org.safehaus.subutai.core.environment.api.helper.Environment;
import org.safehaus.subutai.core.metric.api.MonitorException;
import org.safehaus.subutai.core.peer.api.CommandUtil;
import org.safehaus.subutai.core.peer.api.ContainerHost;
import org.safehaus.subutai.plugin.mongodb.api.InstallationType;
import org.safehaus.subutai.plugin.mongodb.api.MongoClusterConfig;
import org.safehaus.subutai.plugin.mongodb.impl.MongoImpl;
import org.safehaus.subutai.plugin.mongodb.impl.common.Commands;


/**
 * Handles uninstall mongo cluster operation
 */
public class UninstallOperationHandler extends AbstractMongoOperationHandler<MongoImpl, MongoClusterConfig>
{
    private final TrackerOperation po;
    private CommandUtil commandUtil = new CommandUtil();


    public UninstallOperationHandler( MongoImpl manager, String clusterName )
    {
        super( manager, clusterName );
        po = manager.getTracker().createTrackerOperation( MongoClusterConfig.PRODUCT_KEY,
                String.format( "Destroying cluster %s", clusterName ) );
    }


    @Override
    public UUID getTrackerId()
    {
        return po.getId();
    }


    @Override
    public void run()
    {
        MongoClusterConfig config = manager.getCluster( clusterName );
        if ( config == null )
        {
            po.addLogFailed( String.format( "Cluster with name %s does not exist", clusterName ) );
            return;
        }

        try
        {
            po.addLog( "Removing subscription from environment." );
            Environment environment = manager.getEnvironmentManager().getEnvironmentByUUID( config.getEnvironmentId() );
            manager.unsubscribeFromAlerts( environment );

            if ( config.getInstallationType() == InstallationType.STANDALONE )
            {
                po.addLog( "Destroying lxc containers" );
                manager.getEnvironmentManager().destroyEnvironment( config.getEnvironmentId() );
                po.addLog( "Lxc containers successfully destroyed" );
            }
            else if ( config.getInstallationType() == InstallationType.OVER_ENVIRONMENT )
            {
                po.addLog( "Purging subutai-hadoop from containers." );
                Set<ContainerHost> containerHosts = environment.getContainerHostsByIds( config.getAllNodeIds() );
                List<CommandResult> commandResults = new ArrayList<>();
                for ( final ContainerHost containerHost : containerHosts )
                {
                    commandResults.add( commandUtil.execute(
                            new RequestBuilder( Commands.getUninstallClearMongoConfigsCommand().getCommand() ),
                                    containerHost ) );
                }
                logResults( po, commandResults );
            }
        }
        catch ( EnvironmentDestroyException | MonitorException | CommandException ex )
        {
            po.addLog( String.format( "%s, skipping...", ex.getMessage() ) );
        }

        po.addLog( "Deleting cluster information from database.." );

        manager.getPluginDAO().deleteInfo( MongoClusterConfig.PRODUCT_KEY, config.getClusterName() );
        po.addLogDone( "Cluster destroyed." );
    }
}
