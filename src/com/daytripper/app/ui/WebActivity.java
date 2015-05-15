package com.daytripper.app.ui;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.daytripper.app.R;
import com.daytripper.app.service.Actionable;
import com.daytripper.app.service.UberRequestTask;
import com.daytripper.app.service.UberRequest;
import com.daytripper.app.util.ResourceUtils;

public class WebActivity extends FragmentActivity implements OnClickListener {
	
	public final static String PARAM_PRODUCT_ID = "product_id";
	public final static String PARAM_START_LATITUDE = "start_latitude";
	public final static String PARAM_END_LATITUDE = "end_latitude";
	public final static String PARAM_START_LONGITUDE = "start_longitude";
	public final static String PARAM_END_LONGITUDE = "end_longitude";
	public final static String PARAM_SURGE_CONFIRMATION_ID = "surge_confirmation_id";
	public final static String PARAM_VERB = "verb";
	public final static String PARAM_OBJECT = "object";
	
	public final static String VERB_CALL = "call";
	public final static String VERB_LOOKUP = "lookup";
	public final static String VERB_CANCEL = "cancel";
	public final static String OBJECT_UBER = "uber";
	public static final String REQUEST_MESSAGE = "Request for cab has been sent.";
	public static final String CANCEL_MESSAGE = "Cancellation request has been sent.";
	public static final String LOOKUP_MESSAGE = "Here is the the Uber cab's status.";

	static final String TAG = "WebActivity";
	private static final String CLIENT_ID = "Cshqu6pqTo9hPRF1Q1zwaAzQ8CuyZzBY";
	private static final String UBER_AUTH_URL = "https://login.uber.com/oauth/authorize?response_type=code&scope=request&client_id=" + CLIENT_ID;
	private static final String DEFAULT_MESSAGE = "Vehicle %s %s (License %s) has an eta of %d minutes.";
	
	private static final String ACTION_REQUEST = "request";
	private static final String ACTION_DETAILS = "details";
	private static final String ACTION_CANCEL = "cancel";
	
	private WebView webView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate()");
		
        
		setContentView(R.layout.activity_web);
		final Bundle bundle = getIntent().getExtras();
		final Map<String,String> prefs = read();
		final String action = bundle.getString(PARAM_VERB);
		
		webView = (WebView) findViewById(R.id.webview);
		webView.setWebViewClient(new WebViewClient() {
			@Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.w(TAG, String.format("redirect url -> %s", url));
				Uri uri = Uri.parse(url);
				String code = uri.getQueryParameter("code");
				Log.i(TAG, "code = " + code);
				if (!TextUtils.isEmpty(code)) {
					UberRequest uberRequest = new UberRequest();
					if (prefs.containsKey("request_id")) {
			    		uberRequest.setRequestId(prefs.get("request_id"));
			    	}
			    	
			    	if (prefs.containsKey("access_token")) {
			    		uberRequest.setAccessToken(prefs.get("access_token"));
			    	}
			    
			    	uberRequest.setCode(code);
			    	uberRequest.setProductId(bundle.getString(PARAM_PRODUCT_ID));
			    	uberRequest.setStartLatitude(bundle.getString(PARAM_START_LATITUDE));
			    	uberRequest.setEndLatitude(bundle.getString(PARAM_END_LATITUDE));
			    	uberRequest.setStartLongitude(bundle.getString(PARAM_START_LONGITUDE));
			    	uberRequest.setEndLongitude(bundle.getString(PARAM_END_LONGITUDE));
			    	uberRequest.setSurgeConfirmationId(bundle.getString("surge_confirmation_id"));
			    	uberRequest.setVerb(action);
			    	uberRequest.setObject(bundle.getString(PARAM_OBJECT));
			    	uberRequest.setTestMode(bundle.getBoolean("test_mode"));
			    	uberRequest.setMethod(HttpPost.METHOD_NAME);
			    	return true;
				}	
		    	return false;
		    }
		});
		
		webView.setHorizontalScrollBarEnabled(true);
		webView.setVerticalScrollBarEnabled(true);
		try {
			if (action.equals(VERB_CALL)) {
				String url = String.format("%s&redirect_uri=%s", UBER_AUTH_URL, URLEncoder.encode(Actionable.ENTITY_ACTION_URL, "UTF-8"));
				Log.i(TAG, String.format("authorize url -> %s", url));
				webView.loadUrl(url);
			} else {
				UberRequest uberRequest = new UberRequest();
				if (prefs.containsKey("request_id")) {
		    		uberRequest.setRequestId(prefs.get("request_id"));
		    	}
		    	
		    	if (prefs.containsKey("access_token")) {
		    		uberRequest.setAccessToken(prefs.get("access_token"));
		    	}
		    	
		    	uberRequest.setVerb(bundle.getString(PARAM_VERB));
		    	uberRequest.setObject(bundle.getString(PARAM_OBJECT));
		    	uberRequest.setTestMode(bundle.getBoolean("test_mode"));
		    	uberRequest.setMethod(HttpPost.METHOD_NAME);
			}
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, e.getMessage());
		}

		Button okButton = (Button) findViewById(R.id.ok_button);
		okButton.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.ok_button:
				finish();
				break;
			
			default:
				break;
		}
	}
	
	void write(Map<String,String> params) {
		SharedPreferences sharedPrefs = getPreferences(Context.MODE_PRIVATE);
		Set<Entry<String,String>> entries = params.entrySet();
		SharedPreferences.Editor editor = sharedPrefs.edit();
		for (Entry<String,String> entry : entries) {
			editor.putString(entry.getKey(), entry.getValue());
		}
		editor.commit();
	}
	
	private Map<String,String> read() {
		Map<String,String> params = new HashMap<String,String>();
		SharedPreferences sharedPrefs = getPreferences(Context.MODE_PRIVATE);
		Map<String,?> allPrefs = sharedPrefs.getAll();
		Set<String> entryKeys = allPrefs.keySet();
		for (String key : entryKeys) {
			params.put(key, allPrefs.get(key).toString());
		}
		return params;
	}
	
	void showPendingRequest(JSONObject json) throws JSONException {
		Log.i(TAG, "showPendingRequest()");
		String requestId = json.getString("requestId");
		String messageTemplate = ResourceUtils.readTextFromResource(this, R.raw.message_template);
		webView.loadData(String.format(messageTemplate, REQUEST_MESSAGE, requestId), "text/html", "UTF-8");
	}
	
	void showActiveRequest(JSONObject json) throws JSONException {
		String requestId = "";
		if (json.has("requestId") && !json.isNull("requestId")) {
			requestId = json.getString("requestId");
		}
		
		String status = "";
		if (json.has("status") && !json.isNull("status")) {
			status = json.getString("status");
		}
		
		String driverName = "";
		if (json.has("driverName") && !json.isNull("driverName")) {
			driverName = json.getString("driverName");
		}
		
		String driverPhoneNumber = "";
		if (json.has("driverPhoneNumber") && !json.isNull("driverPhoneNumber")) {
			driverPhoneNumber = json.getString("driverPhoneNumber");
		}
		
		String driverPictureUrl = "";
		if (json.has("driverPictureUrl") && !json.isNull("driverPictureUrl")) {
			driverPictureUrl = json.getString("driverPictureUrl");
		}
		
		Integer eta = -1;
		if (json.has("eta") && !json.isNull("eta")) {
			eta = json.getInt("eta");
		}
		
		String vehicleMake = "";
		if (json.has("vehicleMake") && !json.isNull("vehicleMake")) {
			vehicleMake = json.getString("vehicleMake");
		}
		
		String vehicleModel = "";
		if (json.has("vehicleModel") && !json.isNull("vehicleModel")) {
			vehicleModel = json.getString("vehicleModel");
		}
		
		String vehicleLicensePlate = "";
		if (json.has("vehicleLicensePlate") && !json.isNull("vehicleLicensePlate")) {
			vehicleLicensePlate = json.getString("vehicleLicensePlate");
		}

		String detailMessage = String.format(Locale.getDefault(), DEFAULT_MESSAGE, vehicleMake, vehicleModel, vehicleLicensePlate, eta);
		String detailTemplate = ResourceUtils.readTextFromResource(this, R.raw.detail_template);
		webView.loadData(String.format(detailTemplate, driverPictureUrl, driverName, driverPhoneNumber, detailMessage), "text/html", "UTF-8");
	}
	
	void showCancelRequest(JSONObject json) throws JSONException {
		String requestId = "-";
		if (json.has("requestId") && !json.isNull("requestId")) {
			requestId = json.getString("requestId");
		}
		
		String messageTemplate = ResourceUtils.readTextFromResource(this, R.raw.message_template);
		webView.loadData(String.format(messageTemplate, CANCEL_MESSAGE, requestId), "text/html", "UTF-8");
	}
	
	void showNoResponse() {
		webView.loadData("<h4>No results found</h4>", "text/html", "UTF-8");
	}
}
