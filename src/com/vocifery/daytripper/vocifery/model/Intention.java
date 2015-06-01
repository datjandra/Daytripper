package com.vocifery.daytripper.vocifery.model;

public enum Intention {
	
	PRICES("https://api.uber.com/v1/estimates/price"),
	DEFAULT("/default");
	
	private String value;
	
	private Intention(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
}
