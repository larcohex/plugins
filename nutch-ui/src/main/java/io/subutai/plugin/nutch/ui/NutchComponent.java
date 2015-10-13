/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.subutai.plugin.nutch.ui;


import java.util.concurrent.ExecutorService;

import javax.naming.NamingException;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.nutch.api.Nutch;
import io.subutai.plugin.nutch.ui.manager.Manager;
import io.subutai.plugin.nutch.ui.wizard.Wizard;


public class NutchComponent extends CustomComponent
{

    public NutchComponent( ExecutorService executorService, Nutch nutch, Hadoop hadoop, Tracker tracker,
                           EnvironmentManager environmentManager ) throws NamingException
    {
        setSizeFull();

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSpacing( true );
        verticalLayout.setSizeFull();

        TabSheet sheet = new TabSheet();
        sheet.setSizeFull();
        final Manager manager = new Manager( executorService, nutch, hadoop, tracker, environmentManager );
        Wizard wizard = new Wizard( executorService, nutch, hadoop, tracker, environmentManager );
        sheet.addTab( wizard.getContent(), "Install" );
        sheet.getTab( 0 ).setId( "NutchInstallTab" );
        sheet.addTab( manager.getContent(), "Manage" );
        sheet.getTab( 1 ).setId( "NutchManageTab" );
        sheet.addSelectedTabChangeListener( new TabSheet.SelectedTabChangeListener()
        {
            @Override
            public void selectedTabChange( TabSheet.SelectedTabChangeEvent event )
            {
                TabSheet tabsheet = event.getTabSheet();
                String caption = tabsheet.getTab( event.getTabSheet().getSelectedTab() ).getCaption();
                if ( caption.equals( "Manage" ) )
                {
                    manager.refreshClustersInfo();
                }
            }
        } );
        verticalLayout.addComponent( sheet );

        setCompositionRoot( verticalLayout );
        manager.refreshClustersInfo();
    }
}

