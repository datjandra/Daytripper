package com.vocifery.daytripper.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class NeuraUtils {

	private static String KEY_NEURA_ACCESS_TOKEN = "com.vocifery.daytripper.util.KEY_NEURA_ACCESS_TOKEN";
	private static String KEY_EVENT_NAME = "com.vocifery.daytripper.util.KEY_EVENT_NAME";

	/**
	 * The app will clear the token locally
	 * 
	 * @param context
	 */
	public static void clearToken(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().clear().commit();
	}

	/**
	 * Save the accessToken persistently
	 * 
	 * @param context
	 * @param accessToken
	 */
	public static void saveAccessTokenPersistent(Context context, String accessToken) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().putString(KEY_NEURA_ACCESS_TOKEN, accessToken).commit();
	}

	public static String getAccessToken(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(KEY_NEURA_ACCESS_TOKEN, null);
	}

	public static void saveEventName(Context context, String eventName) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().putString(KEY_EVENT_NAME, eventName).commit();
	}

	public static String getEventName(Context context) {
		SharedPreferences prefs  = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(KEY_EVENT_NAME, null);
	}

	public static void saveEventDetails(Context context, String eventName, String eventDetails) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().putString(eventName, eventDetails).commit();
	}

	public static String getEventDetails(Context context, String eventName) {
		SharedPreferences prefs  = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(eventName, null);
	}
}
