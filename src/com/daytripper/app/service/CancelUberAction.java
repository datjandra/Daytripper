package com.daytripper.app.service;

import android.content.Intent;

import com.daytripper.app.ui.WebActivity;

public class CancelUberAction implements Actionable {
	
	@Override
	public String getSource() {
		return this.getClass().getName();
	}
	
	@Override
	public String doActionWithIntent(Intent intent) {
		return null;
	}

	@Override
	public String getVerb() {
		return WebActivity.VERB_CANCEL;
	}

	@Override
	public String getObject() {
		return WebActivity.OBJECT_UBER;
	}
	
	@Override
	public String getCustomMessage() {
		return WebActivity.CANCEL_MESSAGE;
	}
}
