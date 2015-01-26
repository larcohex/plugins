package org.safehaus.subutai.plugin.etl.ui;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.safehaus.subutai.common.peer.ContainerHost;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.etl.api.ETL;
import org.safehaus.subutai.plugin.etl.api.setting.ExportSetting;

import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;


public class ExportPanel extends ImportExportBase
{

    private final ETL etl;
    private final ExecutorService executorService;
    AbstractTextField hdfsPathField = UIUtil.getTextField( "HDFS file path:", 300 );


    public ExportPanel( ETL etl, ExecutorService executorService, Tracker tracker )
    {
        super( tracker );
        this.etl = etl;
        this.executorService = executorService;

        init();
    }


    @Override
    public void setHost( ContainerHost host )
    {
        super.setHost( host );
        init();
    }


    @Override
    ExportSetting makeSettings()
    {
        ExportSetting s = new ExportSetting();
        s.setClusterName( clusterName );
        s.setHostname( host.getHostname() );
        s.setConnectionString( connStringField.getValue() );
        s.setTableName( tableField.getValue() );
        s.setUsername( usernameField.getValue() );
        s.setPassword( passwordField.getValue() );
        s.setHdfsPath( hdfsPathField.getValue() );
        s.setOptionalParameters( optionalParams.getValue() );
        return s;
    }


    @Override
    final void init()
    {
        removeAllComponents();
        super.init();
        fields.add( hdfsPathField );

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.addComponent( UIUtil.getButton( "Export", 120, new Button.ClickListener()
        {

            @Override
            public void buttonClick( Button.ClickEvent event )
            {
                clearLogMessages();
                if ( !checkFields() )
                {
                    return;
                }
                setFieldsEnabled( false );
                ExportSetting es = makeSettings();
                final UUID trackId = etl.exportData( es );

                OperationWatcher watcher = new OperationWatcher( trackId );
                watcher.setCallback( new OperationCallback()
                {

                    @Override
                    public void onComplete()
                    {
                        setFieldsEnabled( true );
                    }
                } );
                executorService.execute( watcher );
            }
        } ) );
        buttons.addComponent( UIUtil.getButton( "Cancel", 120, new Button.ClickListener()
        {

            @Override
            public void buttonClick( Button.ClickEvent event )
            {
                detachFromParent();
            }
        } ) );

        List<Component> ls = new ArrayList<>();
        ls.add( UIUtil.getLabel( "<h1>Sqoop Export</h1>", 100, Unit.PERCENTAGE ) );
        ls.add( connStringField );
        ls.add( tableField );
        ls.add( usernameField );
        ls.add( passwordField );
        ls.add( hdfsPathField );
        ls.add( optionalParams );
        ls.add( buttons );

        addComponents( ls );
    }


    @Override
    boolean checkFields()
    {
        if ( super.checkFields() )
        {
            if ( !hasValue( tableField, "Table name not specified" ) )
            {
                return false;
            }
            if ( !hasValue( hdfsPathField, "HDFS file path not specified" ) )
            {
                return false;
            }
            // every field has value
            return true;
        }
        return false;
    }
}
