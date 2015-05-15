package com.daytripper.app.service;

import android.content.Intent;

public class LookupUberAction implements Actionable, UberRequestConstants {
	
	@Override
	public String getSource() {
		return this.getClass().getName();
	}
	
	@Override
	public String doActionWithIntent(Intent intent) {
		String accessToken = intent.getStringExtra(FIELD_ACCESS_TOKEN);
		String requestId = intent.getStringExtra(FIELD_REQUEST_ID);
		
		UberRequest uberRequest = new UberRequest();
		uberRequest.setAccessToken(accessToken);
		uberRequest.setRequestId(requestId);
		uberRequest.setVerb(VERB_LOOKUP);
		uberRequest.setObject(OBJECT_UBER);
		return UberRequestClient.doPost(uberRequest);
	}

	@Override
	public String getVerb() {
		return VERB_LOOKUP;
	}

	@Override
	public String getObject() {
		return OBJECT_UBER;
	}
	
	@Override
	public String getCustomMessage() {
		return null;
	}
}
