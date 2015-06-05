package com.vocifery.daytripper.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.vocifery.daytripper.Daytripper;
import com.vocifery.daytripper.R;

public class NoReplyAction implements Actionable {

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
		// TODO Auto-generated method stub
		return this.getClass().getName();
	}

	@Override
	public String doActionWithIntent(Intent intent) {
		final Context appContext = Daytripper.getAppContext();
		String defaultName = appContext.getString(R.string.default_name);
		SharedPreferences prefs = appContext.getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
		String userName = prefs.getString(Daytripper.USERNAME_KEY, defaultName);
		String noOpMessage = appContext.getString(R.string.no_op_message, userName);
		return noOpMessage;
	}

	@Override
	public String getCustomMessage() {
		// TODO Auto-generated method stub
		return null;
	}

}
