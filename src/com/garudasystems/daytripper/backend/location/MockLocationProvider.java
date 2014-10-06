package com.garudasystems.daytripper.backend.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

public class MockLocationProvider {

	private String providerName;
	private LocationManager locationManager;
	
	private final static float ACCURACY = 5f;
	
	public MockLocationProvider(String providerName, Context ctx) {
		this.providerName = providerName;
		locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
		locationManager.addTestProvider(providerName, false, false, false,
				false, true, true, true, 0, 5);
		locationManager.setTestProviderEnabled(providerName, true);
	}
	
	public void pushLocation(double lat, double lon) {
		Location location = new Location(providerName);
		location.setLatitude(lat);
		location.setLongitude(lon);
		location.setAltitude(0);
		location.setTime(System.currentTimeMillis());
		location.setAccuracy(ACCURACY);
		locationManager.setTestProviderLocation(providerName, location);
	}
	
	public void shutdown() {
		locationManager.removeTestProvider(providerName);
	}

}
