package com.vocifery.daytripper;

import java.util.ArrayList;

import android.app.Application;
import android.content.Context;

import com.mapquest.android.maps.GeoPoint;
import com.vocifery.daytripper.vocifery.model.Searchable;

public class Daytripper extends Application {

	private GeoPoint selectedPoint;
	private ArrayList<Searchable> allItems;
	private static Context context;
	
	public void onCreate() {
		super.onCreate();
		Daytripper.context = getApplicationContext();
	}

	public GeoPoint getSelectedPoint() {
		return selectedPoint;
	}

	public void setSelectedPoint(GeoPoint selectedPoint) {
		this.selectedPoint = selectedPoint;
	}

	public ArrayList<Searchable> getAllItems() {
		return allItems;
	}

	public void setAllItems(ArrayList<Searchable> searchableList) {
		allItems = searchableList;
	}

	public final static Context getAppContext() {
        return Daytripper.context;
    }
}