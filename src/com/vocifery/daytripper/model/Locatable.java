package com.vocifery.daytripper.model;

import android.os.Parcelable;

public interface Locatable extends Parcelable {
	
	public String getId();
	public Double getLatitude();
	public Double getLongitude();
	public String getName();
	public String getDetails();
}
