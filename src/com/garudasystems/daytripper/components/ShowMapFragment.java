package com.garudasystems.daytripper.components;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.garudasystems.daytripper.R;
import com.mapquest.android.maps.MapView;

public class ShowMapFragment extends Fragment {

	public static final String TAG = "ShowMapFragment";

	private MapView mapView = null;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.show_map_fragment,
				container, false);
		mapView = (MapView) rootView.findViewById(R.id.mapquest);
		return rootView;
	}
	
	 @Override
	 public void onDestroy() {
		 super.onDestroy();
		 if (mapView != null) {
			 mapView.destroy();
			 mapView = null;
		 }
	 }
	public MapView getMapView() {
		return mapView;
	}
}
