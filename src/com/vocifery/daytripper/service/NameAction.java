package com.vocifery.daytripper.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.vocifery.daytripper.Daytripper;
import com.vocifery.daytripper.util.QueryParser;

public class NameAction implements Actionable {

	@Override
	public String getVerb() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getObject() {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return null;
	}
}
