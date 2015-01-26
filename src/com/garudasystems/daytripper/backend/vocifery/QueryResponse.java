package com.garudasystems.daytripper.backend.vocifery;

import java.util.Collections;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class QueryResponse implements Parcelable {

	private String source;
	private Integer page;
	private Integer total;
	private Integer chunk;
	private List<Result> resultList;
	
	public QueryResponse() {}
	
	private QueryResponse(Parcel parcel) {
		source = parcel.readString();
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

}
