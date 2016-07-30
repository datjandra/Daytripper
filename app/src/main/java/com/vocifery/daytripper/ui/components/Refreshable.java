package com.vocifery.daytripper.ui.components;

import com.vocifery.daytripper.model.QueryResponse;

public interface Refreshable {
	public void receivedResponse(String response, boolean vocalize);
	public void refresh(int page, int count);
	public void requestDenied(String reason);
	public void startProgress();
	public void stopProgress();
	public void cancel();
}
