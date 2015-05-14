package com.daytripper.app.components.map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daytripper.app.Daytripper;
import com.daytripper.app.R;
import com.daytripper.app.vocifery.model.Locatable;
import com.daytripper.app.vocifery.model.Result;
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
	private ArrayList<Locatable> allItems;
	
	public static final String ITEM_STATE = "ItemState";
	public static final String RESULT_LIST = ShowMapFragment.class.getName() + "." + Result.class.getName();

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
			ArrayList<Result> resultList = savedInstanceState.getParcelableArrayList(ITEM_STATE);
			if (resultList != null) {
				allItems = new ArrayList<Locatable>();
				allItems.addAll(resultList);
			}
        }
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.show_map_fragment, container,
				false);
		mapView = (MapView) rootView.findViewById(R.id.mapquest);
		annotationView = new AnnotationView(mapView);
		annotationView.tryToKeepBubbleOnScreen(true);
		
		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				annotationView.hide();
			}
		});
		
		annotationView.setOnFocusChangeListener(new OnFocusChangeListener() {          
			@Override
	        public void onFocusChange(View v, boolean hasFocus) {
	            if (!hasFocus) {
	            	Log.i(TAG, "onFocusChange()");
	            }
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
	    savedState.putParcelableArrayList(ITEM_STATE, allItems);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (allItems != null) {
			addPointsToMap(allItems);
		}
	}
	
	public MapView getMapView() {
		return mapView;
	}
	
	public void updateMap(List<? extends Locatable> resultList, boolean reload) {
		if (resultList == null || resultList.isEmpty()) {
			return;
		}
		
		if (reload) {
			annotationView.hide();
			mapView.getOverlays().clear();
			mapView.invalidate();
			allItems = null;
		}
		
		if (allItems == null) {
			allItems = new ArrayList<Locatable>();
		}
		allItems.addAll(resultList);
		addPointsToMap(resultList);
	}
	
	private void addPointsToMap(List<? extends Locatable> resultList) {
		int minLat = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int minLon = Integer.MAX_VALUE;
		int maxLon = Integer.MIN_VALUE;
		
		Set<Pair<Double,Double>> mapPoints = new HashSet<Pair<Double,Double>>();
		Drawable defaultMarker = getActivity().getResources().getDrawable(R.drawable.default_marker);
		final DefaultItemizedOverlay overlays = new DefaultItemizedOverlay(defaultMarker);

		for (Locatable result : resultList) {
			Double latitude = result.getLatitude();
			Double longitude = result.getLongitude();
			if (latitude == null || 
					latitude == 0 || 
					longitude == null || 
					longitude == 0) {
				continue;
			}

			Pair<Double,Double> point = new Pair<Double,Double>(latitude, longitude);
			if (mapPoints.contains(point)) {
				continue;
			} else {
				mapPoints.add(point);
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

		final Daytripper daytripper = ((Daytripper) getActivity().getApplicationContext());
		overlays.setTapListener(new ItemizedOverlay.OverlayTapListener() {
			@Override
			public void onTap(GeoPoint pt, MapView mapView) {
				int lastTouchedIndex = overlays.getLastFocusedIndex();
				if (lastTouchedIndex > -1) {
					daytripper.setSelectedPoint(pt);
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
