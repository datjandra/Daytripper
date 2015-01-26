package com.garudasystems.daytripper.backend.vocifery;

public class Example {

	private String instruction;
	private String example;
	private String logo;
	
	public Example(String instruction, String example, String logo) {
		this.instruction = instruction;
		this.example = example;
		this.logo = logo;
	}
	
	public String getExample() {
		return example;
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

	public String getInstruction() {
		return instruction;
	}

	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}
}
