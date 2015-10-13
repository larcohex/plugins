/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.subutai.plugin.nutch.ui.manager;


import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.naming.NamingException;

import com.vaadin.data.Property;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.server.Sizeable;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.Window;

import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.hadoop.api.HadoopClusterConfig;
import io.subutai.plugin.nutch.api.Nutch;
import io.subutai.plugin.nutch.api.NutchConfig;
import io.subutai.server.ui.component.ConfirmationDialog;
import io.subutai.server.ui.component.ProgressWindow;
import io.subutai.server.ui.component.TerminalWindow;


public class Manager
{
    protected static final String AVAILABLE_OPERATIONS_COLUMN_CAPTION = "AVAILABLE_OPERATIONS";
    protected static final String REFRESH_CLUSTERS_CAPTION = "Refresh Clusters";
    protected static final String DESTROY_BUTTON_CAPTION = "Destroy";
    protected static final String DESTROY_CLUSTER_BUTTON_CAPTION = "Destroy Cluster";
    protected static final String ADD_NODE_BUTTON_CAPTION = "Add Node";
    protected static final String HOST_COLUMN_CAPTION = "Host";
    protected static final String IP_COLUMN_CAPTION = "IP List";
    protected static final String BUTTON_STYLE_NAME = "default";

    final Button refreshClustersBtn, destroyClusterBtn, addNodeBtn;
    private final Embedded progressIndicator = new Embedded( "", new ThemeResource( "img/spinner.gif" ) );
    private final GridLayout contentRoot;
    private final ComboBox clusterCombo;
    private final Table nodesTable;
    private final Nutch nutch;
    private final ExecutorService executorService;
    private final Tracker tracker;
    private final Hadoop hadoop;
    private NutchConfig config;
    private final EnvironmentManager environmentManager;


    public Manager( final ExecutorService executorService, Nutch nutch, Hadoop hadoop, Tracker tracker,
                    EnvironmentManager environmentManager ) throws NamingException
    {
        this.executorService = executorService;
        this.nutch = nutch;
        this.hadoop = hadoop;
        this.tracker = tracker;
        this.environmentManager = environmentManager;


        contentRoot = new GridLayout();
        contentRoot.setSpacing( true );
        contentRoot.setMargin( true );
        contentRoot.setSizeFull();
        contentRoot.setRows( 10 );
        contentRoot.setColumns( 1 );
        //tables go here
        nodesTable = createTableTemplate( "Nodes" );
        nodesTable.setId( "nodesTable" );

        HorizontalLayout controlsContent = new HorizontalLayout();
        controlsContent.setSpacing( true );

        Label clusterNameLabel = new Label( "Select the cluster" );
        controlsContent.addComponent( clusterNameLabel );

        clusterCombo = new ComboBox();
        clusterCombo.setId( "clusterCombo" );
        clusterCombo.setImmediate( true );
        clusterCombo.setTextInputAllowed( false );
        clusterCombo.setWidth( 200, Sizeable.Unit.PIXELS );
        clusterCombo.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                config = ( NutchConfig ) event.getProperty().getValue();
                refreshUI();
            }
        } );
        controlsContent.addComponent( clusterCombo );


        /** Refresh Cluster button */
        refreshClustersBtn = new Button( REFRESH_CLUSTERS_CAPTION );
        refreshClustersBtn.setId( "refreshClustersBtn" );
        refreshClustersBtn.addStyleName( BUTTON_STYLE_NAME );
        refreshClustersBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                progressIndicator.setVisible( true );
                refreshClustersBtn.setEnabled( false );
                new Thread( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        refreshClustersInfo();
                    }
                } ).start();
            }
        } );
        controlsContent.addComponent( refreshClustersBtn );

        /** Destroy Cluster button */
        destroyClusterBtn = new Button( DESTROY_CLUSTER_BUTTON_CAPTION );
        destroyClusterBtn.setId( "destroyClusterBtn" );
        destroyClusterBtn.addStyleName( BUTTON_STYLE_NAME );
        addClickListenerToDestroyClusterButton();
        controlsContent.addComponent( destroyClusterBtn );


        /** Add Node button */
        addNodeBtn = new Button( ADD_NODE_BUTTON_CAPTION );
        addNodeBtn.setId( "addNodeBtn" );
        addNodeBtn.addStyleName( BUTTON_STYLE_NAME );
        addClickListenerToAddNodeButton();
        controlsContent.addComponent( addNodeBtn );

        progressIndicator.setVisible( false );
        progressIndicator.setId( "indicator" );
        controlsContent.addComponent( progressIndicator );

        contentRoot.addComponent( controlsContent, 0, 0 );
        contentRoot.addComponent( nodesTable, 0, 1, 0, 9 );
    }


    public void addClickListenerToAddNodeButton()
    {
        addNodeBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                if ( config != null )
                {
                    HadoopClusterConfig hadoopConfig = hadoop.getCluster( config.getHadoopClusterName() );
                    if ( hadoopConfig != null )
                    {
                        Set<String> nodes = new HashSet<>( hadoopConfig.getAllNodes() );
                        nodes.removeAll( config.getNodes() );
                        if ( !nodes.isEmpty() )
                        {
                            Set<EnvironmentContainerHost> hosts;
                            try
                            {
                                hosts = environmentManager.loadEnvironment( hadoopConfig.getEnvironmentId() )
                                                          .getContainerHostsByIds( nodes );
                            }
                            catch ( ContainerHostNotFoundException e )
                            {
                                show( "Containers not found" );
                                return;
                            }
                            catch ( EnvironmentNotFoundException e )
                            {
                                show( "Environment not found" );
                                return;
                            }
                            AddNodeWindow addNodeWindow =
                                    new AddNodeWindow( nutch, tracker, executorService, config, hosts );
                            contentRoot.getUI().addWindow( addNodeWindow );
                            addNodeWindow.addCloseListener( new Window.CloseListener()
                            {
                                @Override
                                public void windowClose( Window.CloseEvent closeEvent )
                                {
                                    refreshClustersInfo();
                                }
                            } );
                        }
                        else
                        {
                            show( "All nodes in corresponding Hadoop cluster have Nutch installed" );
                        }
                    }
                    else
                    {
                        show( "Hadoop cluster info not found" );
                    }
                }
                else
                {
                    show( "Please, select cluster" );
                }
            }
        } );
    }


    public void addClickListenerToDestroyClusterButton()
    {
        destroyClusterBtn.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                if ( config != null )
                {
                    ConfirmationDialog alert = new ConfirmationDialog(
                            String.format( "Do you want to destroy the %s cluster?", config.getClusterName() ), "Yes",
                            "No" );
                    alert.getOk().addClickListener( new Button.ClickListener()
                    {
                        @Override
                        public void buttonClick( Button.ClickEvent clickEvent )
                        {
                            UUID trackID = nutch.uninstallCluster( config.getClusterName() );
                            ProgressWindow window =
                                    new ProgressWindow( executorService, tracker, trackID, NutchConfig.PRODUCT_KEY );
                            window.getWindow().addCloseListener( new Window.CloseListener()
                            {
                                @Override
                                public void windowClose( Window.CloseEvent closeEvent )
                                {
                                    refreshClustersInfo();
                                }
                            } );
                            contentRoot.getUI().addWindow( window.getWindow() );
                        }
                    } );

                    contentRoot.getUI().addWindow( alert.getAlert() );
                }
                else
                {
                    show( "Please, select cluster" );
                }
            }
        } );
    }


    private Table createTableTemplate( String caption )
    {
        final Table table = new Table( caption );
        table.addContainerProperty( HOST_COLUMN_CAPTION, String.class, null );
        table.addContainerProperty( IP_COLUMN_CAPTION, String.class, null );
        table.addContainerProperty( AVAILABLE_OPERATIONS_COLUMN_CAPTION, HorizontalLayout.class, null );
        addClickListenerToNodesTable( table );
        table.setSizeFull();
        table.setPageLength( 10 );
        table.setSelectable( false );
        table.setImmediate( true );
        return table;
    }


    public void addClickListenerToNodesTable( final Table table )
    {
        table.addItemClickListener( new ItemClickEvent.ItemClickListener()
        {
            @Override
            public void itemClick( ItemClickEvent event )
            {
                if ( event.isDoubleClick() )
                {
                    String containerHostname =
                            ( String ) table.getItem( event.getItemId() ).getItemProperty( "Host" ).getValue();
                    Set<EnvironmentContainerHost> containerHosts;
                    try
                    {
                        containerHosts =
                                environmentManager.loadEnvironment( config.getEnvironmentId() ).getContainerHosts();
                    }
                    catch ( EnvironmentNotFoundException e )
                    {
                        show( "Environment not found" );
                        return;
                    }

                    Iterator iterator = containerHosts.iterator();
                    EnvironmentContainerHost EnvironmentContainerHost = null;
                    while ( iterator.hasNext() )
                    {
                        EnvironmentContainerHost = ( EnvironmentContainerHost ) iterator.next();
                        if ( EnvironmentContainerHost.getHostname().equals( containerHostname ) )
                        {
                            break;
                        }
                    }
                    if ( EnvironmentContainerHost != null )
                    {
                        TerminalWindow terminal = new TerminalWindow( EnvironmentContainerHost );
                        contentRoot.getUI().addWindow( terminal.getWindow() );
                    }
                    else
                    {
                        show( "Host not found" );
                    }
                }
            }
        } );
    }


    private void show( String notification )
    {
        Notification.show( notification );
    }


    private void refreshUI()
    {
        if ( config != null )
        {
            Set<EnvironmentContainerHost> hosts;
            try
            {
                hosts = environmentManager.loadEnvironment( config.getEnvironmentId() )
                                          .getContainerHostsByIds( config.getNodes() );
            }
            catch ( ContainerHostNotFoundException e )
            {
                show( "Containers not found" );
                return;
            }
            catch ( EnvironmentNotFoundException e )
            {
                show( "Environment not found" );
                return;
            }
            populateTable( nodesTable, hosts );
        }
        else
        {
            nodesTable.removeAllItems();
        }
    }


    private void populateTable( final Table table, Set<EnvironmentContainerHost> containerHosts )
    {

        table.removeAllItems();

        for ( final EnvironmentContainerHost host : containerHosts )
        {
            final Button destroyBtn = new Button( DESTROY_BUTTON_CAPTION );
            destroyBtn.setId( host.getIpByInterfaceName( "eth0" ) + "-nutchDestroy" );
            destroyBtn.addStyleName( BUTTON_STYLE_NAME );

            final HorizontalLayout availableOperations = new HorizontalLayout();
            availableOperations.addStyleName( BUTTON_STYLE_NAME );
            availableOperations.setSpacing( true );

            addGivenComponents( availableOperations, destroyBtn );

            table.addItem( new Object[] {
                    host.getHostname(), host.getIpByInterfaceName( "eth0" ), availableOperations
            }, null );
            addClickListenerToDestroyButton( host, destroyBtn );
        }
    }


    public void addGivenComponents( HorizontalLayout layout, Button... buttons )
    {
        for ( Button b : buttons )
        {
            layout.addComponent( b );
        }
    }


    public void addClickListenerToDestroyButton( final EnvironmentContainerHost host, Button... buttons )
    {
        getButton( DESTROY_BUTTON_CAPTION, buttons ).addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                ConfirmationDialog alert = new ConfirmationDialog(
                        String.format( "Do you want to destroy the %s node?", host.getHostname() ), "Yes", "No" );
                alert.getOk().addClickListener( new Button.ClickListener()
                {
                    @Override
                    public void buttonClick( Button.ClickEvent clickEvent )
                    {
                        UUID trackID = nutch.destroyNode( config.getClusterName(), host.getHostname() );
                        ProgressWindow window =
                                new ProgressWindow( executorService, tracker, trackID, NutchConfig.PRODUCT_KEY );
                        window.getWindow().addCloseListener( new Window.CloseListener()
                        {
                            @Override
                            public void windowClose( Window.CloseEvent closeEvent )
                            {
                                refreshClustersInfo();
                            }
                        } );
                        contentRoot.getUI().addWindow( window.getWindow() );
                    }
                } );

                contentRoot.getUI().addWindow( alert.getAlert() );
            }
        } );
    }


    public Button getButton( String caption, Button... buttons )
    {
        for ( Button b : buttons )
        {
            if ( b.getCaption().equals( caption ) )
            {
                return b;
            }
        }
        return null;
    }


    public void refreshClustersInfo()
    {
        List<NutchConfig> clustersInfo = nutch.getClusters();
        NutchConfig clusterInfo = ( NutchConfig ) clusterCombo.getValue();
        clusterCombo.removeAllItems();
        if ( clustersInfo != null && !clustersInfo.isEmpty() )
        {
            for ( NutchConfig nutchClusterInfo : clustersInfo )
            {
                clusterCombo.addItem( nutchClusterInfo );
                clusterCombo.setItemCaption( nutchClusterInfo,
                        nutchClusterInfo.getClusterName() + "(" + nutchClusterInfo.getHadoopClusterName() + ")" );
            }

            if ( clusterInfo != null )
            {
                for ( NutchConfig nutchConfig : clustersInfo )
                {
                    if ( nutchConfig.getClusterName().equals( clusterInfo.getClusterName() ) )
                    {
                        clusterCombo.setValue( nutchConfig );
                        progressIndicator.setVisible( false );
                        refreshClustersBtn.setEnabled( true );
                        return;
                    }
                }
            }
            else
            {
                clusterCombo.setValue( clustersInfo.iterator().next() );
                progressIndicator.setVisible( false );
                refreshClustersBtn.setEnabled( true );
            }
        }
        progressIndicator.setVisible( false );
        refreshClustersBtn.setEnabled( true );
    }


    public Component getContent()
    {
        return contentRoot;
    }
}
