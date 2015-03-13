package com.garudasystems.daytripper.components.map;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.backend.vocifery.Result;
import com.mapquest.android.maps.AnnotationView;
import com.mapquest.android.maps.DefaultItemizedOverlay;
import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.ItemizedOverlay;
import com.mapquest.android.maps.MapController;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.OverlayItem;

public class ShowMapFragment extends Fragment {

	public static final String TAG = "ShowMapFragment";

	private MapView mapView = null;
	private AnnotationView annotationView;
	private TextView bubbleTitle;
	private TextView bubbleSnippet; 
	private ArrayList<Result> allItems;
	
	public static final String RESULT_LIST = ShowMapFragment.class.getName() + "." + Result.class.getName();

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.show_map_fragment, container,
				false);
		mapView = (MapView) rootView.findViewById(R.id.mapquest);
		annotationView = new AnnotationView(mapView);
		/*
		float density = mapView.getContext().getResources().getDisplayMetrics().density;
		annotationView.setBubbleRadius((int) (12 * density + 0.5f));
		*/
		annotationView.tryToKeepBubbleOnScreen(true);
		annotationView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				((AnnotationView) view).hide();
			}
		});
		
		LayoutInflater li = (LayoutInflater) mapView.getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		RelativeLayout innerView = (RelativeLayout) li.inflate(
				R.layout.custom_inner_view, annotationView, false);
		annotationView.setInnerView(innerView);
		
		bubbleTitle = (TextView) innerView
				.findViewById(R.id.bubble_title);
		bubbleSnippet = (TextView) innerView
				.findViewById(R.id.bubble_snippet);
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

	@Override
	public void onSaveInstanceState(Bundle savedState) {
	    super.onSaveInstanceState(savedState);
	}
	
	public MapView getMapView() {
		return mapView;
	}
	
	public void updateMap(List<Result> resultList, boolean reload) {
		if (reload) {
			annotationView.hide();
			mapView.getOverlays().clear();
			mapView.invalidate();
		}
		addPointsToMap(resultList);
	}
	
	private void addPointsToMap(List<Result> resultList) {
		int minLat = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int minLon = Integer.MAX_VALUE;
		int maxLon = Integer.MIN_VALUE;

		StateListDrawable listDrawable = 
				(StateListDrawable) getActivity().getResources().getDrawable(R.drawable.marker_icon);
		Drawable icon = getActivity().getResources().getDrawable(
				R.drawable.default_marker);
		final DefaultItemizedOverlay overlays = new DefaultItemizedOverlay(listDrawable);

		for (Result result : resultList) {
			Double latitude = result.getLatitude();
			Double longitude = result.getLongitude();
			if (latitude == null || longitude == null) {
				continue;
			}

			GeoPoint geoPoint = new GeoPoint(latitude, longitude);
			int lat = geoPoint.getLatitudeE6();
			int lon = geoPoint.getLongitudeE6();

			maxLat = Math.max(lat, maxLat);
			minLat = Math.min(lat, minLat);
			maxLon = Math.max(lon, maxLon);
			minLon = Math.min(lon, minLon);

			OverlayItem item = new OverlayItem(geoPoint, result.getName(),
					result.getDetails());
			overlays.addItem(item);
		}

		overlays.setTapListener(new ItemizedOverlay.OverlayTapListener() {
			@Override
			public void onTap(GeoPoint pt, MapView mapView) {
				int lastTouchedIndex = overlays.getLastFocusedIndex();
				if (lastTouchedIndex > -1) {
					mapView.getController().animateTo(pt);
					OverlayItem tapped = overlays.getItem(lastTouchedIndex);
					bubbleTitle.setText(tapped.getTitle());
					bubbleSnippet.setText(tapped.getSnippet());
					annotationView.showAnnotationView(tapped);
				}
			}
		});

		mapView.getOverlays().add(overlays);
		mapView.invalidate();
		mapView.setBuiltInZoomControls(true);

		double fitFactor = 1.5;
		MapController mapController = mapView.getController();
		mapController.zoomToSpan((int) (Math.abs(maxLat - minLat) * fitFactor),
				(int) (Math.abs(maxLon - minLon) * fitFactor));
		mapController.animateTo(new GeoPoint((maxLat + minLat) / 2,
				(maxLon + minLon) / 2));
	}
}
