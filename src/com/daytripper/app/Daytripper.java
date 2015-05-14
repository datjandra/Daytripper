package com.daytripper.app;

import android.app.Application;
import android.content.Context;

import com.mapquest.android.maps.GeoPoint;

public class Daytripper extends Application {

	private GeoPoint selectedPoint;
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

	public final static Context getAppContext() {
        return Daytripper.context;
    }
}
