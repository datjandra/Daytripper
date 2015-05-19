package com.daytripper.app.service;

public interface UberRequestListener {
	public void sendingRequest();
	public void stopRequest();
	public void onNoResponse();
	public void onRequestSent();
}
