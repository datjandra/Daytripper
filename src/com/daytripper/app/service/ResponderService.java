package com.daytripper.app.service;

import java.util.Locale;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.daytripper.app.Daytripper;
import com.daytripper.app.util.TrieNode;

public class ResponderService extends IntentService implements UberRequestConstants {

	public static final String ACTION_REQUEST = "com.daytripper.app.REQUEST";
	public static final String ACTION_RESPONSE = "com.daytripper.app.RESPONSE";
	public static final String ACTION_SOURCE = "com.daytripper.app.SOURCE";
	
	public static final String EXTRA_MESSAGE = "com.daytripper.app.extra.MESSAGE";
	public static final String CUSTOM_MESSAGE = "com.daytripper.app.extra.CUSTOM_MESSAGE";
	public static final String UBER_STATUS_MESSAGE = "com.daytripper.app.extra.UBER_STATUS_MESSAGE";
	public static final String UBER_CANCEL_MESSAGE = "com.daytripper.app.extra.UBER_CANCEL_MESSAGE";
	
	public static final String KEY_QUERY = "com.daytripper.app.QUERY";
	public static final String KEY_lOCATION = "com.daytripper.app.LOCATION";
	public static final String KEY_DESTINATION = "com.daytripper.app.DESTINATION";
	public static final String KEY_PAGE = "com.daytripper.app.PAGE";
	public static final String KEY_COUNT = "com.daytripper.app.COUNT";
	
	public static final Actionable DEFAULT_ACTION = new DefaultAction();
	public static final Actionable CANCEL_UBER_ACTION = new CancelUberAction();
	public static final Actionable LOOKUP_UBER_ACTION = new LookupUberAction();

	private static final String TAG = "ResponderService";
	private static final TrieNode<String,Actionable> ACTIONS = new TrieNode<String,Actionable>();
	static {
		ACTIONS.addPattern(new String[] {"cancel", "uber"}, CANCEL_UBER_ACTION);
		ACTIONS.addPattern(new String[] {"show", "uber"}, LOOKUP_UBER_ACTION);
	}
	
	public ResponderService() {
		super("ResponderService");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "onCreate()");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "onHandleIntent()");
		String query = intent.getStringExtra(KEY_QUERY);
		query = query.toLowerCase(Locale.getDefault()).replace("\'", "");
		
		String[] pattern = query.split("\\s+");
		Actionable actionable = ACTIONS.lookup(pattern);
		if (actionable == null) {
			String response = DEFAULT_ACTION.doActionWithIntent(intent);
			if (!TextUtils.isEmpty(response)) {
				Intent broadcastIntent = new Intent();
				broadcastIntent.setAction(ACTION_RESPONSE);
		        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		        broadcastIntent.putExtra(EXTRA_MESSAGE, response);
				LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
			}	
		} else {
			SharedPreferences prefs = getApplicationContext().getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
			intent.putExtra(FIELD_ACCESS_TOKEN, prefs.getString(FIELD_ACCESS_TOKEN, null));
			intent.putExtra(FIELD_REQUEST_ID, prefs.getString(FIELD_REQUEST_ID, null));
			
			String response = actionable.doActionWithIntent(intent);
			if (!TextUtils.isEmpty(response)) {
				Intent broadcastIntent = new Intent();
				broadcastIntent.setAction(ACTION_RESPONSE);
		        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		        if (actionable.equals(LOOKUP_UBER_ACTION)) {
		        	broadcastIntent.putExtra(UBER_STATUS_MESSAGE, response);
				} else if (actionable.equals(CANCEL_UBER_ACTION)) {
					broadcastIntent.putExtra(UBER_CANCEL_MESSAGE, response);
				}
				LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
			}	
		}	
	}
}
