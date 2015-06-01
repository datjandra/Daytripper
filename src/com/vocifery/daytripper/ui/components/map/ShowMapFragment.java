package com.vocifery.daytripper.ui.components.map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mapquest.android.Geocoder;
import com.mapquest.android.maps.AnnotationView;
import com.mapquest.android.maps.DefaultItemizedOverlay;
import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.ItemizedOverlay;
import com.mapquest.android.maps.LineOverlay;
import com.mapquest.android.maps.MapController;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.OverlayItem;
import com.vocifery.daytripper.Daytripper;
import com.vocifery.daytripper.R;
import com.vocifery.daytripper.util.StringConstants;
import com.vocifery.daytripper.vocifery.model.Locatable;
import com.vocifery.daytripper.vocifery.model.Result;

public class ShowMapFragment extends Fragment implements StringConstants {

	public static final String TAG = "ShowMapFragment";

	private MapView mapView = null;
	private AnnotationView annotationView;
	private TextView bubbleTitle;
	private TextView bubbleSnippet; 
	private ArrayList<Locatable> allItems;
	private Geocoder geocoder;
	private CardView routeInfo;
	private TextView startAddress;
	private TextView endAddress;
	
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
        geocoder = new Geocoder(getActivity());
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.show_map_fragment, container,
				false);
		mapView = (MapView) rootView.findViewById(R.id.mapquest);
		annotationView = new AnnotationView(mapView);
		annotationView.tryToKeepBubbleOnScreen(true);
		
		annotationView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				annotationView.hide();
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
		
		routeInfo = (CardView) rootView.findViewById(R.id.route_info);
		startAddress = (TextView) routeInfo.findViewById(R.id.start_address);
		endAddress = (TextView) routeInfo.findViewById(R.id.end_address);
		return rootView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mapView != null) {
			mapView.destroy();
			mapView = null;
		}
		
		if (geocoder != null) {
			geocoder = null;
		}
		
		final Daytripper daytripper = ((Daytripper) getActivity().getApplicationContext());
		daytripper.setSelectedPoint(null);
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
		routeInfo.setVisibility(View.GONE);
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
	
	public void updateMapWithRoute(List<? extends Locatable> route, boolean reload) {
		routeInfo.setVisibility(View.GONE);
		if (route == null || route.isEmpty()) {
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
		allItems.addAll(route);
		addRouteToMap(route.get(0), route.get(route.size()-1));
	}
	
	public void zoom(int zoomLevel) {
		if (mapView != null) {
			final Daytripper daytripper = ((Daytripper) getActivity().getApplicationContext());
			if (daytripper != null) {
				GeoPoint selectedPoint = daytripper.getSelectedPoint();
				if (selectedPoint != null) {
					mapView.getController().animateTo(selectedPoint);
				}
			}
			mapView.getController().setZoom(zoomLevel);
		}
	}
	
	private void addRouteToMap(Locatable start, Locatable end) {
		int minLat = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int minLon = Integer.MAX_VALUE;
		int maxLon = Integer.MIN_VALUE;
		
		final DefaultItemizedOverlay itemizedOverlays = 
				new DefaultItemizedOverlay(getResources().getDrawable(R.drawable.default_marker));
		itemizedOverlays.setTapListener(new ItemizedOverlay.OverlayTapListener() {
			@Override
			public void onTap(GeoPoint pt, MapView mapView) {
				int lastTouchedIndex = itemizedOverlays.getLastFocusedIndex();
				if (lastTouchedIndex > -1) {
					mapView.getController().animateTo(pt);
					OverlayItem tapped = itemizedOverlays.getItem(lastTouchedIndex);
					bubbleTitle.setText(tapped.getTitle());
					bubbleSnippet.setText(tapped.getSnippet());
					annotationView.showAnnotationView(tapped);
				}
			}
		});
		
		List<GeoPoint> routeData = new ArrayList<GeoPoint>();
		Double latitude = start.getLatitude();
		Double longitude = start.getLongitude();
		GeoPoint geoPoint = new GeoPoint(latitude, longitude);
		routeData.add(geoPoint);
		
		Drawable dirStart = getResources().getDrawable(R.drawable.people);
		dirStart.setBounds(
			    0 - dirStart.getIntrinsicWidth() / 2, 0 - dirStart.getIntrinsicHeight(), 
			    dirStart.getIntrinsicWidth() / 2, 0);
		
		String snippet = String.format(Locale.getDefault(),
				"%10.6f, %10.6f", latitude, longitude);
		OverlayItem overlayItem = new OverlayItem(geoPoint, "Pick-up", snippet);
		overlayItem.setMarker(dirStart);
		new ReverseGeocodeTask(startAddress).execute(geoPoint);
		itemizedOverlays.addItem(overlayItem);
		
		int lat = geoPoint.getLatitudeE6();
		int lon = geoPoint.getLongitudeE6();

		maxLat = Math.max(lat, maxLat);
		minLat = Math.min(lat, minLat);
		maxLon = Math.max(lon, maxLon);
		minLon = Math.min(lon, minLon);

		latitude = end.getLatitude();
		longitude = end.getLongitude();
		geoPoint = new GeoPoint(latitude, longitude);
		lat = geoPoint.getLatitudeE6();
		lon = geoPoint.getLongitudeE6();
		routeData.add(geoPoint);
		
		Drawable dirEnd = getResources().getDrawable(R.drawable.place);
		dirEnd.setBounds(
			    0 - dirEnd.getIntrinsicWidth() / 2, 0 - dirEnd.getIntrinsicHeight(), 
			    dirEnd.getIntrinsicWidth() / 2, 0);
		
		snippet = String.format(Locale.getDefault(),
				"%10.6f, %10.6f", latitude, longitude);
		overlayItem = new OverlayItem(geoPoint, "Drop-off", snippet);
		overlayItem.setMarker(dirEnd);
		new ReverseGeocodeTask(endAddress).execute(geoPoint);
		itemizedOverlays.addItem(overlayItem);

		maxLat = Math.max(lat, maxLat);
		minLat = Math.min(lat, minLat);
		maxLon = Math.max(lon, maxLon);
		minLon = Math.min(lon, minLon);
		
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(getResources().getColor(R.color.primary_dark));
        paint.setAlpha(100);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(5);
        
        LineOverlay line = new LineOverlay(paint);
        line.setData(routeData);
        line.setShowPoints(true, null);
        
        mapView.getOverlays().add(line);
        mapView.getOverlays().add(itemizedOverlays);
		mapView.invalidate();
		mapView.setBuiltInZoomControls(true);
		
		double fitFactor = 1.5;
		MapController mapController = mapView.getController();
		mapController.zoomToSpan((int) (Math.abs(maxLat - minLat) * fitFactor),
				(int) (Math.abs(maxLon - minLon) * fitFactor));
		mapController.animateTo(new GeoPoint((maxLat + minLat) / 2,
				(maxLon + minLon) / 2));
		routeInfo.setVisibility(View.VISIBLE);
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
					mapView.getController().animateTo(pt);
					OverlayItem tapped = overlays.getItem(lastTouchedIndex);
					bubbleTitle.setText(tapped.getTitle());
					bubbleSnippet.setText(tapped.getSnippet());
					annotationView.showAnnotationView(tapped);
					daytripper.setSelectedPoint(tapped.getPoint());
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
	
	private class ReverseGeocodeTask extends AsyncTask<GeoPoint, Void, List<Address>> {
		
		private TextView addressField;
		private String defaultSnippet;
		
		private ReverseGeocodeTask(TextView addressField) {
			this.addressField = addressField;
		}
		
        protected List<Address> doInBackground(GeoPoint... geoPoint) {
            try {
                return geocoder.getFromLocation(geoPoint[0].getLatitude(), geoPoint[0].getLongitude(), 1);
            } catch (Exception e) {
            	defaultSnippet = String.format(Locale.getDefault(),
						"%10.6f, %10.6f", geoPoint[0].getLatitude(), geoPoint[0].getLongitude());
                Log.w(TAG, e.getMessage());
                return null;
            }
        }

        protected void onPostExecute(List<Address> result) {
            if (result != null && !result.isEmpty()) {
                Address address = result.get(0);
                if (address == null) {
                	addressField.setText(defaultSnippet);
                	return;
                }
                
                StringBuilder addressText = new StringBuilder();
                for (int i=0; i < address.getMaxAddressLineIndex(); i++) {
                	if (i < address.getMaxAddressLineIndex() - 1) {
                		addressText.append(address.getAddressLine(i) + ", ");
                	} else {
                		addressText.append(address.getAddressLine(i));
                	}
                }
                addressField.setText(addressText.toString().trim());
            } else if (!TextUtils.isEmpty(defaultSnippet)) {
            	addressField.setText(defaultSnippet);
            }
        }
    }
}
