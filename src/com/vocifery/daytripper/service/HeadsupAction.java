package com.vocifery.daytripper.service;

import android.content.Intent;

public class HeadsupAction implements Actionable {

	private final static String HEADSUP_MESSAGE = "com.vocifery.daytripper.service.HeadsupAction.HEADSUP_MESSAGE";
	
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
		return this.getClass().getName();
	}

	@Override
	public String doActionWithIntent(Intent intent) {
		return HEADSUP_MESSAGE;
	}

	@Override
	public String getCustomMessage() {
		// TODO Auto-generated method stub
		return null;
	}
}
