package com.garudasystems.daytripper.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.backend.vocifery.QueryResponse;
import com.garudasystems.daytripper.components.Refreshable;
import com.garudasystems.daytripper.components.RetainableFragment;
import com.garudasystems.daytripper.components.ShowListFragment;
import com.garudasystems.daytripper.components.ViewPagerFragment;
import com.garudasystems.daytripper.components.map.ShowMapFragment;

public class MainActivity extends FragmentActivity implements LocationListener,
		Refreshable, TextToSpeech.OnInitListener {

	private static final String TAG = "MainActivity";
	private static final String CACHED_QUERY_STATE = "CachedQuery";
	
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
	// private RetainableFragment retainableFragment;
	private LinearLayout mainContent;
	private LinearLayout teaserContent;
	
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
        mainContent = (LinearLayout) findViewById(R.id.main_content);
        teaserContent = (LinearLayout) findViewById(R.id.teaser_content);
        
        int orientation = getCurrentOrientation();
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || 
        		orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
        	ImageView imageView = (ImageView) teaserContent.findViewById(R.id.img_restaurants);
        	imageView.setImageDrawable(null);
        	
        	imageView = (ImageView) teaserContent.findViewById(R.id.img_concerts);
        	imageView.setImageDrawable(null);
        	
        	imageView = (ImageView) teaserContent.findViewById(R.id.img_meetups);
        	imageView.setImageDrawable(null);
        }
		
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
				/*
				String showListFragmentTag = getFragmentTag(R.id.viewpager, 
						SearchActivityTabAdapter.LIST_FRAGMENT_INDEX);
				if (showListFragmentTag != null) {
					Fragment fragment = getFragmentByTag(showListFragmentTag);
					if (fragment != null) {
						((ShowListFragment) fragment).reset();
					}
				}
				*/
				return false;
			}
		});

		initLocationManager();
		location = bestLastKnownLocation(MIN_LAST_READ_ACCURACY, TEN_MIN);
		
		if (savedInstanceState == null) {
			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction transaction = fm.beginTransaction();
			ViewPagerFragment fragment = new ViewPagerFragment();
			transaction.replace(R.id.content_fragment, fragment);
			transaction.commit();
		} else {
			String value = savedInstanceState.getString(CACHED_QUERY_STATE);
			if (value != null && !value.isEmpty()) {
				cachedQuery = value;
			}
			if (cachedQuery != null && !cachedQuery.isEmpty() && !mainContent.isShown()) {
				teaserContent.setVisibility(View.GONE);
				mainContent.setVisibility(View.VISIBLE);
			}
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
			final Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.help_content);
			
			TextView helpClose = (TextView) dialog.findViewById(R.id.help_close);
			helpClose.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
			
			WebView webView = (WebView) dialog.findViewById(R.id.html_help);
			webView.loadData(readTextFromResource(R.raw.help), "text/html", null);
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
			int result = tts.setLanguage(Locale.UK);
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				result = tts.setLanguage(Locale.US);
				if (result == TextToSpeech.LANG_MISSING_DATA
						|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
					Log.e(TAG, "Language is not available.");
				}
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
	public void receivedResponse(QueryResponse queryResponse, boolean responseMessage) {
		try {
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
				if (responseMessage) {
					say(message);
					showToast(message, Toast.LENGTH_SHORT);
				}
			}
			String showListFragmentTag = getFragmentTag(R.id.viewpager,
					SearchActivityTabAdapter.LIST_FRAGMENT_INDEX);
			if (showListFragmentTag != null) {
				Fragment fragment = getFragmentByTag(showListFragmentTag);
				if (fragment != null) {
					((ShowListFragment) fragment).refreshList(queryResponse,
							reload);
				}
			}
			String supportMapFragmentTag = getFragmentTag(R.id.viewpager,
					SearchActivityTabAdapter.MAP_FRAGMENT_INDEX);
			if (supportMapFragmentTag != null) {
				Fragment fragment = getFragmentByTag(supportMapFragmentTag);
				if (fragment != null) {
					((ShowMapFragment) fragment).updateMap(
							queryResponse.getResultList(), reload);
				}
			}
		} finally {
			lockOrientation(false);
		}
	}
	
	@Override
	public void requestDenied(String reason) {
		showToast(reason, Toast.LENGTH_SHORT);
		say(reason);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedState) {
	    super.onSaveInstanceState(savedState);
	    if (cachedQuery != null) {
	    	savedState.putString(CACHED_QUERY_STATE, cachedQuery);
	    }
	}
	
	@Override
	protected void onResume() {
		Log.i(TAG, "onResume()");
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
		
		if (cachedQuery != null && !cachedQuery.isEmpty() && !mainContent.isShown()) {
			teaserContent.setVisibility(View.GONE);
			mainContent.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause()");
		super.onPause();
		searchView.clearFocus();
		if (locationManager != null) {
			locationManager.removeUpdates(this);
		}
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy()");
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
	
	@Override
	public void startProgress() {
		lockOrientation(true);
		mainProgressBar.setVisibility(View.VISIBLE);
	}
	
	@Override
	public void stopProgress() {
		mainProgressBar.setVisibility(View.INVISIBLE);
		teaserContent.setVisibility(View.GONE);
		if (!mainContent.isShown()) {
			mainContent.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public void cancel() {
		lockOrientation(false);
	}
	
	private String readTextFromResource(int resourceID) {
		InputStream raw = getResources().openRawResource(resourceID);
		StringBuilder contentBuilder = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(raw));
		try {
			String line;
			while ((line = br.readLine()) != null) {
				contentBuilder.append(line);
			}
			br.close();
		} catch (IOException e) {
			Log.i(TAG, "readTextFromResource - " + e.getMessage());
		}  finally {
			try {
				br.close();
			} catch (Exception e) {}
		}
		return contentBuilder.toString();
	}
	
	private int getCurrentOrientation() {
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
	        setRequestedOrientation(getCurrentOrientation());
	    } else {
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
	    }
	}
	
	private void startWork(final String query, final String locationString, final int page, final int count) {
		final Refreshable refreshable = this;
		new Handler().post(new Runnable() {
			public void run() {
				FragmentManager fragmentManager = getSupportFragmentManager();
				RetainableFragment retainableFragment = (RetainableFragment) fragmentManager.findFragmentByTag(RetainableFragment.TAG);
				if (retainableFragment == null) {
					retainableFragment = new RetainableFragment();
					fragmentManager.beginTransaction().add(retainableFragment, RetainableFragment.TAG).commit();
				}
				retainableFragment.startWork(refreshable, query, locationString, page, count);
			}
		});
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
}
