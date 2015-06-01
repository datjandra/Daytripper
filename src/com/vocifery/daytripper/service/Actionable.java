package com.vocifery.daytripper.service;

import android.content.Intent;

public interface Actionable {
	
	public final static int CONNECTION_TIMEOUT = 20000;
	public final static int SOCKET_TIMEOUT = 20000;
	public final static String SEARCH_URL = "http://vocifery.com/api/v0/entity/search";
	public final static String ENTITY_ACTION_URL = "https://realitywarp-vocifery.rhcloud.com/api/v0/entity/action";
	public final static String REDIRECT_URL = "https://realitywarp-vocifery.rhcloud.com/api/v0/ping";

	public String getVerb();
	public String getObject();
	public String getSource();
	public String doActionWithIntent(Intent intent);
	public String getCustomMessage();
}
