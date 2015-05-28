package com.daytripper.app.service;

import com.daytripper.app.util.QueryParser;

import android.content.Intent;

public class MapZoomAction implements Actionable {

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
		String query = intent.getStringExtra(ResponderService.KEY_QUERY);
		Integer zoom = QueryParser.extractZoomFromQuery(query);
		return (zoom != null ? zoom.toString() : null);
	}

	@Override
	public String getCustomMessage() {
		return null;
	}

}
