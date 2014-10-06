package com.garudasystems.daytripper.backend.vocifery;

import android.os.Parcel;
import android.os.Parcelable;

public class SearchResult implements Parcelable {

	private String name;
	private String mobileUrl;
	private String imageUrl;
	private String ratingImgUrl;
	private String ratingImgUrlSmall;
	private String addressOne;
	private String addressTwo;
	private String displayPhone;
	private String price;
	private String deal;
	private Integer reviewCount;
	private Double latitude;
	private Double longitude;
	
	public static final Parcelable.Creator<SearchResult> CREATOR = new Parcelable.Creator<SearchResult>() {
		public SearchResult createFromParcel(Parcel in) {
			return new SearchResult(in);
		}

		public SearchResult[] newArray(int size) {
			return new SearchResult[size];
		}
	};
	
	public SearchResult() {}
	
	public SearchResult(String name, String addressOne, String addressTwo, String phone) {
		this.name = name;
		this.addressOne = addressOne;
		this.addressTwo = addressTwo;
		this.displayPhone = phone;
	}
	
	private SearchResult(Parcel parcel) {
		name = parcel.readString();
		mobileUrl = parcel.readString();
		imageUrl = parcel.readString();
		ratingImgUrl = parcel.readString();
		ratingImgUrlSmall = parcel.readString();
		addressOne = parcel.readString();
		addressTwo = parcel.readString();
		displayPhone = parcel.readString();
		price = parcel.readString();
		deal = parcel.readString();
		reviewCount = parcel.readInt();
		latitude = parcel.readDouble();
		longitude = parcel.readDouble();
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getAddressOne() {
		return addressOne;
	}
	
	public void setAddressOne(String addressOne) {
		this.addressOne = addressOne;
	}
	
	public String getAddressTwo() {
		return addressTwo;
	}
	
	public void setAddressTwo(String addressTwo) {
		this.addressTwo = addressTwo;
	}
	
	public String getDisplayPhone() {
		return displayPhone;
	}
	
	public void setDisplayPhone(String displayPhone) {
		this.displayPhone = displayPhone;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getDeal() {
		return deal;
	}

	public void setDeal(String deal) {
		this.deal = deal;
	}

	public Integer getReviewCount() {
		return reviewCount;
	}

	public void setReviewCount(Integer reviewCount) {
		this.reviewCount = reviewCount;
	}

	public String getMobileUrl() {
		return mobileUrl;
	}

	public void setMobileUrl(String mobileUrl) {
		this.mobileUrl = mobileUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getRatingImgUrlSmall() {
		return ratingImgUrlSmall;
	}

	public void setRatingImgUrlSmall(String ratingImgUrlSmall) {
		this.ratingImgUrlSmall = ratingImgUrlSmall;
	}

	public String getRatingImgUrl() {
		return ratingImgUrl;
	}

	public void setRatingImgUrl(String ratingImgUrl) {
		this.ratingImgUrl = ratingImgUrl;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitudeStr) {
		try {
			latitude = Double.valueOf(latitudeStr);
		} catch (Exception e) {}
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitudeStr) {
		try {
			longitude = Double.valueOf(longitudeStr);
		} catch (Exception e) {}	
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(name);
		parcel.writeString(mobileUrl);
		parcel.writeString(imageUrl);
		parcel.writeString(ratingImgUrl);
		parcel.writeString(ratingImgUrlSmall);
		parcel.writeString(addressOne);
		parcel.writeString(addressTwo);
		parcel.writeString(displayPhone);
		parcel.writeString(price);
		parcel.writeString(deal);
		parcel.writeInt(reviewCount);
		parcel.writeDouble(latitude);
		parcel.writeDouble(longitude);
	}
}
