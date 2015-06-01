package com.vocifery.daytripper.service;

import com.vocifery.daytripper.ui.MainActivity;

import android.content.Intent;

public class ShutUpAction implements Actionable {

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
		intent.putExtra(MainActivity.VOCIFEROUS_KEY, false);
		return Boolean.toString(false);
	}

	@Override
	public String getCustomMessage() {
		// TODO Auto-generated method stub
		return null;
	}

}
