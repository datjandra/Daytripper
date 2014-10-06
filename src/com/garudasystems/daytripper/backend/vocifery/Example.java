package com.garudasystems.daytripper.backend.vocifery;

public class Example {

	private String phrase;
	private String example;
	private String logo;
	
	public Example(String phrase, String example, String logo) {
		this.phrase = phrase;
		this.example = example;
		this.logo = logo;
	}
	
	public String getPhrase() {
		return phrase;
	}
	
	public String getExample() {
		return example;
	}
	
	public void setPhrase(String phrase) {
		this.phrase = phrase;
	}
	
	public void setExample(String example) {
		this.example = example;
	}

	public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}
}
