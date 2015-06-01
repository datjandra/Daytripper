package com.vocifery.daytripper.service;

public class UberRequest {

	private String verb;
	private String object;
	private String accessToken;
	private String code;
	private String requestId;
	private String productId;
	private String startLatitude;
	private String endLatitude;
	private String startLongitude;
	private String endLongitude;
	private String surgeConfirmationId;
	private String method;
	private Boolean testMode;
	
	public String getVerb() {
		return verb;
	}
	
	public void setVerb(String verb) {
		this.verb = verb;
	}
	
	public String getObject() {
		return object;
	}
	
	public void setObject(String object) {
		this.object = object;
	}
	
	public String getAccessToken() {
		return accessToken;
	}
	
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getRequestId() {
		return requestId;
	}
	
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	
	public String getProductId() {
		return productId;
	}
	
	public void setProductId(String productId) {
		this.productId = productId;
	}
	
	public String getStartLatitude() {
		return startLatitude;
	}
	
	public void setStartLatitude(String startLatitude) {
		this.startLatitude = startLatitude;
	}
	
	public String getEndLatitude() {
		return endLatitude;
	}
	
	public void setEndLatitude(String endLatitude) {
		this.endLatitude = endLatitude;
	}
	
	public String getStartLongitude() {
		return startLongitude;
	}
	
	public void setStartLongitude(String startLongitude) {
		this.startLongitude = startLongitude;
	}
	
	public String getEndLongitude() {
		return endLongitude;
	}
	
	public void setEndLongitude(String endLongitude) {
		this.endLongitude = endLongitude;
	}
	
	public String getSurgeConfirmationId() {
		return surgeConfirmationId;
	}
	
	public void setSurgeConfirmationId(String surgeConfirmationId) {
		this.surgeConfirmationId = surgeConfirmationId;
	}
	
	public Boolean getTestMode() {
		return testMode;
	}
	
	public void setTestMode(Boolean testMode) {
		this.testMode = testMode;
	}
	
	public String getMethod() {
		return method;
	}
	
	public void setMethod(String method) {
		this.method = method;
	}
}
