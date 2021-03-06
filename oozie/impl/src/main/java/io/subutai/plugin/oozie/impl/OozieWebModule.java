package io.subutai.plugin.oozie.impl;


import com.google.gson.Gson;
import io.subutai.webui.api.WebuiModule;
import io.subutai.webui.entity.AngularjsDependency;
import io.subutai.webui.entity.WebuiModuleResourse;

import java.util.HashMap;
import java.util.Map;


public class OozieWebModule implements WebuiModule
{
    public static String NAME = "Oozie";
    public static String IMG = "plugins/oozie/oozie.png";

	private static final Map<String, Integer> TEMPLATES_REQUIREMENT;
	static
	{
		TEMPLATES_REQUIREMENT = new HashMap<>();
		TEMPLATES_REQUIREMENT.put("hadoop", 1);
	}


	private WebuiModuleResourse oozieResource;


	public void init()
	{
		oozieResource = new WebuiModuleResourse( NAME.toLowerCase(), IMG );
		AngularjsDependency angularjsDependency = new AngularjsDependency(
				"subutai.plugins.oozie",
				"plugins/oozie/oozie.js",
				"plugins/oozie/controller.js",
				"plugins/oozie/service.js",
				"plugins/hadoop/service.js",
				"subutai-app/environment/service.js"
		);

		oozieResource.addDependency(angularjsDependency);
	}

	@Override
	public String getAngularState()
	{
		return oozieResource.getAngularjsList();
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
		return String.format( ".state('%s', %s)", NAME.toLowerCase(), oozieResource.getAngularjsList() );
	}
}
