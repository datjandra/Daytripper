package com.vocifery.daytripper.actions;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.vocifery.daytripper.R;
import com.vocifery.daytripper.ui.Daytripper;

public class NoReplyAction implements Actionable {

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
		return null;
	}

}
