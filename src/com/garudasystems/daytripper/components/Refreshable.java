package com.garudasystems.daytripper.components;

import com.garudasystems.daytripper.backend.vocifery.QueryResponse;

public interface Refreshable {
	public void receivedResponse(QueryResponse response);
	public void refresh(int page, int count);
	public void requestDenied(String reason);
}
