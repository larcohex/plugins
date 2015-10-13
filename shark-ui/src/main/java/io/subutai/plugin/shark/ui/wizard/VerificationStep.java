package io.subutai.plugin.shark.ui.wizard;


import java.util.UUID;
import java.util.concurrent.ExecutorService;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Window;

import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.common.ui.ConfigView;
import io.subutai.plugin.shark.api.Shark;
import io.subutai.plugin.shark.api.SharkClusterConfig;
import io.subutai.server.ui.component.ProgressWindow;


public class VerificationStep extends Panel
{

    public VerificationStep( final Shark shark, final ExecutorService executor, final Tracker tracker,
                             final Wizard wizard )
    {
        final SharkClusterConfig config = wizard.getConfig();

        setSizeFull();

        GridLayout grid = new GridLayout( 1, 5 );
        grid.setSpacing( true );
        grid.setMargin( true );
        grid.setSizeFull();

        Label confirmationLbl = new Label( "<strong>Please verify the installation settings "
                + "(you may change them by clicking on Back button)</strong><br/>" );
        confirmationLbl.setContentMode( ContentMode.HTML );

        ConfigView cfgView = new ConfigView( "Installation configuration" );
        cfgView.addStringCfg( "Cluster Name", wizard.getConfig().getClusterName() );

        cfgView.addStringCfg( "Spark cluster name", config.getSparkClusterName() );


        Button install = new Button( "Install" );
        install.setId( "SharkVerInstall" );
        install.addStyleName( "default" );
        install.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent clickEvent )
            {
                UUID trackId = shark.installCluster( wizard.getConfig() );
                ProgressWindow w = new ProgressWindow( executor, tracker, trackId, SharkClusterConfig.PRODUCT_KEY );
                w.getWindow().addCloseListener( new Window.CloseListener()
                {
                    @Override
                    public void windowClose( Window.CloseEvent closeEvent )
                    {
                        wizard.init();
                    }
                } );
                getUI().addWindow( w.getWindow() );
            }
        } );

        Button back = new Button( "Back" );
        back.setId( "SharkVerBack" );
        back.addStyleName( "default" );
        back.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent event )
            {
                wizard.back();
            }
        } );

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.addComponent( back );
        buttons.addComponent( install );

        grid.addComponent( confirmationLbl, 0, 0 );
        grid.addComponent( cfgView.getCfgTable(), 0, 1, 0, 3 );
        grid.addComponent( buttons, 0, 4 );

        setContent( grid );
    }
}

