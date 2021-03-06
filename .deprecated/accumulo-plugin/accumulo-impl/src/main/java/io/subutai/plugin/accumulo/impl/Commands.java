package io.subutai.plugin.accumulo.impl;


import io.subutai.common.command.RequestBuilder;
import io.subutai.common.settings.Common;
import io.subutai.plugin.accumulo.api.AccumuloClusterConfig;


public class Commands
{

    public static final String installCommand = "apt-get --force-yes --assume-yes install ";

    public static final String uninstallCommand =
            "apt-get --force-yes --assume-yes purge " + Common.PACKAGE_PREFIX + AccumuloClusterConfig.PRODUCT_KEY
                    .toLowerCase();

    public static final RequestBuilder startCommand = new RequestBuilder( "/etc/init.d/accumulo start" ).daemon();

    public static final RequestBuilder stopCommand =
            new RequestBuilder( "/etc/init.d/accumulo stop" ).withTimeout( 90 );

    public static final RequestBuilder statusCommand =
            new RequestBuilder( "/etc/init.d/accumulo status" ).withTimeout( 30 );

    public static final String checkIfInstalled = "dpkg -l | grep '^ii' | grep " + Common.PACKAGE_PREFIX_WITHOUT_DASH;


    public static RequestBuilder getInstallCommand()
    {
        return new RequestBuilder(
                "apt-get --force-yes --assume-yes install " + Common.PACKAGE_PREFIX + AccumuloClusterConfig.PRODUCT_KEY
                        .toLowerCase() ).withTimeout( 360 );
    }


    public static RequestBuilder getClearMastersFileCommand( String fileName )
    {
        return new RequestBuilder(". /etc/profile && accumuloMastersConf.sh " + fileName + " clear");
    }

    public static RequestBuilder getClearSlavesFileCommand( String fileName )
    {
        return new RequestBuilder(". /etc/profile && accumuloSlavesConf.sh " + fileName + " clear");
    }

    public static RequestBuilder getAddMasterCommand( String hostname )
    {
        return new RequestBuilder(
                ". /etc/profile && accumuloMastersConf.sh masters add " + hostname ).withTimeout( 30 );
    }


    public static RequestBuilder getAddTracersCommand( String serializedHostNames )
    {
        return new RequestBuilder(
                ". /etc/profile && accumuloMastersConf.sh tracers add " + serializedHostNames ).withTimeout( 30 );
    }


    public static RequestBuilder getListOfPackageInstalledWithPrefix( String prefix )
    {
        return new RequestBuilder(
                String.format( "dpkg-query -W -f='${Package}\\t${Status}\\t${Version}\\n' '%s*'", prefix ) )
                .withTimeout( 30 );
    }


    public static RequestBuilder getPackageQueryCommand( String packageName )
    {
        return new RequestBuilder( String.format( "dpkg -s %s", packageName ) ).withTimeout( 30 );
    }


    public static String getClearTracerCommand( String hostname )
    {
        return ". /etc/profile && accumuloMastersConf.sh tracers clear " + hostname;
    }


    public static RequestBuilder getAddGCCommand( String hostname )
    {
        return new RequestBuilder(
                ". /etc/profile && accumuloMastersConf.sh gc add " + hostname ).withTimeout( 30 );
    }


    public static RequestBuilder getAddMonitorCommand( String hostname )
    {
        return new RequestBuilder(
                ". /etc/profile && accumuloMastersConf.sh monitor add " + hostname ).withTimeout( 30 );
    }


    public static RequestBuilder getAddSlavesCommand( String serializedHostNames )
    {
        return new RequestBuilder(
                ". /etc/profile && accumuloSlavesConf.sh slaves add " + serializedHostNames ).withTimeout( 30 );
    }


    public static String getClearSlaveCommand( String hostname )
    {
        return ". /etc/profile && accumuloSlavesConf.sh slaves clear " + hostname;
    }


    public static RequestBuilder getBindZKClusterCommand( String zkNodesCommaSeparated )
    {
        return new RequestBuilder(
                ". /etc/profile && accumulo-conf.sh remove accumulo-site.xml instance.zookeeper.host && "
                        + "accumulo-conf.sh add accumulo-site.xml instance.zookeeper.host " + zkNodesCommaSeparated )
                .withTimeout( 30 );
    }


    public static RequestBuilder getInitCommand( String instanceName, String password )
    {
        return new RequestBuilder( ". /etc/profile && accumulo-init.sh " + instanceName + " " + password )
                .withTimeout( 30 );
    }


    public static String getAddPropertyCommand( String propertyName, String propertyValue )
    {
        return ". /etc/profile && accumulo-property.sh add " + propertyName + " " + propertyValue;
    }


    public static String getRemovePropertyCommand( String propertyName )
    {
        return ". /etc/profile && accumulo-property.sh clear " + propertyName;
    }


    public static RequestBuilder getRemoveAccumuloFromHFDSCommand()
    {
        return new RequestBuilder( ". /etc/profile && hadoop dfs -rmr /accumulo" ).withTimeout( 30 );
    }
}
