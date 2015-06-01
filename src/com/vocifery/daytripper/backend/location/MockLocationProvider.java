package com.vocifery.daytripper.backend.location;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;

public class MockLocationProvider {

	private String providerName;
	private LocationManager locationManager;
	
	private final static int POWER = Criteria.POWER_LOW;
	private final static int ACCURACY = Criteria.ACCURACY_FINE;
	
	private final static double MOCK_LAT = 37.775115;
	private final static double MOCK_LON = -122.417368;
	
	public MockLocationProvider(String providerName, Context ctx) {
		this.providerName = providerName;
		locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
		
		 LocationProvider lp = locationManager.getProvider(providerName);
		 if (lp != null) {
			 locationManager.removeTestProvider(providerName);
		 }
		
		locationManager.addTestProvider(
				providerName, 
				false, 
				false, 
				false,
				false, 
				true, 
				true, 
				true, 
				POWER, 
				ACCURACY);
		locationManager.setTestProviderEnabled(providerName, true);
	}
	
	public void pushTestLocation() {
		pushLocation(MOCK_LAT, MOCK_LON);
	}
	
	public void pushLocation(double lat, double lon) {
		Location location = new Location(providerName);
		location.setLatitude(lat);
		location.setLongitude(lon);
		location.setAltitude(0);
		location.setTime(System.currentTimeMillis());
		location.setAccuracy(ACCURACY);
		try {
			Method locationJellyBeanFixMethod = Location.class.getMethod("makeComplete");
			if (locationJellyBeanFixMethod != null) {
			   locationJellyBeanFixMethod.invoke(location);
			}
		} 
		catch (NoSuchMethodException e) {} 
		catch (IllegalAccessException e) {} 
		catch (IllegalArgumentException e) {} 
		catch (InvocationTargetException e) {}
		locationManager.setTestProviderLocation(providerName, location);
	}
	
	public void shutdown() {
		locationManager.removeTestProvider(providerName);
	}
}
