package io.subutai.plugin.zookeeper.impl;


import com.google.gson.Gson;
import io.subutai.webui.api.WebuiModule;
import io.subutai.webui.entity.AngularjsDependency;
import io.subutai.webui.entity.WebuiModuleResourse;

import java.util.HashMap;
import java.util.Map;


public class ZookeeperWebModule implements WebuiModule
{
    public static String NAME = "Zookeeper";
    public static String IMG = "plugins/zookeeper/zookeeper.png";

    private static final Map<String, Integer> TEMPLATES_REQUIREMENT;
    static
    {
        TEMPLATES_REQUIREMENT = new HashMap<>();
        TEMPLATES_REQUIREMENT.put("zookeeper", 1);
    }


    private WebuiModuleResourse zooResource;


    public void init()
    {
        zooResource = new WebuiModuleResourse( NAME.toLowerCase(), IMG );
        AngularjsDependency angularjsDependency = new AngularjsDependency(
                "subutai.plugins.zookeeper",
                "plugins/zookeeper/zookeeper.js",
                "plugins/zookeeper/controller.js",
                "plugins/zookeeper/service.js",
                "plugins/hadoop/service.js",
                "subutai-app/environment/service.js"
        );

        zooResource.addDependency(angularjsDependency);
    }

    @Override
    public String getAngularState()
    {
        return zooResource.getAngularjsList();
    }

    @Override
    public String getName()
    {
        return NAME;
    }


    @Override
    public String getModuleInfo()
    {
        return String.format( "{\"img\" : \"%s\", \"name\" : \"%s\", \"requirement\" : %s}", IMG, NAME, new Gson().toJson( TEMPLATES_REQUIREMENT ).toString());
    }


    @Override
    public String getAngularDependecyList()
    {
        return String.format( ".state('%s', %s)", NAME.toLowerCase(), zooResource.getAngularjsList() );
    }
}
