package io.subutai.plugin.nutch.impl;


import com.google.gson.Gson;
import io.subutai.webui.api.WebuiModule;
import io.subutai.webui.entity.AngularjsDependency;
import io.subutai.webui.entity.WebuiModuleResourse;

import java.util.HashMap;
import java.util.Map;

public class NutchWebModule implements WebuiModule
{
	public static String NAME = "Nutch";
	public static String IMG = "plugins/nutch/nutch.png";

	private static final Map<String, Integer> TEMPLATES_REQUIREMENT;
	static
	{
		TEMPLATES_REQUIREMENT = new HashMap<>();
		TEMPLATES_REQUIREMENT.put("hadoop", 1);
	}


	private WebuiModuleResourse nutchResource;


	public void init()
	{
		nutchResource = new WebuiModuleResourse( NAME.toLowerCase(), IMG );
		AngularjsDependency angularjsDependency = new AngularjsDependency(
				"subutai.plugins.nutch",
				"plugins/nutch/nutch.js",
				"plugins/nutch/controller.js",
				"plugins/nutch/service.js",
				"plugins/hadoop/service.js",
				"subutai-app/environment/service.js"
		);

		nutchResource.addDependency(angularjsDependency);
	}

	@Override
	public String getAngularState()
	{
		return nutchResource.getAngularjsList();
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
		return String.format( ".state('%s', %s)", NAME.toLowerCase(), nutchResource.getAngularjsList() );
	}
}
