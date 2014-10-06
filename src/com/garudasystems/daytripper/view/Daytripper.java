package com.garudasystems.daytripper.view;

import android.app.Application;
import android.content.Context;

public class Daytripper extends Application {

	private static Context context;
	
	public void onCreate() {
		super.onCreate();
		Daytripper.context = getApplicationContext();
	}

	public final static Context getAppContext() {
        return Daytripper.context;
    }
}
