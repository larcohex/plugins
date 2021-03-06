package io.subutai.plugin.etl.ui.load;


import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.naming.NamingException;

import com.google.common.collect.Sets;
import com.vaadin.data.Property;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.UI;

import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.etl.api.ETL;
import io.subutai.plugin.etl.ui.ETLBaseManager;
import io.subutai.plugin.etl.ui.ExportPanel;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.hadoop.api.HadoopClusterConfig;
import io.subutai.plugin.sqoop.api.Sqoop;
import io.subutai.plugin.sqoop.api.SqoopConfig;


public class ETLLoadManager extends ETLBaseManager
{
    private final ExportPanel exportPanel;


    public ETLLoadManager( final ExecutorService executorService, ETL etl, final Hadoop hadoop, final Sqoop sqoop,
                           Tracker tracker, final EnvironmentManager environmentManager ) throws NamingException
    {
        super( executorService, etl, hadoop, sqoop, tracker, environmentManager );

        List<HadoopClusterConfig> clusters = hadoop.getClusters();

        if ( !clusters.isEmpty() )
        {
            for ( HadoopClusterConfig hadoopClusterInfo : clusters )
            {
                hadoopClustersCombo.addItem( hadoopClusterInfo );
                hadoopClustersCombo.setItemCaption( hadoopClusterInfo, hadoopClusterInfo.getClusterName() );
            }
        }

        final ComboBox sqoopSelection = new ComboBox( SQOOP_COMBO_BOX_CAPTION );
        sqoopSelection.setNullSelectionAllowed( false );
        sqoopSelection.setImmediate( true );
        sqoopSelection.setTextInputAllowed( false );
        sqoopSelection.setRequired( true );
        contentRoot.addComponent( sqoopSelection, 0, 2 );

        exportPanel = new ExportPanel( etl, sqoop, executorService, tracker );
        contentRoot.addComponent( exportPanel, 1, 0, 19, 17 );

        // event listeners
        hadoopClustersCombo.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                if ( event.getProperty().getValue() != null )
                {
                    final HadoopClusterConfig hadoopInfo = ( HadoopClusterConfig ) event.getProperty().getValue();
                    sqoopSelection.setValue( null );
                    sqoopSelection.removeAllItems();

                    enableProgressBar();
                    executorService.execute( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Environment hadoopEnvironment = null;
                            try
                            {
                                hadoopEnvironment = environmentManager.loadEnvironment( hadoopInfo.getEnvironmentId() );
                            }
                            catch ( EnvironmentNotFoundException e )
                            {
                                e.printStackTrace();
                            }
                            Set<EnvironmentContainerHost> hadoopNodes = null;
                            if ( hadoopEnvironment != null )
                            {
                                try
                                {
                                    hadoopNodes = hadoopEnvironment
                                            .getContainerHostsByIds( Sets.newHashSet( hadoopInfo.getAllNodes() ) );
                                }
                                catch ( ContainerHostNotFoundException e )
                                {
                                    e.printStackTrace();
                                }
                            }
                            final Set<EnvironmentContainerHost> finalHadoopNodes = hadoopNodes;
                            UI.getCurrent().access( new Runnable()
                            {
                                @Override
                                public void run()
                                {

                                    Set<EnvironmentContainerHost> filteredNodes =
                                            filterSqoopInstalledNodes( finalHadoopNodes );

                                    if ( filteredNodes.isEmpty() )
                                    {
                                        show( "No node has subutai Sqoop package installed" );
                                    }
                                    else
                                    {
                                        for ( EnvironmentContainerHost hadoopNode : filteredNodes )
                                        {
                                            sqoopSelection.addItem( hadoopNode );
                                            sqoopSelection.setItemCaption( hadoopNode, hadoopNode.getHostname() );
                                        }
                                    }
                                    disableProgressBar();
                                }
                            } );
                        }
                    } );
                }
            }
        } );

        sqoopSelection.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                if ( event.getProperty().getValue() != null )
                {
                    EnvironmentContainerHost containerHost =
                            ( EnvironmentContainerHost ) event.getProperty().getValue();
                    exportPanel.setHost( containerHost );
                    SqoopConfig config = findSqoopConfigOfContainerHost( sqoop.getClusters(), containerHost );
                    exportPanel.setSqoopClusterName( config.getClusterName() );
                }
            }
        } );
    }
}
