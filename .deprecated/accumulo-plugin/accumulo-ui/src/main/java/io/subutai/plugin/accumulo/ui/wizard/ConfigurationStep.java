/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.subutai.plugin.accumulo.ui.wizard;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TwinColSelect;
import com.vaadin.ui.VerticalLayout;

import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.util.CollectionUtil;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.plugin.accumulo.api.Accumulo;
import io.subutai.plugin.accumulo.api.AccumuloClusterConfig;
import io.subutai.plugin.accumulo.api.SetupType;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.hadoop.api.HadoopClusterConfig;
import io.subutai.plugin.zookeeper.api.Zookeeper;
import io.subutai.plugin.zookeeper.api.ZookeeperClusterConfig;


public class ConfigurationStep extends Panel
{
    private static final Logger LOGGER = LoggerFactory.getLogger( ConfigurationStep.class );
    private EnvironmentManager environmentManager;
    private Wizard wizard;
    private Hadoop hadoop;
    private Zookeeper zookeeper;
    private Accumulo accumulo;


    public ConfigurationStep( final Accumulo accumulo, final Hadoop hadoop, final Zookeeper zookeeper,
                              final EnvironmentManager environmentManager, final Wizard wizard )
    {

        this.environmentManager = environmentManager;
        this.wizard = wizard;
        this.hadoop = hadoop;
        this.zookeeper = zookeeper;
        this.accumulo = accumulo;

        if ( wizard.getConfig().getSetupType() == SetupType.OVER_HADOOP_N_ZK )
        {
            //hadoop combo
            final ComboBox hadoopClustersCombo = getCombo( "Hadoop cluster" );
            hadoopClustersCombo.setId( "hadoopClusterscb" );

            //zookeeper combo
            final ComboBox zkClustersCombo = getCombo( "Zookeeper cluster" );
            zkClustersCombo.setId( "zkClustersCombo" );

            //master nodes
            final ComboBox masterNodeCombo = getCombo( "Master node" );
            masterNodeCombo.setId( "masterNodeCombo" );
            final ComboBox gcNodeCombo = getCombo( "GC node" );
            gcNodeCombo.setId( "gcNodeCombo" );
            final ComboBox monitorNodeCombo = getCombo( "Monitor node" );
            monitorNodeCombo.setId( "monitorNodeCombo" );

            //accumulo init controls
            TextField clusterNameTxtFld = getTextField( "Cluster name", "Cluster name", 20 );
            clusterNameTxtFld.setId( "clusterNameTxtFld" );
            TextField instanceNameTxtFld = getTextField( "Instance name", "Instance name", 20 );
            instanceNameTxtFld.setId( "instanceNameTxtFld" );
            TextField passwordTxtFld = getTextField( "Password", "Password", 20 );
            passwordTxtFld.setId( "passwordTxtFld" );

            //tracers
            final TwinColSelect tracersSelect =
                    getTwinSelect( "Tracers", "hostname", "Available Nodes", "Selected Nodes", 4 );
            tracersSelect.setId( "TracersSelect" );
            //slave nodes
            final TwinColSelect slavesSelect =
                    getTwinSelect( "Slaves", "hostname", "Available Nodes", "Selected Nodes", 4 );
            slavesSelect.setId( "SlavesSelect" );

            //get existing hadoop clusters
            List<HadoopClusterConfig> hadoopClusters = hadoop.getClusters();

            //fill hadoopClustersCombo with hadoop cluster infos
            for ( HadoopClusterConfig hadoopClusterInfo : hadoopClusters )
            {
                hadoopClustersCombo.addItem( hadoopClusterInfo );
                hadoopClustersCombo.setItemCaption( hadoopClusterInfo, hadoopClusterInfo.getClusterName() );
            }

            //try to find hadoop cluster info based on one saved in the configuration
            if ( wizard.getConfig().getHadoopClusterName() != null )
            {
                hadoop.getCluster( wizard.getConfig().getHadoopClusterName() );
            }

            //select if saved found
            if ( !hadoopClusters.isEmpty() )
            {
                //select first one if saved not found
                hadoopClustersCombo.setValue( hadoopClusters.iterator().next() );
                fillZookeeperComboBox( zkClustersCombo, zookeeper,
                        hadoopClusters.iterator().next().getEnvironmentId() );
            }


            // fill selection controls with hadoop nodes
            if ( hadoopClustersCombo.getValue() != null )
            {
                HadoopClusterConfig hadoopInfo = ( HadoopClusterConfig ) hadoopClustersCombo.getValue();

                wizard.getConfig().setHadoopClusterName( hadoopInfo.getClusterName() );

                setComboDS( masterNodeCombo, hadoopInfo.getAllNodes(), new HashSet<String>() );
                setComboDS( gcNodeCombo, hadoopInfo.getAllNodes(), new HashSet<String>() );
                setComboDS( monitorNodeCombo, hadoopInfo.getAllNodes(), new HashSet<String>() );

                setTwinSelectDS( tracersSelect, getSlaveContainerHosts( Sets.newHashSet( hadoopInfo.getAllNodes() ) ) );
                setTwinSelectDS( slavesSelect, getSlaveContainerHosts( Sets.newHashSet( hadoopInfo.getAllNodes() ) ) );
            }

            //on hadoop cluster change reset all controls and config
            hadoopClustersCombo.addValueChangeListener( new Property.ValueChangeListener()
            {
                @Override
                public void valueChange( Property.ValueChangeEvent event )
                {
                    if ( event.getProperty().getValue() != null )
                    {
                        HadoopClusterConfig hadoopInfo = ( HadoopClusterConfig ) event.getProperty().getValue();
                        //reset relevant controls
                        setComboDS( masterNodeCombo, hadoopInfo.getAllNodes(), new HashSet<String>() );
                        setComboDS( gcNodeCombo, hadoopInfo.getAllNodes(), new HashSet<String>() );
                        setComboDS( monitorNodeCombo, hadoopInfo.getAllNodes(), new HashSet<String>() );

                        setTwinSelectDS( tracersSelect,
                                getSlaveContainerHosts( Sets.newHashSet( hadoopInfo.getAllNodes() ) ) );
                        setTwinSelectDS( slavesSelect,
                                getSlaveContainerHosts( Sets.newHashSet( hadoopInfo.getAllNodes() ) ) );
                        //reset relevant properties
                        wizard.getConfig().setMasterNode( null );
                        wizard.getConfig().setGcNode( null );
                        wizard.getConfig().setMonitor( null );
                        wizard.getConfig().setTracers( null );
                        wizard.getConfig().setSlaves( null );
                        wizard.getConfig().setHadoopClusterName( hadoopInfo.getClusterName() );
                        fillZookeeperComboBox( zkClustersCombo, zookeeper, hadoopInfo.getEnvironmentId() );
                    }
                }
            } );

            //restore master node if back button is pressed
            if ( wizard.getConfig().getMasterNode() != null )
            {
                masterNodeCombo.setValue( wizard.getConfig().getMasterNode() );
            }
            //restore gc node if back button is pressed
            if ( wizard.getConfig().getGcNode() != null )
            {
                gcNodeCombo.setValue( wizard.getConfig().getGcNode() );
            }
            //restore monitor node if back button is pressed
            if ( wizard.getConfig().getMonitor() != null )
            {
                monitorNodeCombo.setValue( wizard.getConfig().getMonitor() );
            }

            //add value change handler
            masterNodeCombo.addValueChangeListener( new Property.ValueChangeListener()
            {
                public void valueChange( Property.ValueChangeEvent event )
                {
                    if ( event.getProperty().getValue() != null )
                    {
                        String masterNode = ( String ) event.getProperty().getValue();
                        wizard.getConfig().setMasterNode( masterNode );
                    }
                }
            } );
            //add value change handler
            gcNodeCombo.addValueChangeListener( new Property.ValueChangeListener()
            {

                public void valueChange( Property.ValueChangeEvent event )
                {
                    if ( event.getProperty().getValue() != null )
                    {
                        String gcNode = ( String ) event.getProperty().getValue();
                        wizard.getConfig().setGcNode( gcNode );
                    }
                }
            } );
            //add value change handler
            monitorNodeCombo.addValueChangeListener( new Property.ValueChangeListener()
            {
                @Override
                public void valueChange( Property.ValueChangeEvent event )
                {
                    if ( event.getProperty().getValue() != null )
                    {
                        String monitor = ( String ) event.getProperty().getValue();
                        wizard.getConfig().setMonitor( monitor );
                    }
                }
            } );

            //restore tracers if back button is pressed
            if ( !CollectionUtil.isCollectionEmpty( wizard.getConfig().getTracers() ) )
            {
                tracersSelect.setValue( wizard.getConfig().getTracers() );
            }
            //restore slaves if back button is pressed
            if ( !CollectionUtil.isCollectionEmpty( wizard.getConfig().getSlaves() ) )
            {
                slavesSelect.setValue( wizard.getConfig().getSlaves() );
            }

            clusterNameTxtFld.setValue( wizard.getConfig().getInstanceName() );
            clusterNameTxtFld.addValueChangeListener( new Property.ValueChangeListener()
            {
                @Override
                public void valueChange( Property.ValueChangeEvent event )
                {
                    wizard.getConfig().setClusterName( event.getProperty().getValue().toString().trim() );
                }
            } );


            instanceNameTxtFld.setValue( wizard.getConfig().getInstanceName() );
            instanceNameTxtFld.addValueChangeListener( new Property.ValueChangeListener()
            {
                @Override
                public void valueChange( Property.ValueChangeEvent event )
                {
                    wizard.getConfig().setInstanceName( event.getProperty().getValue().toString().trim() );
                }
            } );

            passwordTxtFld.setValue( wizard.getConfig().getPassword() );
            passwordTxtFld.addValueChangeListener( new Property.ValueChangeListener()
            {
                @Override
                public void valueChange( Property.ValueChangeEvent event )
                {
                    wizard.getConfig().setPassword( event.getProperty().getValue().toString().trim() );
                }
            } );


            //add value change handler
            tracersSelect.addValueChangeListener( new Property.ValueChangeListener()
            {

                public void valueChange( Property.ValueChangeEvent event )
                {
                    if ( event.getProperty().getValue() != null )
                    {
                        Set<String> nodes = new HashSet<>();
                        Set<EnvironmentContainerHost> nodeList =
                                ( Set<EnvironmentContainerHost> ) event.getProperty().getValue();
                        for ( EnvironmentContainerHost host : nodeList )
                        {
                            nodes.add( host.getId() );
                        }
                        wizard.getConfig().setTracers( nodes );
                    }
                }
            } );
            //add value change handler
            slavesSelect.addValueChangeListener( new Property.ValueChangeListener()
            {
                public void valueChange( Property.ValueChangeEvent event )
                {
                    if ( event.getProperty().getValue() != null )
                    {
                        Set<String> nodes = new HashSet<>();
                        Set<EnvironmentContainerHost> nodeList =
                                ( Set<EnvironmentContainerHost> ) event.getProperty().getValue();
                        for ( EnvironmentContainerHost host : nodeList )
                        {
                            nodes.add( host.getId() );
                        }
                        wizard.getConfig().setSlaves( nodes );
                    }
                }
            } );

            Button next = new Button( "Next" );
            next.setId( "confNext2" );
            next.addStyleName( "default" );
            //check valid configuration
            next.addClickListener( new Button.ClickListener()
            {

                @Override
                public void buttonClick( Button.ClickEvent event )
                {

                    if ( Strings.isNullOrEmpty( wizard.getConfig().getClusterName() ) )
                    {
                        show( "Please, enter cluster name" );
                    }
                    else if ( Strings.isNullOrEmpty( wizard.getConfig().getZookeeperClusterName() ) )
                    {
                        show( "Please, select Zookeeper cluster" );
                    }
                    else if ( Strings.isNullOrEmpty( wizard.getConfig().getHadoopClusterName() ) )
                    {
                        show( "Please, select Hadoop cluster" );
                    }
                    else if ( wizard.getConfig().getMasterNode() == null )
                    {
                        show( "Please, select master node" );
                    }
                    else if ( Strings.isNullOrEmpty( wizard.getConfig().getInstanceName() ) )
                    {
                        show( "Please, specify instance name" );
                    }
                    else if ( Strings.isNullOrEmpty( wizard.getConfig().getPassword() ) )
                    {
                        show( "Please, specify password" );
                    }
                    else if ( wizard.getConfig().getGcNode() == null )
                    {
                        show( "Please, select gc node" );
                    }
                    else if ( wizard.getConfig().getMonitor() == null )
                    {
                        show( "Please, select monitor" );
                    }
                    else if ( CollectionUtil.isCollectionEmpty( wizard.getConfig().getTracers() ) )
                    {
                        show( "Please, select tracer(s)" );
                    }
                    else if ( CollectionUtil.isCollectionEmpty( wizard.getConfig().getSlaves() ) )
                    {
                        show( "Please, select slave(s)" );
                    }
                    else
                    {
                        wizard.next();
                    }
                }
            } );

            Button back = new Button( "Back" );
            back.setId( "confBack2" );
            back.addStyleName( "default" );
            back.addClickListener( new Button.ClickListener()
            {
                @Override
                public void buttonClick( Button.ClickEvent event )
                {
                    wizard.back();
                }
            } );


            setSizeFull();

            VerticalLayout content = new VerticalLayout();
            content.setSizeFull();
            content.setSpacing( true );
            content.setMargin( true );

            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing( true );
            layout.addComponent( new Label( "Please, specify installation settings" ) );
            layout.addComponent( content );

            HorizontalLayout masters = new HorizontalLayout();
            masters.setMargin( new MarginInfo( true, false, false, false ) );
            masters.setSpacing( true );
            masters.addComponent( hadoopClustersCombo );
            masters.addComponent( zkClustersCombo );
            masters.addComponent( masterNodeCombo );
            masters.addComponent( gcNodeCombo );
            masters.addComponent( monitorNodeCombo );

            HorizontalLayout credentials = new HorizontalLayout();
            credentials.setMargin( new MarginInfo( true, false, false, false ) );
            credentials.setSpacing( true );
            credentials.addComponent( clusterNameTxtFld );
            credentials.addComponent( instanceNameTxtFld );
            credentials.addComponent( passwordTxtFld );

            HorizontalLayout buttons = new HorizontalLayout();
            buttons.setMargin( new MarginInfo( true, false, false, false ) );
            buttons.setSpacing( true );
            buttons.addComponent( back );
            buttons.addComponent( next );

            content.addComponent( masters );
            content.addComponent( credentials );
            content.addComponent( tracersSelect );
            content.addComponent( slavesSelect );
            content.addComponent( buttons );

            setContent( layout );
        }
    }


    private void fillZookeeperComboBox( ComboBox zkClustersCombo, Zookeeper zookeeper, String environmentId )
    {
        zkClustersCombo.removeAllItems();
        zkClustersCombo.setValue( null );

        List<ZookeeperClusterConfig> zookeeperClusterConfigs = new ArrayList<>();
        List<ZookeeperClusterConfig> zkClusters = zookeeper.getClusters();

        //fill zkClustersCombo with zk cluster infos
        for ( final ZookeeperClusterConfig zookeeperClusterConfig : zkClusters )
        {
            if ( zookeeperClusterConfig.getEnvironmentId().equals( environmentId ) )
            {
                zkClustersCombo.addItem( zookeeperClusterConfig );
                zkClustersCombo.setItemCaption( zookeeperClusterConfig, zookeeperClusterConfig.getClusterName() );
                zookeeperClusterConfigs.add( zookeeperClusterConfig );
            }
        }
        //try to find zk cluster info based on one saved in the configuration
        ZookeeperClusterConfig zookeeperClusterConfig = null;
        if ( wizard.getConfig().getZookeeperClusterName() != null )
        {
            zookeeperClusterConfig = zookeeper.getCluster( wizard.getConfig().getZookeeperClusterName() );
        }

        //select if saved found
        if ( zookeeperClusterConfig != null )
        {
            zkClustersCombo.setValue( zookeeperClusterConfig );
            zkClustersCombo.setItemCaption( zookeeperClusterConfig, zookeeperClusterConfig.getClusterName() );
        }
        else if ( !zookeeperClusterConfigs.isEmpty() )
        {
            //select first one if saved not found
            zkClustersCombo.setValue( zookeeperClusterConfigs.iterator().next() );
        }

        if ( zkClustersCombo.getValue() != null )
        {
            ZookeeperClusterConfig zkConfig = ( ZookeeperClusterConfig ) zkClustersCombo.getValue();
            wizard.getConfig().setZookeeperClusterName( zkConfig.getClusterName() );
        }

        zkClustersCombo.addValueChangeListener( new Property.ValueChangeListener()
        {
            @Override
            public void valueChange( Property.ValueChangeEvent event )
            {
                if ( event.getProperty().getValue() != null )
                {
                    ZookeeperClusterConfig zkConfig = ( ZookeeperClusterConfig ) event.getProperty().getValue();
                    wizard.getConfig().setZookeeperClusterName( zkConfig.getClusterName() );
                }
            }
        } );
    }


    private Set<EnvironmentContainerHost> getSlaveContainerHosts( Set<String> slaves )
    {
        Set<EnvironmentContainerHost> set = new HashSet<>();
        if ( wizard.getConfig().getHadoopClusterName() == null || wizard.getConfig().getZookeeperClusterName() == null )
        {
            return set;
        }
        HadoopClusterConfig hadoopClusterConfig = hadoop.getCluster( wizard.getConfig().getHadoopClusterName() );
        List<AccumuloClusterConfig> accumuloClusterConfigs = accumulo.getClusters();
        List<String> allowedNodes = new ArrayList<>( slaves );

        for ( final AccumuloClusterConfig accumuloClusterConfig : accumuloClusterConfigs )
        {
            for ( int i = 0; i < allowedNodes.size(); i++ )
            {
                String nodeId = allowedNodes.get( i );
                if ( accumuloClusterConfig.getAllNodes().contains( nodeId ) )
                {
                    allowedNodes.remove( i-- );
                }
            }
        }

        for ( String uuid : allowedNodes )
        {
            try
            {
                set.add( environmentManager.loadEnvironment( hadoopClusterConfig.getEnvironmentId() )
                                           .getContainerHostById( uuid ) );
            }
            catch ( ContainerHostNotFoundException | EnvironmentNotFoundException e )
            {
                LOGGER.error( "Error applying operation on environment/container" );
            }
        }
        return set;
    }


    public static ComboBox getCombo( String title )
    {
        ComboBox combo = new ComboBox( title );
        combo.setImmediate( true );
        combo.setTextInputAllowed( false );
        combo.setRequired( true );
        combo.setNullSelectionAllowed( false );
        return combo;
    }


    public static TwinColSelect getTwinSelect( String title, String captionProperty, String leftTitle,
                                               String rightTitle, int rows )
    {
        TwinColSelect twinColSelect = new TwinColSelect( title );
        twinColSelect.setItemCaptionPropertyId( captionProperty );
        twinColSelect.setRows( rows );
        twinColSelect.setMultiSelect( true );
        twinColSelect.setImmediate( true );
        twinColSelect.setLeftColumnCaption( leftTitle );
        twinColSelect.setRightColumnCaption( rightTitle );
        twinColSelect.setWidth( 100, Sizeable.Unit.PERCENTAGE );
        twinColSelect.setRequired( true );
        return twinColSelect;
    }


    public static TextField getTextField( String caption, String prompt, int maxLength )
    {
        TextField textField = new TextField( caption );
        textField.setInputPrompt( prompt );
        textField.setMaxLength( maxLength );
        textField.setRequired( true );
        return textField;
    }


    private void setComboDS( ComboBox target, List<String> agents, Set<String> excludeNodes )
    {
        target.removeAllItems();
        target.setValue( null );

        ZookeeperClusterConfig zookeeperClusterConfig;

        try
        {
            zookeeperClusterConfig = zookeeper.getCluster( wizard.getConfig().getZookeeperClusterName() );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Some exception getting zookeeper cluster.", e );
            return;
        }
        if ( zookeeperClusterConfig == null )
        {
            return;
        }

        List<String> allowedNodes = new ArrayList<>( agents );
        List<AccumuloClusterConfig> accumuloClusterConfigs = accumulo.getClusters();
        for ( final AccumuloClusterConfig accumuloClusterConfig : accumuloClusterConfigs )
        {
            for ( int i = 0; i < allowedNodes.size(); i++ )
            {
                String nodeId = allowedNodes.get( i );
                if ( accumuloClusterConfig.getAllNodes().contains( nodeId ) )
                {
                    allowedNodes.remove( i-- );
                }
            }
        }

        for ( String agent : allowedNodes )
        {
            EnvironmentContainerHost host = getHost( agent );
            if ( host != null && !excludeNodes.contains( agent ) )
            {
                target.addItem( host.getId() );
                target.setItemCaption( host.getId(), host.getHostname() );
            }
        }
    }


    private EnvironmentContainerHost getHost( String uuid )
    {
        try
        {
            return environmentManager.loadEnvironment(
                    hadoop.getCluster( wizard.getConfig().getHadoopClusterName() ).getEnvironmentId() )
                                     .getContainerHostById( uuid );
        }
        catch ( ContainerHostNotFoundException | EnvironmentNotFoundException e )
        {
            LOGGER.error( "Environment/Container doesn't exists." );
            return null;
        }
    }


    private void setTwinSelectDS( TwinColSelect target, Set<EnvironmentContainerHost> containerHosts )
    {
        target.setValue( null );
        target.setContainerDataSource( new BeanItemContainer<>( EnvironmentContainerHost.class, containerHosts ) );
    }


    private void show( String notification )
    {
        Notification.show( notification );
    }
}
