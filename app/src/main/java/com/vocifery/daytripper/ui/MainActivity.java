package com.vocifery.daytripper.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
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
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.neura.sdk.config.NeuraConsts;
import com.neura.sdk.object.AuthenticationRequest;
import com.neura.sdk.object.Permission;
import com.neura.sdk.object.SubscriptionRequest;
import com.neura.sdk.service.NeuraApiClient;
import com.neura.sdk.service.NeuraServices;
import com.neura.sdk.service.SubscriptionRequestCallbacks;
import com.neura.sdk.util.Builder;
import com.neura.sdk.util.NeuraAuthUtil;
import com.neura.sdk.util.NeuraUtil;
import com.vocifery.daytripper.R;
import com.vocifery.daytripper.service.RequestConstants;
import com.vocifery.daytripper.service.ResponderService;
import com.vocifery.daytripper.ui.components.IntroFragment;
import com.vocifery.daytripper.ui.components.Refreshable;
import com.vocifery.daytripper.ui.components.ResultFragment;
import com.vocifery.daytripper.util.ResourceUtils;

import org.alicebot.ab.Chat;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressLint("InflateParams")
public class MainActivity extends AppCompatActivity implements
		LocationListener,
		Refreshable,
		TextToSpeech.OnInitListener,
		RequestConstants,
		SharedPreferences.OnSharedPreferenceChangeListener,
		Handler.Callback {

	public static final String ACTION_NOTIFY = "com.vocifery.daytripper.NOTIFY";
	public static final String ACTION_GET_CONVERSATION = "com.vocifery.daytripper.CONVERSATION";

	private static final String TAG = "MainActivity";
	private static final String APP_REFERRER = "Daytripper";
	private static final long MEASURE_TIME = 1000 * 60;
	private static final long POLLING_FREQ = 1000 * 20;
	private static final long ONE_MIN = 1000 * 60;
	private static final long TWO_MIN = ONE_MIN * 2;
	private static final long FIVE_MIN = ONE_MIN * 5;
	private static final long TEN_MIN = FIVE_MIN * 2;
	private static final float MIN_LAST_READ_ACCURACY = 1000.0f;
	private static final float MIN_ACCURACY = 50.0f;
	private static final float MIN_DISTANCE = 20.0f;
	private static final int NEURA_AUTHENTICATION_REQUEST_CODE = 0;
	
	private LocationManager locationManager;
	private Location location;
	
	private TextToSpeech tts;
	private ProgressBar mainProgressBar;
	private SearchView searchView;
	private Dialog helpDialog;
	private BroadcastReceiver broadcastReceiver;
	private IntroFragment introFragment;
	private ResultFragment resultFragment;
	private NeuraApiClient neuraClient;
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
		searchView = (SearchView) findViewById(R.id.search_view);
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));

		if (findViewById(R.id.fragment_container) != null) {
			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();

			IntroFragment introFrag = getIntroFragment();
			ft.add(R.id.fragment_container, introFrag);

			ResultFragment resultFrag = getResultFragment();
			ft.add(R.id.fragment_container, resultFrag);

			ft.show(introFrag);
			ft.hide(resultFrag);
			ft.commit();
		}

		initLocationManager();
		location = bestLastKnownLocation(MIN_LAST_READ_ACCURACY, TEN_MIN);

		if (savedInstanceState != null) {
			String lastQuery = getLastQuery();
			/*
			if (!TextUtils.isEmpty(lastQuery) && !mainContent.isShown()) {
				teaserContent.setVisibility(View.GONE);
				mainContent.setVisibility(View.VISIBLE);
			}
			*/
		}
		
		broadcastReceiver = new BroadcastReceiver() {
			@Override
            public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (!TextUtils.isEmpty(action) && action.equalsIgnoreCase(ResponderService.RESPONSE_ACTION)) {
					processMessage(intent);
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
		startWork(lastQuery, locationString);
	}
	
	@Override
	public void receivedResponse(String response, boolean vocalize) {
		try {
			if (vocalize) {
				String plainText = Jsoup.parse(response).text();
				say(plainText);
			}
		} finally {
			lockOrientation(false);
		}
	}
	
	@Override
	public void requestDenied(String reason) {
		say(reason);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedState) {
	    super.onSaveInstanceState(savedState);
	}

	@Override
	protected void onStart() {
		Log.i(TAG, "onStart()");
		super.onStart();
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume()");
		super.onResume();
		String lastQuery = getLastQuery();
		/*
		if (!TextUtils.isEmpty(lastQuery) && !mainContent.isShown()) {
			teaserContent.setVisibility(View.GONE);
			mainContent.setVisibility(View.VISIBLE);
		}
		*/
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
		disconnectNeura();
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
		/*
		teaserContent.setVisibility(View.GONE);
		if (!mainContent.isShown()) {
			mainContent.setVisibility(View.VISIBLE);
		}
		*/
	}
	
	@Override
	public void cancel() {
		lockOrientation(false);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (!TextUtils.isEmpty(key) && key.equals(ResponderService.VOICE_FLAG)) {
			vociferous = sharedPreferences.getBoolean(ResponderService.VOICE_FLAG, true);
		}
	}

	@Override
	public boolean handleMessage(Message message) {
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 0) {
			if (resultCode == Activity.RESULT_OK) {
				Daytripper daytripper = (Daytripper) Daytripper.getAppContext();
				String accessToken = NeuraAuthUtil.extractToken(data);
				registerNeuraEvent(accessToken, daytripper.getNeuraEventName());
				Log.i(TAG, String.format("Successfully logged in with accessToken %s", accessToken));
			} else {
				int errorCode = data.getIntExtra(NeuraConsts.EXTRA_ERROR_CODE, -1);
				Log.e(TAG, String.format("Authentication failed due to %s", NeuraUtil.errorCodeToString(errorCode)));
			}
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
	    return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
	}

	private void lockOrientation(boolean lock) {
	    if (lock) {
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
	    } else {
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	    }
	}
	
	private void startWork(final String query, final String locationString) {
		startProgress();
		Intent serviceIntent = new Intent(this, ResponderService.class);
		serviceIntent.setAction(ResponderService.USER_ACTION);
		serviceIntent.putExtra(ResponderService.KEY_QUERY, query);
		serviceIntent.putExtra(ResponderService.KEY_lOCATION, locationString);
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
				startWork(query, locationString);
			}
		} finally {
			searchView.clearFocus();
		}
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
	
	private void processMessage(Intent intent) {
		try {
			if (intent == null) {
				String errorMessage = getMessage(R.string.error_message);
				say(errorMessage);
				showContent(errorMessage);
				return;
			}
			
			if (intent.hasExtra(ResponderService.VOICE_FLAG)) {
				toggleVoice(intent.getStringExtra(ResponderService.VOICE_FLAG),
						intent.getStringExtra(ResponderService.EXTRA_NO_OP_MESSAGE));
			} else if (intent.hasExtra(ResponderService.NEURA_USER_LEFT_WORK)) {
				String userLeftWork = intent.getStringExtra(ResponderService.NEURA_USER_LEFT_WORK);
				handleNeuraEvent(ResponderService.NEURA_USER_LEFT_WORK, userLeftWork);
			} else if (intent.hasExtra(ResponderService.NEURA_USER_ARRIVED_HOME)) {
				String userArrivedHome = intent.getStringExtra(ResponderService.NEURA_USER_ARRIVED_HOME);
				handleNeuraEvent(ResponderService.NEURA_USER_ARRIVED_HOME, userArrivedHome);
			} else if (intent.hasExtra(ResponderService.NEURA_USER_LEFT_HOME)) {
				String userLeftHome = intent.getStringExtra(ResponderService.NEURA_USER_LEFT_HOME);
				handleNeuraEvent(ResponderService.NEURA_USER_LEFT_HOME, userLeftHome);
			} else if (intent.hasExtra(ResponderService.NEURA_USER_ARRIVED_TO_WORK)) {
				String userArrivedToWork = intent.getStringExtra(ResponderService.NEURA_USER_ARRIVED_TO_WORK);
				handleNeuraEvent(ResponderService.NEURA_USER_ARRIVED_TO_WORK, userArrivedToWork);
			}

			String noOpMessage = intent.getStringExtra(ResponderService.EXTRA_NO_OP_MESSAGE);
			if (!TextUtils.isEmpty(noOpMessage)) {
				receivedResponse(noOpMessage, true);
			}

			String url = intent.getStringExtra(ResponderService.EXTRA_URL_MESSAGE);
			if (!TextUtils.isEmpty(url)) {
				Log.i(TAG, String.format("showUrl(%s)", url));
				showUrl(url);
			} else if (!TextUtils.isEmpty(noOpMessage)) {
				Log.i(TAG, String.format("showContent(%s)", noOpMessage));
				showContent(noOpMessage);
			}


		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			String errorMessage = getMessage(R.string.system_error_message);
			say(errorMessage);
			showContent(errorMessage);
		} finally {
			stopProgress();
		}
    }
	
	private void startListening() {
        IntentFilter filter = new IntentFilter(ResponderService.RESPONSE_ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }
	
	private void stopListening() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
	}

	private void showUrl(String url) {
		if (findViewById(R.id.fragment_container) != null) {
			IntroFragment introFrag = getIntroFragment();
			ResultFragment resultFrag = getResultFragment();
			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			ft.hide(introFrag);
			ft.show(resultFrag);
			ft.commit();
			resultFrag.updateWebviewUrl(url);
		}
	}

	private void showContent(String content) {
		if (findViewById(R.id.fragment_container) != null) {
			IntroFragment introFrag = getIntroFragment();
			ResultFragment resultFrag = getResultFragment();
			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			ft.hide(introFrag);
			ft.show(resultFrag);
			ft.commit();
			resultFrag.updateWebviewContent(content);
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

			Chat.locationKnown = true;
			Chat.longitude = Double.toString(location.getLongitude());
			Chat.latitude = Double.toString(location.getLatitude());
		}
	}

	private void toggleVoice(String voiceFlag, String response) {
		if (TextUtils.isEmpty(voiceFlag)) {
			Log.w(TAG, "Null voice flag");
			return;
		}

		say(response);
		vociferous = (voiceFlag.equals("on") ? Boolean.TRUE : Boolean.FALSE);

		SharedPreferences prefs = getApplicationContext().getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(ResponderService.VOICE_FLAG, vociferous);
		editor.commit();
	}

	private IntroFragment getIntroFragment() {
		if (introFragment == null) {
			introFragment = new IntroFragment();
		}
		return introFragment;
	}
	
	private ResultFragment getResultFragment() {
		if (resultFragment == null) {
			resultFragment = new ResultFragment();
		}
		return resultFragment;
	}

	private static String getLastQuery() {
		final Daytripper daytripper = (Daytripper) Daytripper.getAppContext();
		return daytripper.getLastQuery();
	}

	private void connectToNeura() {
		Builder builder = new Builder(this);
		neuraClient = builder.build();
		neuraClient.setAppUid(getString(R.string.app_uid_production));
		neuraClient.setAppSecret(getString(R.string.app_secret_production));
		neuraClient.connect();
	}

	private void disconnectNeura() {
		if (neuraClient != null) {
			neuraClient.disconnect();
		}
	}

	private void handleNeuraEvent(String eventName, String eventDetails) {
		checkNeuraSupport();
		Daytripper daytripper = (Daytripper) Daytripper.getAppContext();
		daytripper.setNeuraEventName(eventName);
		daytripper.setNeuraEventDetails(eventDetails);
		Log.i(TAG, String.format("event=%s, details=%s", eventName, eventDetails));
	}

	private void checkNeuraSupport() {
		boolean neuraSupported = NeuraUtil.isNeuraAppSupported(MainActivity.this);
		if (!neuraSupported) {
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
			dialogBuilder.setMessage("Error: This device cannot support the Neura app");
			dialogBuilder.setNeutralButton("Close", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					MainActivity.this.finish();
				}
			});
			dialogBuilder.create().show();
		} else {
			connectToNeura();
			setNeuraPermissions();
		}
	}

	private void setNeuraPermissions() {
		AuthenticationRequest authRequest = new AuthenticationRequest();
		authRequest.setAppId(getString(R.string.app_uid_production));
		authRequest.setAppSecret(getString(R.string.app_secret_production));
		String[] permissions = getString(R.string.neura_permissions).split(",");
		ArrayList<Permission> permissionList = Permission.list(permissions);
		authRequest.setPermissions(permissionList);
		boolean neuraInstalled = new NeuraAuthUtil().authenticate(MainActivity.this,
				NEURA_AUTHENTICATION_REQUEST_CODE, authRequest);
		if (!neuraInstalled) {
			NeuraUtil.redirectToGooglePlayNeuraMeDownloadPage(this, APP_REFERRER);
		}
	}

	private void registerNeuraEvent(String accessToken, String eventName) {
		if (!neuraClient.isConnected()) {
			Log.e(TAG, "Attempt to register for events without Neura connection");
			return;
		}

		SubscriptionRequest subscriptionRequest = new SubscriptionRequest.Builder(this)
				.setAccessToken(accessToken)
				.setAction(NeuraConsts.ACTION_SUBSCRIBE)
				.setEventName(eventName)
				.build();
		NeuraServices.SubscriptionsAPI.executeSubscriptionRequest(neuraClient, subscriptionRequest, new SubscriptionRequestCallbacks() {
			@Override
			public void onSuccess(String eventName, Bundle result, String identifier) {
				Log.i(TAG, String.format("Successfully subscribed to %s", eventName));
			}

			@Override
			public void onFailure(String eventName, Bundle result, int errorCode) {
				Log.e(TAG, String.format("Failed to subscribe to %s with error %s", eventName, NeuraUtil.errorCodeToString(errorCode)));
			}
		});
	}

	private void unregisterNeuraEvent(String accessToken, String eventName, String subscriptionIdentifier) {
		if (!neuraClient.isConnected()) {
			Log.e(TAG, "Attempt to unregister events without Neura connection");
			return;
		}

		SubscriptionRequest subscriptionRequest = new SubscriptionRequest.Builder(this)
				.setAccessToken(accessToken)
				.setEventName(eventName)
				.setAction(NeuraConsts.ACTION_UNSUBSCRIBE)
				.setIdentifier(subscriptionIdentifier)
				.build();

		NeuraServices.SubscriptionsAPI.executeSubscriptionRequest(neuraClient, subscriptionRequest, new SubscriptionRequestCallbacks() {
			@Override
			public void onSuccess(String eventName, Bundle resultData, String identifier) {
				Log.i(TAG, String.format("Successfully unsubscribed for %s", eventName));
			}

			@Override
			public void onFailure(String eventName, Bundle resultData, int errorCode) {
				Log.e(TAG, String.format("Error unsubscribing to %s with error %s", eventName, NeuraUtil.errorCodeToString(errorCode)));
			}
		});
	}
}
