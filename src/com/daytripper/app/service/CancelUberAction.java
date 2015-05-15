package com.daytripper.app.service;

import android.content.Intent;

public class CancelUberAction implements Actionable, UberRequestConstants {
	
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
		uberRequest.setVerb(VERB_CANCEL);
		uberRequest.setObject(OBJECT_UBER);
		return UberRequestClient.doPost(uberRequest);
	}

	@Override
	public String getVerb() {
		return VERB_CANCEL;
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
