package com.vocifery.daytripper.ui;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
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

import com.vocifery.daytripper.Daytripper;
import com.vocifery.daytripper.R;
import com.vocifery.daytripper.service.ResponderService;
import com.vocifery.daytripper.service.UberRequestConstants;
import com.vocifery.daytripper.service.UberRequestListener;
import com.vocifery.daytripper.ui.components.Refreshable;
import com.vocifery.daytripper.ui.components.ShowListFragment;
import com.vocifery.daytripper.ui.components.ViewPagerFragment;
import com.vocifery.daytripper.ui.components.map.ShowMapFragment;
import com.vocifery.daytripper.util.QueryResponseConverter;
import com.vocifery.daytripper.util.ResourceUtils;
import com.vocifery.daytripper.vocifery.model.Locatable;
import com.vocifery.daytripper.vocifery.model.QueryResponse;

public class MainActivity extends AppCompatActivity implements LocationListener,
		Refreshable, TextToSpeech.OnInitListener, UberRequestListener, UberRequestConstants, SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String ACTION_NOTIFY = "com.vocifery.daytripper.NOTIFY";
	public static final String ACTION_GET_CONVERSATION = "com.vocifery.daytripper.CONVERSATION"; 
	public static final String VOCIFEROUS_KEY = "com.vocifery.daytripper.VOCIFEROUS";
	
	private static final String TAG = "MainActivity";
	private static final String CACHED_QUERY_STATE = "CachedQuery";
	private static final String UBER_MESSAGE_HACK = "Your ride is %d minutes away.";
	
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
	
	private String lastQuery;
	private TextToSpeech tts;
	private ProgressBar mainProgressBar;
	private SearchView searchView;
	// private RetainableFragment retainableFragment;
	private LinearLayout mainContent;
	private LinearLayout teaserContent;
	private Dialog helpDialog;
	private BroadcastReceiver broadcastReceiver;
	private boolean vociferous = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		createHelpDialog();
		
		final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.actionbar_custom);
        mainProgressBar = (ProgressBar) actionBar.getCustomView().findViewById(R.id.main_progress);
        
        final Context context = this;
        TextView helpText = (TextView) actionBar.getCustomView().findViewById(R.id.help_text);
        helpText.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View view) {
        		WebView webView = (WebView) helpDialog.findViewById(R.id.html_help);
    			webView.loadData(ResourceUtils.readTextFromResource(context, R.raw.help), "text/html", null);
    			helpDialog.show();
        	}
        });
        
		tts = new TextToSpeech(this, this);
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

		initLocationManager();
		//requestLocationUpdates(this);
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
				lastQuery = value;
			}
			if (lastQuery != null && !lastQuery.isEmpty() && !mainContent.isShown()) {
				teaserContent.setVisibility(View.GONE);
				mainContent.setVisibility(View.VISIBLE);
			}
		}
		
		broadcastReceiver = new BroadcastReceiver() {
			@Override
            public void onReceive(Context context, Intent intent) {
				try {
					processMessage(intent);
				} catch (JSONException e) {
					Log.e(TAG, "processMessage() error - " + e.getMessage());
				}
            }
        };
        startListening();
        
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
		prefs.registerOnSharedPreferenceChangeListener(this);
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
			tts.setSpeechRate(0.9f);
		} else {
			Log.e(TAG, "Could not initialize TextToSpeech.");
		}
	}

	@Override
	public void refresh(int page, int count) {
		updateLocation();
		String locationString = null;
		if (location != null) {
			locationString = location.getLatitude() + ", "
					+ location.getLongitude();
		}
		Log.i(TAG, "refresh - sending query " + lastQuery + " with location "
				+ locationString);
		startWork(lastQuery, locationString, page, count);
	}
	
	@Override
	public void receivedResponse(QueryResponse queryResponse, boolean responseMessage) {
		try {
			if (queryResponse == null || queryResponse.getTotal() == 0) {
				showErrorMessage(queryResponse);
				return;
			}
			
			/*
			String source = queryResponse.getSource();
			if (source != null) {
				String message = queryResponse.getMessage();
				if (!TextUtils.isEmpty(message)) {
					message = String.format(Locale.getDefault(), message, queryResponse.getTotal(), queryResponse.getSource());
					say(message);
					showToast(message, Toast.LENGTH_SHORT);
				}
			}
			*/
			
			Integer total = queryResponse.getTotal();
			if (total == null) {
				showErrorMessage(queryResponse);
				return;
			}
			
			boolean reload = false;
			Integer page = queryResponse.getPage();
			if (page != null && page <= 1) {
				reload = true;
				String message = queryResponse.getMessage();
				if (message == null) {
					message = getRandomSuccessMessage(queryResponse.getTotal(), queryResponse.getSource());
				} else {
					SharedPreferences prefs = getApplicationContext().getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
					String username = prefs.getString(Daytripper.USERNAME_KEY, null);
					if (TextUtils.isEmpty(username)) {
						username = getString(R.string.default_name);
					}
					
					message = String.format(Locale.getDefault(), message, queryResponse.getTotal(), queryResponse.getSource(), username);
				}
				
				if (responseMessage) {
					say(message);
					showToast(message, Toast.LENGTH_SHORT);
				}
			}
			
			if (queryResponse.getRoute() == null || queryResponse.getRoute().isEmpty()) {
				queryResponse.getResultList();
			}
			
			updateList(queryResponse, reload);
			updateMap(queryResponse, reload);
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
	    if (lastQuery != null) {
	    	savedState.putString(CACHED_QUERY_STATE, lastQuery);
	    }
	}
	
	@Override
	protected void onResume() {
		Log.i(TAG, "onResume()");
		super.onResume();
		if (lastQuery != null && !lastQuery.isEmpty() && !mainContent.isShown()) {
			teaserContent.setVisibility(View.GONE);
			mainContent.setVisibility(View.VISIBLE);
		}
		requestLocationUpdates(this);
		startListening();
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause()");
		super.onPause();
		searchView.clearFocus();
		if (locationManager != null) {
			locationManager.removeUpdates(this);
		}
		
		if (helpDialog.isShowing()) {
			helpDialog.dismiss();
		}
		stopListening();
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
	
	@Override
	public void sendingRequest() {
		startProgress();
	}
	
	@Override
	public void stopRequest() {
		stopProgress();
	}
	
	@Override
	public void onNoResponse() {
		stopProgress();
		say(getMessage(R.string.uber_no_response));
	}

	@Override
	public void onRequestSent() {
		stopProgress();
		say(getMessage(R.string.uber_request_sent));
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (!TextUtils.isEmpty(key) && key.equals(VOCIFEROUS_KEY)) {
			vociferous = sharedPreferences.getBoolean(VOCIFEROUS_KEY, true);
		}
	}
	
	private void createHelpDialog() {
		helpDialog = new Dialog(this);
		helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		helpDialog.setContentView(R.layout.help_content);
		
		TextView helpClose = (TextView) helpDialog.findViewById(R.id.help_close);
		helpClose.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				helpDialog.dismiss();
			}
		});
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
		startProgress();
		Intent serviceIntent = new Intent(this, ResponderService.class);
		serviceIntent.putExtra(ResponderService.KEY_QUERY, query);
		serviceIntent.putExtra(ResponderService.KEY_lOCATION, locationString);
		serviceIntent.putExtra(ResponderService.KEY_PAGE, page);
		serviceIntent.putExtra(ResponderService.KEY_COUNT, count);
		
		/*
		Daytripper daytripper = (Daytripper) getApplicationContext();
		GeoPoint selectedPoint = daytripper.getSelectedPoint();
		if (selectedPoint != null) {
			String destination = String.format(Locale.getDefault(),
					"%10.6f, %10.6f", selectedPoint.getLatitude(), selectedPoint.getLongitude());
			serviceIntent.putExtra(ResponderService.KEY_DESTINATION, destination);
		} else {
			String destination = QueryParser.extractDestinationFromQuery(query, resultList);
			if (!TextUtils.isEmpty(destination)) {
				serviceIntent.putExtra(ResponderService.KEY_DESTINATION, destination);
			}
		}
		daytripper.setSelectedPoint(null);
		*/
		startService(serviceIntent);
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
				updateLocation();
				String query = intent.getStringExtra(SearchManager.QUERY);
				if (query == null || query.isEmpty()) {
					Log.i(TAG, "query is null");
					return;
				}
				query = query.trim();
				
				if (searchView.getQuery() == null || searchView.getQuery().length() == 0) {
					searchView.setQuery(query, false);
				}
				lastQuery = query;

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
		if (!vociferous) {
			return;
		}
		
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
	
	private void showSearchResult(String result, String customMessage) throws JSONException {
		QueryResponse queryResponse = 
				QueryResponseConverter.parseJson(result);
		if (!TextUtils.isEmpty(customMessage)) {
			queryResponse.setMessage(customMessage);
		}
		receivedResponse(queryResponse, true);
	}
	
	private void showUberStatus(String result) throws JSONException {
		JSONObject json = new JSONObject(result);
		String requestId = "";
		if (json.has(FIELD_REQUEST_ID) && !json.isNull(FIELD_REQUEST_ID)) {
			requestId = json.getString(FIELD_REQUEST_ID);
		}
		
		String status = "";
		if (json.has(FIELD_STATUS) && !json.isNull(FIELD_STATUS)) {
			status = json.getString(FIELD_STATUS);
		}
		
		String driverName = "";
		if (json.has(FIELD_DRIVER_NAME) && !json.isNull(FIELD_DRIVER_NAME)) {
			driverName = json.getString(FIELD_DRIVER_NAME);
		}
		
		String driverPhoneNumber = "";
		if (json.has(FIELD_DRIVER_PHONE_NUMBER) && !json.isNull(FIELD_DRIVER_PHONE_NUMBER)) {
			driverPhoneNumber = json.getString(FIELD_DRIVER_PHONE_NUMBER);
		}
		
		String driverPictureUrl = "";
		if (json.has(FIELD_DRIVER_PICTURE_URL) && !json.isNull(FIELD_DRIVER_PICTURE_URL)) {
			driverPictureUrl = json.getString(FIELD_DRIVER_PICTURE_URL);
		}
		
		Integer eta = -1;
		if (json.has(FIELD_ETA) && !json.isNull(FIELD_ETA)) {
			eta = json.getInt(FIELD_ETA);
		}
		
		String vehicleMake = "";
		if (json.has(FIELD_VEHICLE_MAKE) && !json.isNull(FIELD_VEHICLE_MAKE)) {
			vehicleMake = json.getString(FIELD_VEHICLE_MAKE);
		}
		
		String vehicleModel = "";
		if (json.has(FIELD_VEHICLE_MODEL) && !json.isNull(FIELD_VEHICLE_MODEL)) {
			vehicleModel = json.getString(FIELD_VEHICLE_MODEL);
		}
		
		String vehicleLicensePlate = "";
		if (json.has(FIELD_VEHICLE_LICENSE_PLATE) && !json.isNull(FIELD_VEHICLE_LICENSE_PLATE)) {
			vehicleLicensePlate = json.getString(FIELD_VEHICLE_LICENSE_PLATE);
		}
		
		String etaMessage = String.format(Locale.getDefault(), UBER_MESSAGE_HACK, eta);
		say(etaMessage);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
		LayoutInflater inflater = getLayoutInflater();
		View titleView = inflater.inflate(R.layout.uber_view_title, null);
		builder.setCustomTitle(titleView);				    
		
		View contentView = inflater.inflate(R.layout.uber_view_content, null);
		TextView driverNameView = (TextView) contentView.findViewById(R.id.driverName);
		driverNameView.setText(driverName);
		
		TextView driverPhoneView = (TextView) contentView.findViewById(R.id.driverPhone);
		driverPhoneView.setText(driverPhoneNumber);
		
		TextView vehicleModelView = (TextView) contentView.findViewById(R.id.vehicleModel);
		vehicleModelView.setText(String.format(Locale.getDefault(), "%s %s", vehicleMake, vehicleModel));
		
		TextView licensePlateView = (TextView) contentView.findViewById(R.id.licensePlate);
		licensePlateView.setText(vehicleLicensePlate);
		
		TextView vehicleStatusView = (TextView) contentView.findViewById(R.id.vehicleStatus);
		vehicleStatusView.setText(etaMessage);
		
		builder.setView(contentView);
		builder.setPositiveButton("OK", null);					
		builder.show();
	}
	
	private void showUberCancel() throws JSONException {
		String cancelMessage = getMessage(R.string.uber_request_cancel);
		say(cancelMessage);
		showToast(cancelMessage, Toast.LENGTH_SHORT);
	}
	
	private void processMessage(Intent intent) throws JSONException {
		try {
			if (intent == null) {
				String errorMessage = getMessage(R.string.error_message);
				say(errorMessage);
				showToast(errorMessage, Toast.LENGTH_SHORT);
				return;
			}
			
			if (intent.hasExtra(ResponderService.EXTRA_MESSAGE)) {
				String customMessage = intent.getStringExtra(ResponderService.CUSTOM_MESSAGE);
				showSearchResult(intent.getStringExtra(ResponderService.EXTRA_MESSAGE), customMessage);
			} else if (intent.hasExtra(ResponderService.UBER_STATUS_MESSAGE)) {
				showUberStatus(intent.getStringExtra(ResponderService.UBER_STATUS_MESSAGE));
			} else if (intent.hasExtra(ResponderService.UBER_CANCEL_MESSAGE)) {
				showUberCancel();
			} else if (intent.hasExtra(ResponderService.MAP_ZOOM_MESSAGE)) {
				showZoom(intent.getStringExtra(ResponderService.MAP_ZOOM_MESSAGE));
			} else if (intent.hasExtra(VOCIFEROUS_KEY)) {
				updateVociferousFlag(intent.getBooleanExtra(VOCIFEROUS_KEY, true));
			} else if (intent.hasExtra(ResponderService.NAME_MESSAGE)) {
				greet(intent.getStringExtra(ResponderService.NAME_MESSAGE));
			} else {
				String noOpMessage = intent.getStringExtra(ResponderService.NO_OP_MESSAGE);
				if (!TextUtils.isEmpty(noOpMessage)) {
					say(noOpMessage);
					showToast(noOpMessage, Toast.LENGTH_SHORT);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			String errorMessage = getMessage(R.string.system_error_message);
			say(errorMessage);
			showToast(errorMessage, Toast.LENGTH_SHORT);
		} finally {
			stopProgress();
		}
    }
	
	private void startListening() {
        IntentFilter filter = new IntentFilter(ResponderService.ACTION_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }
	
	private void stopListening() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
	}
	
	private void showErrorMessage(QueryResponse queryResponse) {
		String message = queryResponse.getMessage();
		if (TextUtils.isEmpty(message)) {
			message = getMessage(R.string.error_message);
		}
		say(message);
		showToast(message, Toast.LENGTH_SHORT);
	}
	
	private void updateList(QueryResponse queryResponse, boolean reload) {
		String showListFragmentTag = getFragmentTag(R.id.viewpager,
				SearchActivityTabAdapter.LIST_FRAGMENT_INDEX);
		if (showListFragmentTag != null) {
			Fragment fragment = getFragmentByTag(showListFragmentTag);
			if (fragment != null) {
				((ShowListFragment) fragment).refreshList(queryResponse,
						reload);
			}
		}
	}
	
	private void updateMap(QueryResponse queryResponse, boolean reload) {
		String supportMapFragmentTag = getFragmentTag(R.id.viewpager,
				SearchActivityTabAdapter.MAP_FRAGMENT_INDEX);
		if (supportMapFragmentTag != null) {
			Fragment fragment = getFragmentByTag(supportMapFragmentTag);
			if (fragment != null) {
				ShowMapFragment showMapFragment = (ShowMapFragment) fragment;
				List<Locatable> route = queryResponse.getRoute();
				if (route != null) {
					showMapFragment.updateMapWithRoute(route, reload);
				} else {
					showMapFragment.updateMap(queryResponse.getResultList(), reload);
				}
			}
		}
	}
	
	private void requestLocationUpdates(final LocationListener listener) {
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
	}
	
	private void updateLocation() {
		Location updatedLocation = bestLastKnownLocation(MIN_LAST_READ_ACCURACY, TEN_MIN);
		if (updatedLocation != null) {
			location = updatedLocation;
		}
	}
	
	private void showZoom(String level) {
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
		String username = prefs.getString(Daytripper.USERNAME_KEY, null);
		if (TextUtils.isEmpty(username)) {
			username = getString(R.string.default_name);
		}
		
		String zoomMessage = getString(R.string.system_zoom_message, level, username);
		say(zoomMessage);
		showToast(zoomMessage, Toast.LENGTH_SHORT);
		
		String supportMapFragmentTag = getFragmentTag(R.id.viewpager,
				SearchActivityTabAdapter.MAP_FRAGMENT_INDEX);
		if (supportMapFragmentTag != null) {
			Fragment fragment = getFragmentByTag(supportMapFragmentTag);
			if (fragment != null) {
				ShowMapFragment mapFragment = (ShowMapFragment) fragment;
				try {
					mapFragment.zoom(Integer.parseInt(level));
				} catch (Exception e) {
					Log.w(TAG, "Bad zoom level " + level);
				}
			}
		}
	}
	
	private String getRandomSuccessMessage(Integer total, String source) {
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
		String username = prefs.getString(Daytripper.USERNAME_KEY, null);
		if (TextUtils.isEmpty(username)) {
			username = getString(R.string.default_name);
		}
		
		int rand = new Random().nextInt(3);
		switch (rand) {
			case 0:
				return getString(R.string.success_message_one, total, source, username);
				
			case 1:
				return getString(R.string.success_message_two, total, source, username);
			
			default:
				return getString(R.string.success_message_three, total, source, username);
				
		}
	}
	
	private void updateVociferousFlag(boolean vociferousFlag) {
		if (vociferousFlag) {
			vociferous = true;
			String message = getString(R.string.speak_up_message);
			say(message);
			showToast(message, Toast.LENGTH_SHORT);
		} else {
			String message = getString(R.string.shut_up_message);
			say(message);
			showToast(message, Toast.LENGTH_SHORT);
		}
		
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(VOCIFEROUS_KEY, vociferousFlag);
		editor.commit();
	}
	
	private void greet(String name) {
		String greeting = getString(R.string.greeting_message, name);
		say(greeting);
		showToast(greeting, Toast.LENGTH_SHORT);
	}
	
	private static String getFragmentTag(int viewId, int index) {
		return "android:switcher:" + viewId + ":" + index;
	}
}
