package com.daytripper.app.vocifery.model;

import java.util.Collections;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class QueryResponse implements Parcelable {

	private String source;
	private String message;
	private String intent;
	private Integer page;
	private Integer total;
	private Integer chunk;
	private List<Result> resultList;
	private List<Locatable> route;
	
	public QueryResponse() {}
	
	private QueryResponse(Parcel parcel) {
		source = parcel.readString();
		intent = parcel.readString();
		page = parcel.readInt();
		total = parcel.readInt();
		chunk = parcel.readInt();
		resultList = 
			Collections.unmodifiableList(parcel.readArrayList(this.getClass().getClassLoader()));
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeString(source);
		parcel.writeString(intent);
		parcel.writeInt(page);
		parcel.writeInt(total);
		parcel.writeInt(chunk);
		parcel.writeList(resultList);
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getIntent() {
		return intent;
	}
	
	public void setIntent(String intent) {
		this.intent = intent;
	}
	
	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public Integer getTotal() {
		return total;
	}

	public void setTotal(Integer total) {
		this.total = total;
	}

	public Integer getChunk() {
		return chunk;
	}

	public void setChunk(Integer chunk) {
		this.chunk = chunk;
	}

	public List<Result> getResultList() {
		return resultList;
	}

	public void setResultList(List<Result> resultList) {
		this.resultList = Collections.unmodifiableList(resultList);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public List<Locatable> getRoute() {
		return route;
	}

	public void setRoute(List<Locatable> route) {
		this.route = Collections.unmodifiableList(route);
	}
}
