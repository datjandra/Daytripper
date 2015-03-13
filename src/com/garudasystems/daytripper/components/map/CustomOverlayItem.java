package com.garudasystems.daytripper.components.map;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.OverlayItem;

public class CustomOverlayItem extends OverlayItem {

	private StateListDrawable marker = null;
	
	public CustomOverlayItem(GeoPoint point, String title, String snippet) {
		super(point, title, snippet);
	}

}
