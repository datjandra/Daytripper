package com.vocifery.daytripper.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.language.DoubleMetaphone;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.vocifery.daytripper.Daytripper;
import com.vocifery.daytripper.R;
import com.vocifery.daytripper.ui.MainActivity;
import com.vocifery.daytripper.util.StringUtils;
import com.vocifery.daytripper.util.TrieNode;

public class ResponderService extends IntentService implements UberRequestConstants {

	public static final String ACTION_REQUEST = "com.vocifery.daytripper.REQUEST";
	public static final String ACTION_RESPONSE = "com.vocifery.daytripper.RESPONSE";
	public static final String ACTION_SOURCE = "com.vocifery.daytripper.SOURCE";
	
	public static final String EXTRA_MESSAGE = "com.vocifery.daytripper.extra.MESSAGE";
	public static final String CUSTOM_MESSAGE = "com.vocifery.daytripper.extra.CUSTOM_MESSAGE";
	public static final String UBER_STATUS_MESSAGE = "com.vocifery.daytripper.extra.UBER_STATUS_MESSAGE";
	public static final String UBER_CANCEL_MESSAGE = "com.vocifery.daytripper.extra.UBER_CANCEL_MESSAGE";
	public static final String MAP_ZOOM_MESSAGE = "com.vocifery.daytripper.extra.MAP_ZOOM_MESSAGE";
	public static final String NAME_MESSAGE = "com.vocifery.daytripper.extra.NAME_MESSAGE";
	public static final String NO_OP_MESSAGE = "com.vocifery.daytripper.extra.NO_OP_MESSAGE";
	
	public static final String KEY_QUERY = "com.vocifery.daytripper.QUERY";
	public static final String KEY_lOCATION = "com.vocifery.daytripper.LOCATION";
	public static final String KEY_DESTINATION = "com.vocifery.daytripper.DESTINATION";
	public static final String KEY_PAGE = "com.vocifery.daytripper.PAGE";
	public static final String KEY_COUNT = "com.vocifery.daytripper.COUNT";
	
	public static final Actionable DEFAULT_ACTION = new DefaultAction();
	public static final Actionable PICKUP_ACTION = new PickUpAction();
	public static final Actionable CANCEL_UBER_ACTION = new CancelUberAction();
	public static final Actionable LOOKUP_UBER_ACTION = new LookupUberAction();
	public static final Actionable MAP_ZOOM_ACTION = new MapZoomAction();
	public static final Actionable SHUT_UP_ACTION = new ShutUpAction();
	public static final Actionable SAY_SOMETHING_ACTION = new SaySomethingAction();
	public static final Actionable NAME_ACTION = new NameAction();
	public static final Actionable NO_REPLY_ACTION = new NoReplyAction();
	
	private static final String TAG = "ResponderService";
	private static final TrieNode<String,Actionable> ACTIONS = new TrieNode<String,Actionable>();
	private static final Map<String,String> KEYWORD_HASHES = new HashMap<String,String>();
	private static final String JSON_MESSAGE = "{ \"mssage\" : \"%s\" }";
	private static final int NGRAM_SIZE = 3;
		
	static {
		ACTIONS.addPattern(new String[] {"pick", "up"}, PICKUP_ACTION);
		ACTIONS.addPattern(new String[] {"drive", "me"}, PICKUP_ACTION);
		ACTIONS.addPattern(new String[] {"take", "me"}, PICKUP_ACTION);
		ACTIONS.addPattern(new String[] {"cancel", "uber"}, CANCEL_UBER_ACTION);
		ACTIONS.addPattern(new String[] {"show", "uber"}, LOOKUP_UBER_ACTION);
		ACTIONS.addPattern(new String[] {"zoom", "level"}, MAP_ZOOM_ACTION);
		ACTIONS.addPattern(new String[] {"shut", "up"}, SHUT_UP_ACTION);
		ACTIONS.addPattern(new String[] {"be", "quiet"}, SHUT_UP_ACTION);
		ACTIONS.addPattern(new String[] {"stop", "talking"}, SHUT_UP_ACTION);
		ACTIONS.addPattern(new String[] {"say", "something"}, SAY_SOMETHING_ACTION);
		ACTIONS.addPattern(new String[] {"talk", "to", "me"}, SAY_SOMETHING_ACTION);
		ACTIONS.addPattern(new String[] {"speak", "up"}, SAY_SOMETHING_ACTION);
		ACTIONS.addPattern(new String[] {"my", "name", "is"}, NAME_ACTION);
		ACTIONS.addPattern(new String[] {"yes"}, NO_REPLY_ACTION);
		ACTIONS.addPattern(new String[] {"no"}, NO_REPLY_ACTION);
		
		DoubleMetaphone encoder = new DoubleMetaphone();
		KEYWORD_HASHES.put(encoder.encode("meetup events"), "meetup events");
		KEYWORD_HASHES.put(encoder.encode("meetup groups"), "meetup groups");
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
		
		DoubleMetaphone encoder = new DoubleMetaphone();
		Set<String> ngrams = StringUtils.extractNgrams(query, NGRAM_SIZE);
		for (String segment : ngrams) {
			String hash = encoder.encode(segment);
			if (KEYWORD_HASHES.containsKey(hash)) {
				query = query.replace(segment, KEYWORD_HASHES.get(hash));
				break;
			}
		}
		
		String[] pattern = query.split("\\s+");
		Actionable actionable = ACTIONS.lookup(pattern);
		
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(ACTION_RESPONSE);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		
		if (actionable == null) {
			String response = DEFAULT_ACTION.doActionWithIntent(intent);
			if (!TextUtils.isEmpty(response)) {
		        broadcastIntent.putExtra(EXTRA_MESSAGE, response);
			} else {
				String errorMessage = String.format(Locale.getDefault(), JSON_MESSAGE, getResources().getString(R.string.system_error_message));
				broadcastIntent.putExtra(EXTRA_MESSAGE, errorMessage);	
			}
		} else {
			SharedPreferences prefs = getApplicationContext().getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
			intent.putExtra(FIELD_ACCESS_TOKEN, prefs.getString(FIELD_ACCESS_TOKEN, null));
			intent.putExtra(FIELD_REQUEST_ID, prefs.getString(FIELD_REQUEST_ID, null));
			
			String response = actionable.doActionWithIntent(intent);
			String customMessage = actionable.getCustomMessage();
			if (!TextUtils.isEmpty(customMessage)) {
				broadcastIntent.putExtra(CUSTOM_MESSAGE, customMessage);
			}
			
			if (!TextUtils.isEmpty(response)) {
		        if (actionable.equals(LOOKUP_UBER_ACTION)) {
		        	broadcastIntent.putExtra(UBER_STATUS_MESSAGE, response);
				} else if (actionable.equals(CANCEL_UBER_ACTION)) {
					broadcastIntent.putExtra(UBER_CANCEL_MESSAGE, response);
				} else if (actionable.equals(MAP_ZOOM_ACTION)) {
					broadcastIntent.putExtra(MAP_ZOOM_MESSAGE, response);
				} else if (actionable.equals(PICKUP_ACTION)) {
					broadcastIntent.putExtra(EXTRA_MESSAGE, response);
				} else if (actionable.equals(SHUT_UP_ACTION) || actionable.equals(SAY_SOMETHING_ACTION)) {
					broadcastIntent.putExtra(MainActivity.VOCIFEROUS_KEY, 
							intent.getBooleanExtra(MainActivity.VOCIFEROUS_KEY, true));
				} else if (actionable.equals(NAME_ACTION)) {
					broadcastIntent.putExtra(NAME_MESSAGE, response);
				} else {
					broadcastIntent.putExtra(NO_OP_MESSAGE, response);
				}
			} else {
				String uberErrorMessage = String.format(Locale.getDefault(), JSON_MESSAGE, getResources().getString(R.string.system_error_message));
				broadcastIntent.putExtra(EXTRA_MESSAGE, uberErrorMessage);
			}
		}	
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
	}
}
