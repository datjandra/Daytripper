package com.daytripper.app.ui.components;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.daytripper.app.Daytripper;
import com.daytripper.app.R;
import com.daytripper.app.service.Actionable;
import com.daytripper.app.service.UberRequest;
import com.daytripper.app.service.UberRequestClient;
import com.daytripper.app.service.UberRequestConstants;
import com.daytripper.app.service.UberRequestListener;
import com.daytripper.app.util.ResourceUtils;

public class UberOAuthFragment extends DialogFragment implements UberRequestConstants {

	private static final String TAG = "UberOAuthFragment";
	private final static String DIALOG_TITLE = "Uber Status";
	private final static String CLIENT_ID = "Cshqu6pqTo9hPRF1Q1zwaAzQ8CuyZzBY";
	private final static String UBER_AUTH_URL = "https://login.uber.com/oauth/authorize?response_type=code&scope=request&client_id=" + CLIENT_ID;
	
	private UberRequest uberRequest;
	private UberRequestListener uberRequestListener;
	private WebView webView;
	private String htmlContent;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.activity_web, container, false);
		webView = (WebView) view.findViewById(R.id.webview);
		Button okButton = (Button) view.findViewById(R.id.ok_button);
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		getDialog().setTitle(DIALOG_TITLE);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle bundle) {
		super.onViewCreated(view, bundle);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.i(TAG, "Override url -> " + url);
				Uri uri = Uri.parse(url);
				String code = uri.getQueryParameter(UberRequestConstants.PARAM_CODE);
				if (!TextUtils.isEmpty(code)) {
					uberRequest.setCode(code);
					new UberRequestTask().execute(uberRequest);
					return true;
				}
				return false;
			}
		});
		webView.setHorizontalScrollBarEnabled(true);
		webView.setVerticalScrollBarEnabled(true);
		webView.clearHistory();
	    webView.clearFormData();
	    webView.clearCache(true);
		
		WebSettings webSettings = webView.getSettings();
        webSettings.setSaveFormData(false);
        webSettings.setSavePassword(false);
        
        if (!TextUtils.isEmpty(htmlContent)) {
        	webView.loadData(htmlContent, "text/html", "UTF-8");
        	return;
        }
        
        uberRequest = new UberRequest();
		Map<String,String> prefs = readPrefs();
		if (prefs.containsKey(PARAM_REQUEST_ID)) {
    		uberRequest.setRequestId(prefs.get(PARAM_REQUEST_ID));
    	}
    	
    	if (prefs.containsKey(PARAM_ACCESS_TOKEN)) {
    		uberRequest.setAccessToken(prefs.get(PARAM_ACCESS_TOKEN));
    	}
    
    	Bundle args = getArguments();
    	uberRequest.setProductId(args.getString(PARAM_PRODUCT_ID));
    	uberRequest.setStartLatitude(args.getString(PARAM_START_LATITUDE));
    	uberRequest.setEndLatitude(args.getString(PARAM_END_LATITUDE));
    	uberRequest.setStartLongitude(args.getString(PARAM_START_LONGITUDE));
    	uberRequest.setEndLongitude(args.getString(PARAM_END_LONGITUDE));
    	uberRequest.setSurgeConfirmationId(args.getString("surge_confirmation_id"));
    	uberRequest.setVerb(VERB_CALL);
    	uberRequest.setObject(OBJECT_UBER);
    	uberRequest.setTestMode(args.getBoolean("test_mode"));
    	uberRequest.setMethod(HttpPost.METHOD_NAME);
    	
        try {
			String authUrl = String.format("%s&redirect_uri=%s", UBER_AUTH_URL, URLEncoder.encode(Actionable.ENTITY_ACTION_URL, "UTF-8"));
			Log.i(TAG, "authUrl -> " + authUrl);
			webView.loadUrl(authUrl);
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, e.getMessage());
		}   
	}
	
	public void setUberRequestListener(UberRequestListener uberRequestListener) {
		this.uberRequestListener = uberRequestListener;
	}
	
	public void setHtmlContent(String htmlContent) {
		this.htmlContent = htmlContent;
	}
	
	private Map<String,String> readPrefs() {
		Map<String,String> params = new HashMap<String,String>();
		SharedPreferences sharedPrefs = getActivity().getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
		Map<String,?> allPrefs = sharedPrefs.getAll();
		Set<String> entryKeys = allPrefs.keySet();
		for (String key : entryKeys) {
			params.put(key, allPrefs.get(key).toString());
		}
		return params;
	}
	
	private void showPendingRequest(String jsonString) throws JSONException {
		Log.i(TAG, "showPendingRequest()");
		String message = getResources().getString(R.string.uber_request_sent);
		JSONObject json = new JSONObject(jsonString);
		
		String requestId = json.getString(FIELD_REQUEST_ID);
		String accessToken = json.getString(FIELD_ACCESS_TOKEN);
		SharedPreferences sharedPrefs = getActivity().getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putString(FIELD_REQUEST_ID, requestId);
		editor.putString(FIELD_ACCESS_TOKEN, accessToken);
		editor.commit();
		
		String messageTemplate = ResourceUtils.readTextFromResource(getActivity(), R.raw.message_template);
		webView.loadData(String.format(messageTemplate, message, requestId), "text/html", "UTF-8");
	}
	
	/*
	private void writePrefs(Map<String,String> params) {
		SharedPreferences sharedPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		Set<Entry<String,String>> entries = params.entrySet();
		SharedPreferences.Editor editor = sharedPrefs.edit();
		for (Entry<String,String> entry : entries) {
			editor.putString(entry.getKey(), entry.getValue());
		}
		editor.commit();
	}
	
	private void removePref(String key) {
		SharedPreferences sharedPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.remove(key);
		editor.commit();
	}
	*/
	
	private class UberRequestTask extends AsyncTask<UberRequest, Void, String> {

		@Override
    	protected void onPostExecute(String jsonResponse) {
			if (uberRequestListener != null) {
				uberRequestListener.onRequestSent();
			}
			try {
				showPendingRequest(jsonResponse);
			} catch (JSONException e) {
				Log.e(TAG, e.getMessage());
				if (uberRequestListener != null) {
					uberRequestListener.onNoResponse();
				}
			}
		}
		
		@Override
		protected String doInBackground(UberRequest... params) {
			return UberRequestClient.doPost(params[0]);
		}
	}
}
