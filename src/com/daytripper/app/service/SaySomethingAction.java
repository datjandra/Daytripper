package com.daytripper.app.service;

import com.daytripper.app.ui.MainActivity;

import android.content.Intent;

public class SaySomethingAction implements Actionable {

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
		intent.putExtra(MainActivity.VOCIFEROUS_KEY, true);
		return Boolean.toString(true);
	}

	@Override
	public String getCustomMessage() {
		// TODO Auto-generated method stub
		return null;
	}

}
