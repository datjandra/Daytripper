package com.vocifery.daytripper.model;

import android.os.Parcel;

public class LocatableItem implements Locatable {

	private String id;
	private String name;
	private String details;
	private Double latitude;
	private Double longitude;
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(id);
		parcel.writeString(name);
		parcel.writeString(details);
		parcel.writeDouble(latitude);
		parcel.writeDouble(longitude);
	}

	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public Double getLatitude() {
		return latitude;
	}

	@Override
	public Double getLongitude() {
		return longitude;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDetails() {
		return details;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
}
