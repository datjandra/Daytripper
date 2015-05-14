package com.daytripper.app.ui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
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

	private static final String TAG = "WebActivity";
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
			    	new EntityActionTask(uberRequest).execute();
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
		    	new EntityActionTask(uberRequest).execute();
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
	
	private void write(Map<String,String> params) {
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
	
	private void showPendingRequest(JSONObject json) throws JSONException {
		Log.i(TAG, "showPendingRequest()");
		String requestId = json.getString("requestId");
		String messageTemplate = ResourceUtils.readTextFromResource(this, R.raw.message_template);
		webView.loadData(String.format(messageTemplate, REQUEST_MESSAGE, requestId), "text/html", "UTF-8");
	}
	
	private void showActiveRequest(JSONObject json) throws JSONException {
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
	
	private void showCancelRequest(JSONObject json) throws JSONException {
		String requestId = "-";
		if (json.has("requestId") && !json.isNull("requestId")) {
			requestId = json.getString("requestId");
		}
		
		String messageTemplate = ResourceUtils.readTextFromResource(this, R.raw.message_template);
		webView.loadData(String.format(messageTemplate, CANCEL_MESSAGE, requestId), "text/html", "UTF-8");
	}
	
	private void showNoResponse() {
		webView.loadData("<h4>No results found</h4>", "text/html", "UTF-8");
	}
	
	private class EntityActionTask extends AsyncTask<Void, Void, String> {
		
		private UberRequest uberRequest;
		
		private EntityActionTask(UberRequest uberRequest) {
			this.uberRequest = uberRequest;
		}
		
    	@Override
		protected String doInBackground(Void...args) {
    		String method = uberRequest.getMethod();
    		if (method != null && method.equalsIgnoreCase(HttpPost.METHOD_NAME)) {
    			return doPost();
    		}
    		return doGet();
    	}
    
    	/*
    	 * Request Statuses
    		All possible statues of a Request's life cycle.
    		Status 	Description
    		processing 	The Request is matching to the most efficient available driver.
    		no_drivers_available 	The Request was unfulfilled because no drivers were available.
    		accepted 	The Request has been accepted by a driver and is "en route" to the start location (i.e. start_latitude and start_longitude).
    		arriving 	The driver has arrived or will be shortly.
    		in_progress 	The Request is "en route" from the start location to the end location.
    		driver_canceled 	The Request has been canceled by the driver.
    		rider_canceled 	The Request canceled by rider.
    		completed 	Request has been completed by the driver
    	 */
    	@Override
    	protected void onPostExecute(String jsonResponse) {
    		Log.i(TAG, "onPostExecute()");
    		if (jsonResponse == null) {
        		showNoResponse();	
    			return;
    		}
    		
    		try {
    			Map<String,String> params = new HashMap<String,String>();
				JSONObject json = new JSONObject(jsonResponse);
				String requestId = null;
				if (json.has("requestId") && !json.isNull("requestId")) {
					requestId = json.getString("requestId");
					params.put("request_id", requestId);
				}
				
				if (json.has("accessToken") && !json.isNull("accessToken")) {
					params.put("access_token", json.getString("accessToken"));
				}
				
				if (!params.isEmpty()) {
					write(params);
				}
				
				String action = uberRequest.getVerb();
				if (!TextUtils.isEmpty(action)) {
					if (action.equals(VERB_CALL)) {
						showPendingRequest(json);
					} else if (action.equals(VERB_LOOKUP)) {
						showActiveRequest(json);
					} else if (action.equals(VERB_CANCEL)) {
						showCancelRequest(json);
					}
				}
			} catch (JSONException e) {
				Log.e(TAG, e.getMessage());
			}
    	}
    	
    	private String doPost() {
    		String jsonResponse = null;
    		try {
    			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
    			if (!TextUtils.isEmpty(uberRequest.getAccessToken())) {
    				nameValuePairs.add(new BasicNameValuePair("access_token", uberRequest.getAccessToken()));
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getCode())) {
    				nameValuePairs.add(new BasicNameValuePair("code", uberRequest.getCode()));
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getEndLatitude())) {
    				nameValuePairs.add(new BasicNameValuePair("end_latitude", uberRequest.getEndLatitude()));
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getEndLongitude())) {
    				nameValuePairs.add(new BasicNameValuePair("end_longitude", uberRequest.getEndLongitude()));
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getProductId())) {
    				nameValuePairs.add(new BasicNameValuePair("product_id", uberRequest.getProductId()));
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getRequestId())) {
    				nameValuePairs.add(new BasicNameValuePair("request_id", uberRequest.getRequestId()));
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getStartLatitude())) {
    				nameValuePairs.add(new BasicNameValuePair("start_latitude", uberRequest.getStartLatitude()));
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getStartLongitude())) {
    				nameValuePairs.add(new BasicNameValuePair("start_longitude", uberRequest.getStartLongitude()));
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getSurgeConfirmationId())) {
    				nameValuePairs.add(new BasicNameValuePair("surge_confirmation_id", uberRequest.getSurgeConfirmationId()));
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getObject())) {
    				nameValuePairs.add(new BasicNameValuePair("object", uberRequest.getObject()));
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getVerb())) {
    				nameValuePairs.add(new BasicNameValuePair("verb", uberRequest.getVerb()));
    			}
    			
    			HttpParams httpParameters = new BasicHttpParams();
    			HttpConnectionParams.setConnectionTimeout(httpParameters,
    					Actionable.CONNECTION_TIMEOUT);
    			HttpConnectionParams.setSoTimeout(httpParameters, Actionable.SOCKET_TIMEOUT);
    			
    			HttpClient httpClient = new DefaultHttpClient(httpParameters);
    			HttpPost post = new HttpPost(Actionable.ENTITY_ACTION_URL);
        		post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        		HttpResponse response = httpClient.execute(post);
        		jsonResponse = EntityUtils.toString(response.getEntity());
    		} catch (Exception e) {
    			StringWriter sw = new StringWriter();
    			PrintWriter writer = new PrintWriter(sw);
    			e.printStackTrace(writer);
    			Log.e(TAG, sw.toString());
    		}
    		return jsonResponse;
    	}
    	
    	private String doGet() {
    		String jsonResponse = null;
    		try {
    			HttpParams httpParameters = new BasicHttpParams();
    			HttpConnectionParams.setConnectionTimeout(httpParameters,
    					Actionable.CONNECTION_TIMEOUT);
    			HttpConnectionParams.setSoTimeout(httpParameters, Actionable.SOCKET_TIMEOUT);
    			
    			if (!TextUtils.isEmpty(uberRequest.getAccessToken())) {
    				httpParameters.setParameter("access_token", uberRequest.getAccessToken());
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getCode())) {
    				httpParameters.setParameter("code", uberRequest.getCode());
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getEndLatitude())) {
    				httpParameters.setParameter("end_latitude", uberRequest.getEndLatitude());
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getEndLongitude())) {
    				httpParameters.setParameter("end_longitude", uberRequest.getEndLongitude());
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getProductId())) {
    				httpParameters.setParameter("product_id", uberRequest.getProductId());
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getRequestId())) {
    				httpParameters.setParameter("request_id", uberRequest.getRequestId());
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getStartLatitude())) {
    				httpParameters.setParameter("start_latitude", uberRequest.getStartLatitude());
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getStartLongitude())) {
    				httpParameters.setParameter("start_longitude", uberRequest.getStartLongitude());
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getSurgeConfirmationId())) {
    				httpParameters.setParameter("surge_confirmation_id", uberRequest.getSurgeConfirmationId());
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getObject())) {
    				httpParameters.setParameter("object", uberRequest.getObject());
    			}
    			
    			if (!TextUtils.isEmpty(uberRequest.getVerb())) {
    				httpParameters.setParameter("verb", uberRequest.getVerb());
    			}
    			
    			HttpClient httpClient = new DefaultHttpClient(httpParameters);
    			HttpGet get = new HttpGet(Actionable.ENTITY_ACTION_URL);
        		get.setParams(httpParameters);	
        		HttpResponse response = httpClient.execute(get);
        		jsonResponse = EntityUtils.toString(response.getEntity());
    		} catch (Exception e) {
    			Log.e(TAG, e.getMessage());
    		}
    		return jsonResponse;
    	}
	}
}
