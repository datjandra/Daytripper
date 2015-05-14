package com.daytripper.app.vocifery.model;

public interface Searchable extends Locatable {

	public String getMobileUrl();
	public String getImageOneUrl();
	public String getImageTwoUrl();
	public String getRatingImgUrl();
	public String getRatingImgUrlSmall();
	public String getAddressOne();
	public String getAddressTwo();
	public String getPrice();
	public String getDeal();
	public Integer getReviewCount();
}
