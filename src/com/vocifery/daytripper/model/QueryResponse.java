package com.vocifery.daytripper.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	private Map<String,Integer> categoryCounts;
	
	public QueryResponse() {}
	
	@SuppressWarnings({ "unchecked", "unused" })
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
	
	public void addCategory(String categoryName) {
		if (categoryCounts == null) {
			categoryCounts = new HashMap<String,Integer>();
		}
		
		if (categoryCounts.containsKey(categoryName)) {
			categoryCounts.put(categoryName, categoryCounts.get(categoryName) + 1);
		} else {
			categoryCounts.put(categoryName, 1);
		}
	}
	
	public List<Map.Entry<String, Integer>> getSortedCategories() {
		if (categoryCounts == null || categoryCounts.isEmpty()) {
			return null;
		}
		
		List<Map.Entry<String, Integer>> sortedCategories = new ArrayList<Map.Entry<String,Integer>>(categoryCounts.entrySet());
		Collections.sort(sortedCategories, new Comparator<Map.Entry<String,Integer>>() {
			@Override
			public int compare(Entry<String, Integer> lhs,
					Entry<String, Integer> rhs) {
				if (lhs.getValue() == null && rhs.getValue() == null) {
					return 0;
				} else if (lhs.getValue() == null) {
					return -1;
				} else if (rhs.getValue() == null) {
					return 1;
				}
				return -(lhs.getValue().compareTo(rhs.getValue()));
			}
		});
		return sortedCategories;
	}
}
