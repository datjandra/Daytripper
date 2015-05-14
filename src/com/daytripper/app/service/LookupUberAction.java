package com.daytripper.app.service;

import com.daytripper.app.ui.WebActivity;

import android.content.Intent;

public class LookupUberAction implements Actionable {
	
	@Override
	public String getSource() {
		return this.getClass().getName();
	}
	
	@Override
	public String doActionWithIntent(Intent intent) {
		return this.getClass().getName();
	}

	@Override
	public String getVerb() {
		return WebActivity.VERB_LOOKUP;
	}

	@Override
	public String getObject() {
		return WebActivity.OBJECT_UBER;
	}
	
	@Override
	public String getCustomMessage() {
		return WebActivity.LOOKUP_MESSAGE;
	}
}
