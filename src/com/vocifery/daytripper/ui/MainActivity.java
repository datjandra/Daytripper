package com.vocifery.daytripper.ui;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import android.annotation.SuppressLint;
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
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
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

import com.vocifery.daytripper.R;
import com.vocifery.daytripper.service.RequestConstants;
import com.vocifery.daytripper.service.ResponderService;
import com.vocifery.daytripper.ui.components.Refreshable;
import com.vocifery.daytripper.ui.components.ShowListFragment;
import com.vocifery.daytripper.ui.components.ShowMapFragment;
import com.vocifery.daytripper.util.QueryResponseConverter;
import com.vocifery.daytripper.util.ResourceUtils;
import com.vocifery.daytripper.util.StringUtils;
import com.vocifery.daytripper.vocifery.model.Locatable;
import com.vocifery.daytripper.vocifery.model.QueryResponse;

@SuppressLint("InflateParams")
public class MainActivity extends AppCompatActivity implements LocationListener,
		Refreshable, TextToSpeech.OnInitListener, RequestConstants, SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String ACTION_NOTIFY = "com.vocifery.daytripper.NOTIFY";
	public static final String ACTION_GET_CONVERSATION = "com.vocifery.daytripper.CONVERSATION"; 
	public static final String VOCIFEROUS_KEY = "com.vocifery.daytripper.VOCIFEROUS";
	
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
	
	private TextToSpeech tts;
	private ProgressBar mainProgressBar;
	private SearchView searchView;
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
		location = bestLastKnownLocation(MIN_LAST_READ_ACCURACY, TEN_MIN);
		
		ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new SearchActivityTabAdapter(getSupportFragmentManager()));
        viewPager.requestTransparentRegion(viewPager);
		
		if (savedInstanceState != null) {
			String lastQuery = getLastQuery();
			if (!TextUtils.isEmpty(lastQuery) && !mainContent.isShown()) {
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
			tts.setSpeechRate(1.0f);
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
		
		String lastQuery = getLastQuery();
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
					message = getRandomSuccessMessage(queryResponse.getTotal(), queryResponse.getSource(), queryResponse.getSortedCategories());
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
	}
	
	@Override
	protected void onResume() {
		Log.i(TAG, "onResume()");
		super.onResume();
		String lastQuery = getLastQuery();
		if (!TextUtils.isEmpty(lastQuery) && !mainContent.isShown()) {
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
				@SuppressWarnings("deprecation")
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
	
	private void processMessage(Intent intent) throws JSONException {
		try {
			if (intent == null) {
				String errorMessage = getMessage(R.string.error_message);
				say(errorMessage);
				showToast(errorMessage, Toast.LENGTH_SHORT);
				return;
			}
			
			if (intent.hasExtra(ResponderService.EXTRA_MESSAGE)) {
				String customMessage = intent.getStringExtra(ResponderService.EXTRA_CUSTOM_MESSAGE);
				showSearchResult(intent.getStringExtra(ResponderService.EXTRA_MESSAGE), customMessage);
			} else if (intent.hasExtra(ResponderService.EXTRA_MAP_ZOOM_MESSAGE)) {
				showZoom(intent.getStringExtra(ResponderService.EXTRA_MAP_ZOOM_MESSAGE));
			} else if (intent.hasExtra(VOCIFEROUS_KEY)) {
				updateVociferousFlag(intent.getBooleanExtra(VOCIFEROUS_KEY, true));
			} else if (intent.hasExtra(ResponderService.EXTRA_NAME_MESSAGE)) {
				greet(intent.getStringExtra(ResponderService.EXTRA_NAME_MESSAGE));
			} else {
				String noOpMessage = intent.getStringExtra(ResponderService.EXTRA_NO_OP_MESSAGE);
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
		ShowListFragment showListFragment = getListFragment();
		showListFragment.refreshList(queryResponse, reload);
	}
	
	private void updateMap(QueryResponse queryResponse, boolean reload) {
		String supportMapFragmentTag = getFragmentTag(R.id.viewpager,
				SearchActivityTabAdapter.MAP_FRAGMENT_INDEX);
		Fragment fragment = getFragmentByTag(supportMapFragmentTag);
		ShowMapFragment showMapFragment = (ShowMapFragment) fragment;
		List<Locatable> route = queryResponse.getRoute();
		if (route != null) {
			showMapFragment.updateMapWithRoute(route, reload);
		} else {
			showMapFragment.updateMap(queryResponse.getResultList(), reload);
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
	
	private String getRandomSuccessMessage(Integer total, String source, List<Map.Entry<String, Integer>> sortedCategories) {
		String topCategory = null;
		if (sortedCategories != null && !sortedCategories.isEmpty()) {
			topCategory = sortedCategories.get(0).getKey().toLowerCase(Locale.getDefault());
		}
		
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
		String username = prefs.getString(Daytripper.USERNAME_KEY, null);
		if (TextUtils.isEmpty(username)) {
			username = getString(R.string.default_name);
		}
		
		if (!TextUtils.isEmpty(topCategory)) {
			int rand = new Random().nextInt(3);
			int resourceId = getResourceId(String.format(Locale.getDefault(), "category_message_%s_%d", 
					source.toLowerCase(Locale.getDefault()), rand + 1));
			return getString(resourceId, total, source, username, StringUtils.cleanup(topCategory));
		} else {
			int rand = new Random().nextInt(3);
			int resourceId = getResourceId(String.format(Locale.getDefault(), "success_message_%d", rand + 1));
			return getString(resourceId, total, source, username);
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
	
	private int getResourceId(String key) {
		String packageName = getPackageName();
		return getResources().getIdentifier(key, "string", packageName);
	}
	
	private ShowListFragment getListFragment() {
		String showListFragmentTag = getFragmentTag(R.id.viewpager,
				SearchActivityTabAdapter.LIST_FRAGMENT_INDEX);
		if (showListFragmentTag != null) {
			Fragment fragment = getFragmentByTag(showListFragmentTag);
			if (fragment != null) {
				return (ShowListFragment) fragment;
			} 
		}
		return null;
	}
	
	private static String getFragmentTag(int viewId, int index) {
		return "android:switcher:" + viewId + ":" + index;
	}
	
	private static String getLastQuery() {
		final Daytripper daytripper = (Daytripper) Daytripper.getAppContext();
		return daytripper.getLastQuery();
	}
}
