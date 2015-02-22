package com.garudasystems.daytripper.view;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.backend.vocifery.QueryResponse;
import com.garudasystems.daytripper.backend.vocifery.Result;
import com.garudasystems.daytripper.components.Refreshable;
import com.garudasystems.daytripper.components.RetainableFragment;
import com.garudasystems.daytripper.components.ShowListFragment;
import com.garudasystems.daytripper.components.ShowMapFragment;
import com.garudasystems.daytripper.components.SimplerExpandableListAdapter;
import com.garudasystems.daytripper.components.ViewPagerFragment;
import com.mapquest.android.maps.AnnotationView;
import com.mapquest.android.maps.DefaultItemizedOverlay;
import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.ItemizedOverlay;
import com.mapquest.android.maps.MapController;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.OverlayItem;

public class MainActivity extends FragmentActivity implements LocationListener,
		Refreshable, TextToSpeech.OnInitListener {

	private static final String TAG = "MainActivity";
	private static final long MEASURE_TIME = 1000 * 60;
	private static final long POLLING_FREQ = 1000 * 20;
	private static final long ONE_MIN = 1000 * 60;
	private static final long TWO_MIN = ONE_MIN * 2;
	private static final long FIVE_MIN = ONE_MIN * 5;
	private static final long TEN_MIN = FIVE_MIN * 2;
	private static final float MIN_LAST_READ_ACCURACY = 1000.0f;
	private static final float MIN_ACCURACY = 50.0f;
	private static final float MIN_DISTANCE = 20.0f;
	
	private LocationManager locationManager;
	private Location location;
	private String cachedQuery;
	private TextToSpeech tts;
	private ProgressBar mainProgressBar;
	private SearchView searchView;
	private RetainableFragment retainableFragment;
	private QueryResponse cachedResponse;
	private AnnotationView annotationView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.actionbar_custom);
        		
		tts = new TextToSpeech(this, this);
        mainProgressBar = (ProgressBar) findViewById(R.id.main_progress);
		
		searchView = (SearchView) findViewById(R.id.search_view);
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String text) {
				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				mainProgressBar.setVisibility(View.VISIBLE);				
				String showListFragmentTag = getFragmentTag(R.id.viewpager, 
						SearchActivityTabAdapter.LIST_FRAGMENT_INDEX);
				if (showListFragmentTag != null) {
					Fragment fragment = getFragmentByTag(showListFragmentTag);
					if (fragment != null) {
						((ShowListFragment) fragment).reset();
					}
				}
				return false;
			}
		});

		initLocationManager();
		location = bestLastKnownLocation(MIN_LAST_READ_ACCURACY, TEN_MIN);
		
		if (savedInstanceState == null) {
			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction transaction = fm.beginTransaction();
			ViewPagerFragment fragment = new ViewPagerFragment();
			transaction.replace(R.id.sample_content_fragment, fragment);
			transaction.commit();
		} else {
			cachedResponse = savedInstanceState.getParcelable(QueryResponse.class.getName());
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_actions, menu);
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.help_menu) {
			final Context context = this;
			final Dialog dialog = new Dialog(context);
			dialog.setContentView(R.layout.help_content);
			
			TextView helpClose = (TextView) dialog.findViewById(R.id.help_close);
			helpClose.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
			
			String[] titles = {
				getResources().getString(R.string.seatgeek_instruction),
				getResources().getString(R.string.yelp_instruction),
				getResources().getString(R.string.filter_instruction),
				getResources().getString(R.string.sort_instruction),
				getResources().getString(R.string.similarity_instruction),
			};
			
			String[][] contents = {
				getResources().getStringArray(R.array.seatgeek_examples),
				getResources().getStringArray(R.array.yelp_examples),
				getResources().getStringArray(R.array.filter_examples),
				getResources().getStringArray(R.array.sort_examples),
				getResources().getStringArray(R.array.similarity_examples)
			};
			
			SimplerExpandableListAdapter expandableAdapter = new SimplerExpandableListAdapter(context, titles, contents);
			ExpandableListView helpContent = (ExpandableListView) dialog.findViewById(R.id.help_content);
			helpContent.setAdapter(expandableAdapter);
			dialog.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onLocationChanged(Location updatedLocation) {
		if (location == null || updatedLocation.getAccuracy() < location.getAccuracy()) {
			location = updatedLocation;
			if (location.getAccuracy() < MIN_ACCURACY) {
				locationManager.removeUpdates(this);
			}
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle bundle) {
	}
	
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = tts.setLanguage(Locale.US);
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e(TAG, "Language is not available.");
			}
		} else {
			Log.e(TAG, "Could not initialize TextToSpeech.");
		}
	}

	@Override
	public void refresh(int page, int count) {
		String locationString = null;
		if (location != null) {
			locationString = location.getLatitude() + ", "
					+ location.getLongitude();
		}
		Log.i(TAG, "refresh - sending query " + cachedQuery + " with location "
				+ locationString);
		startWork(cachedQuery, locationString, page, count);
	}
	
	@Override
	public void receivedResponse(QueryResponse queryResponse) {
		if (mainProgressBar.isShown()) {
			mainProgressBar.setVisibility(View.INVISIBLE);
		}

		if (queryResponse == null || queryResponse.getSource() == null
				|| queryResponse.getTotal() == null
				|| queryResponse.getTotal() <= 0) {
			String message = getMessage(R.string.error_message);
			say(message);
			showToast(message, Toast.LENGTH_SHORT);
			return;
		}

		boolean reload = false;
		Integer page = queryResponse.getPage();
		if (page != null && page <= 1) {
			reload = true;
			String template = getMessage(R.string.success_message);
			String message = String.format(Locale.getDefault(), template,
					queryResponse.getTotal(), queryResponse.getSource());
			say(message);
			showToast(message, Toast.LENGTH_SHORT);
		}

		String showListFragmentTag = getFragmentTag(R.id.viewpager,
				SearchActivityTabAdapter.LIST_FRAGMENT_INDEX);
		if (showListFragmentTag != null) {
			Fragment fragment = getFragmentByTag(showListFragmentTag);
			if (fragment != null) {
				((ShowListFragment) fragment)
						.refreshList(queryResponse, reload);
			}
		}

		String supportMapFragmentTag = getFragmentTag(R.id.viewpager,
				SearchActivityTabAdapter.MAP_FRAGMENT_INDEX);
		if (supportMapFragmentTag != null) {
			Fragment fragment = getFragmentByTag(supportMapFragmentTag);
			if (fragment != null) {
				updateMap(queryResponse.getResultList(),
						(ShowMapFragment) fragment, reload);
			}
		}
		
		if (cachedResponse == null || cachedResponse != queryResponse) {
			cachedResponse = queryResponse;
		}
		lockOrientation(true);
	}
	
	@Override
	public void requestDenied(String reason) {
		showToast(reason, Toast.LENGTH_SHORT);
		say(reason);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedState) {
	    super.onSaveInstanceState(savedState);
	    savedState.putParcelable(QueryResponse.class.getName(), cachedResponse);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		final LocationListener listener = this;
		if (locationManager != null && location != null) {
			if (location.getAccuracy() > MIN_LAST_READ_ACCURACY || location.getTime() < (System.currentTimeMillis() - TWO_MIN)) {
				locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, POLLING_FREQ, MIN_DISTANCE, this);
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, POLLING_FREQ, MIN_DISTANCE, this);
				Executors.newScheduledThreadPool(1).schedule(new Runnable() {
					@Override
					public void run() {
						locationManager.removeUpdates(listener);
					}
				}, MEASURE_TIME, TimeUnit.MILLISECONDS);
			}
		}
		
		if (cachedResponse != null) {
			receivedResponse(cachedResponse);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (locationManager != null) {
			locationManager.removeUpdates(this);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}
	
	private int getCurentOrientation() {
	    Display d = ((WindowManager) getSystemService(WINDOW_SERVICE))
	            .getDefaultDisplay();
	    DisplayMetrics displaymetrics = new DisplayMetrics();
	    getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
	    int screenWidth = displaymetrics.widthPixels;
	    int screenHeight = displaymetrics.heightPixels;
	    boolean isWide = screenWidth >= screenHeight;
	    switch (d.getRotation()) {
	    case Surface.ROTATION_0:
	        return isWide ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
	                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
	    case Surface.ROTATION_90:
	        return isWide ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
	                : ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
	    case Surface.ROTATION_180:
	        return isWide ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
	                : ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
	    case Surface.ROTATION_270:
	        return isWide ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
	                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
	    }
	    return -1;
	}

	private void lockOrientation(boolean lock) {
	    if (lock) {
	        setRequestedOrientation(getCurentOrientation());
	    } else {
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
	    }
	}
	
	private void startWork(String query, String locationString, int page, int count) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		retainableFragment = (RetainableFragment) fragmentManager.findFragmentByTag(RetainableFragment.TAG);
		if (retainableFragment == null) {
			retainableFragment = new RetainableFragment();
			fragmentManager.beginTransaction().add(retainableFragment, RetainableFragment.TAG).commit();
		}
		retainableFragment.startWork(this, query, locationString, page, count);
	}
	
	private void initLocationManager() {
		if (locationManager == null) {
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		}
	}
	
	private Location bestLastKnownLocation(float minAccuracy, long maxAge) {
		Location updatedLocation = null;
		if (locationManager == null) {
			return null;
		}
		
		updatedLocation = locationManager
				.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
		if (updatedLocation != null) {
			float accuracy = updatedLocation.getAccuracy();
			long time = updatedLocation.getTime();
			if (accuracy <= minAccuracy || (System.currentTimeMillis() - time) <= maxAge) {
				return updatedLocation;
			} 
		}
		
		updatedLocation = locationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (updatedLocation != null) {
			float accuracy = updatedLocation.getAccuracy();
			long time = updatedLocation.getTime();
			if (accuracy <= minAccuracy || (System.currentTimeMillis() - time) <= maxAge) {
				return updatedLocation;
			} 
		}
		
		updatedLocation = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (updatedLocation != null) {
			float accuracy = updatedLocation.getAccuracy();
			long time = updatedLocation.getTime();
			if (accuracy <= minAccuracy || (System.currentTimeMillis() - time) <= maxAge) {
				return updatedLocation;
			} 
		}
		return updatedLocation;
	}

	private void handleIntent(Intent intent) {
		try {
			if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
				String query = intent.getStringExtra(SearchManager.QUERY);
				if (query == null || query.isEmpty()) {
					Log.i(TAG, "query is null");
					return;
				}
				query = query.trim();
				
				if (searchView.getQuery() == null || searchView.getQuery().length() == 0) {
					searchView.setQuery(query, false);
				}
				cachedQuery = query;

				String locationString = null;
				if (location != null) {
					locationString = location.getLatitude() + ", "
							+ location.getLongitude();
				}

				Log.i(TAG, "handleIntent - sending query " + query
						+ " with location " + locationString);
				startWork(query, locationString, 0, 0);
				lockOrientation(false);
			}
		} finally {
			searchView.clearFocus();
		}
	}

	private Fragment getFragmentByTag(String tag) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
		return fragment;
	}

	private void showToast(final String text, final int length) {
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(getApplicationContext(), text, length).show();
			}
		});
	}

	private void say(final String text) {
		try {
			runOnUiThread(new Runnable() {
				public void run() {
					if (tts != null) {
						tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
					}
				}
			});
		} catch (Exception e) {
		}
	}

	private String getMessage(int resourceId) {
		return getResources().getString(resourceId);
	}
	
	private static String getFragmentTag(int viewId, int index) {
		return "android:switcher:" + viewId + ":" + index;
	}

	private static void addPointsToMap(Context context, List<Result> resultList, MapView mapView, 
			final AnnotationView annotation, final TextView bubbleTitle, final TextView bubbleSnippet) {
		int minLat = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int minLon = Integer.MAX_VALUE;
		int maxLon = Integer.MIN_VALUE;
		
		Drawable icon = context.getResources().getDrawable(R.drawable.location_marker);
		final DefaultItemizedOverlay overlays = new DefaultItemizedOverlay(icon);
		
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
			
			OverlayItem item = new OverlayItem(geoPoint, result.getName(), result.getDetails());
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
					annotation.showAnnotationView(tapped);
				}
			}
		});
		
		mapView.getOverlays().add(overlays);
		mapView.invalidate();
		mapView.setBuiltInZoomControls(true);
		
		double fitFactor = 1.5;
		MapController mapController = mapView.getController();
		mapController.zoomToSpan((int) (Math.abs(maxLat - minLat) * fitFactor), (int)(Math.abs(maxLon - minLon) * fitFactor));
		mapController.animateTo(new GeoPoint( (maxLat + minLat)/2, (maxLon + minLon)/2 )); 
	}
	
	private void updateMap(List<Result> resultList,
			ShowMapFragment showMapFragment, boolean reload) {
		final Context context = this;
		final MapView mapView = showMapFragment.getMapView();
		if (mapView != null) {
			if (annotationView == null) {
				annotationView = new AnnotationView(mapView);
			}
			
			float density = mapView.getContext().getResources().getDisplayMetrics().density;
			annotationView.setBubbleRadius((int)(12*density+0.5f));
			annotationView.tryToKeepBubbleOnScreen(true);
			
			LayoutInflater li = (LayoutInflater) mapView.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			RelativeLayout innerView = (RelativeLayout) li.inflate(R.layout.custom_inner_view, annotationView, false);
			annotationView.setInnerView(innerView);
	
			annotationView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					((AnnotationView) view).hide();
				}
			});
				
			if (reload) {
				annotationView.hide();
				mapView.getOverlays().clear();
				mapView.invalidate();
			}
			
			TextView bubbleTitle = (TextView) innerView.findViewById(R.id.bubble_title);
			TextView bubbleSnippet = (TextView) innerView.findViewById(R.id.bubble_snippet);
			addPointsToMap(context, resultList, mapView, annotationView, bubbleTitle, bubbleSnippet);
		}
	}
}
