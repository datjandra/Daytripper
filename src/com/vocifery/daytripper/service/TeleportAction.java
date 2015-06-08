package com.vocifery.daytripper.service;

import java.util.Locale;

import com.mapquest.android.maps.GeoPoint;
import com.vocifery.daytripper.Daytripper;

import android.content.Intent;

public class TeleportAction extends DefaultAction {

	@Override
	public String getSource() {
		return this.getClass().getName();
	}
	
	@Override
	protected String getLocationString(Intent intent) {
		final Daytripper daytripper = (Daytripper) Daytripper.getAppContext();
		GeoPoint selectedPoint = daytripper.getSelectedPoint();
		if (selectedPoint != null) {
			String selectedLocation = String.format(Locale.getDefault(),
					"%10.6f, %10.6f", selectedPoint.getLatitude(), selectedPoint.getLongitude());
			return selectedLocation;
		}
		return intent.getStringExtra(ResponderService.KEY_lOCATION);
	}
}
