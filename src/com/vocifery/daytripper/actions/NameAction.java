package com.vocifery.daytripper.actions;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.vocifery.daytripper.service.ResponderService;
import com.vocifery.daytripper.ui.Daytripper;
import com.vocifery.daytripper.util.QueryParser;

public class NameAction implements Actionable {

	@Override
	public String getVerb() {
		return null;
	}

	@Override
	public String getObject() {
		return null;
	}

	@Override
	public String getSource() {
		return getClass().getName();
	}

	@Override
	public String doActionWithIntent(Intent intent) {
		String query = intent.getStringExtra(ResponderService.KEY_QUERY);
		String name = QueryParser.extractNameFromQuery(query);
		SharedPreferences prefs = Daytripper.getAppContext().getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.putString(Daytripper.USERNAME_KEY, name);
		editor.commit();
		return name;
	}

	@Override
	public String getCustomMessage() {
		return null;
	}
}
