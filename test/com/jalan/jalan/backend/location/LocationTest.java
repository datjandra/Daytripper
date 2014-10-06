package com.jalan.jalan.backend.location;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.test.AndroidTestCase;

public class LocationTest extends AndroidTestCase {

	public void testGPS() {
        LocationManager locationManager = (LocationManager) this.getContext().getSystemService(Context.LOCATION_SERVICE);
        locationManager.addTestProvider("Test", false, false, false, false, false, false, false, Criteria.POWER_LOW, Criteria.ACCURACY_FINE);
        locationManager.setTestProviderEnabled("Test", true);

        // Set up your test
        Location location = new Location("Test");
        location.setLatitude(10.0);
        location.setLongitude(20.0);
        locationManager.setTestProviderLocation("Test", location);

        // Check if your listener reacted the right way
        locationManager.removeTestProvider("Test");
    }
}
