package io.subutai.plugin.mysql.ui.environment;


import io.subutai.common.util.FileUtil;
import io.subutai.plugin.mysql.ui.MySQLPortalModule;

import com.vaadin.server.FileResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;


public class StepStart extends VerticalLayout
{
    public StepStart( final EnvironmentWizard environmentWizard )
    {
        setSizeFull();

        GridLayout gridLayout = new GridLayout( 10, 6 );
        gridLayout.setSizeFull();

        Label welcomeMsg = new Label( "<center><h2>Welcome to MySQL Cluster Installation Wizard!</h2><center>" );

        welcomeMsg.addStyleName( "h2" );
        welcomeMsg.setContentMode( ContentMode.HTML );
        gridLayout.addComponent( welcomeMsg,3,1,6,2 );

        Label logoImg = new Label();
        logoImg.setIcon( new FileResource( FileUtil.getFile( MySQLPortalModule.MODULE_IMAGE, this) ) );
        logoImg.setContentMode( ContentMode.HTML );
        logoImg.setHeight( 150,Unit.PIXELS );
        logoImg.setWidth( 220,Unit.PIXELS );
        gridLayout.addComponent( logoImg,1,3,2,5 );

        Button next = new Button("Start");
        next.setId( "SQLWelcomeStartBtn" );
        next.addStyleName( "default" );
        next.setWidth( 100,Unit.PIXELS );
        gridLayout.addComponent( next,6,4,6,4 );
        gridLayout.setComponentAlignment( next, Alignment.BOTTOM_RIGHT );

        next.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                environmentWizard.init();
                environmentWizard.next();
            }
        } );
        addComponent( gridLayout );
    }
}
