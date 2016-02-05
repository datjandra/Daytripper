package com.vocifery.daytripper.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.mapquest.android.maps.GeoPoint;
import com.vocifery.daytripper.R;
import com.vocifery.daytripper.service.ResponderService;
import com.vocifery.daytripper.ui.Daytripper;
import com.vocifery.daytripper.util.HttpUtils;
import com.vocifery.daytripper.util.QueryParser;

@SuppressWarnings("deprecation")
public class PickUpAction implements Actionable {

	private final static String KEYWORD_FROM = "from";
	private final static String KEYWORD_TO = "to";
	private final static String TAG = "PickUpAction";
		
	@Override
	public String getVerb() {
		return null;
	}

	@Override
	public String getObject() {
		return null;
	}

	@Override
	public String getSource() {
		return this.getClass().getName();
	}

	@Override
	public String doActionWithIntent(Intent intent) {
		String query = intent.getStringExtra(ResponderService.KEY_QUERY);
		String startLocation = intent.getStringExtra(ResponderService.KEY_lOCATION);
		
		final Daytripper daytripper = (Daytripper) Daytripper.getAppContext();
		String endLocation = QueryParser.extractDestinationFromQuery(query, daytripper.getAllItems());
		
		String[] words = query.split("\\s+");
		for (String word : words) {
			if (word.toLowerCase(Locale.getDefault()).contains(KEYWORD_FROM)) {
				startLocation = null;
			} else if (word.toLowerCase(Locale.getDefault()).contains(KEYWORD_TO)) {
				endLocation = null;
				daytripper.setSelectedPoint(null);
			}
		}
		
		if (TextUtils.isEmpty(endLocation)) {
			GeoPoint selectedPoint = daytripper.getSelectedPoint();
			if (selectedPoint != null) {
				endLocation = String.format(Locale.getDefault(),
						"%10.6f, %10.6f", selectedPoint.getLatitude(), selectedPoint.getLongitude());
			}
			daytripper.setSelectedPoint(null);
		}
		
		Log.d(TAG, query);
		return doVehicleSearch(startLocation, endLocation, query);
	}

	@Override
	public String getCustomMessage() {
		return Daytripper.getAppContext().getString(R.string.success_message_ride);
	}
	
	private String doVehicleSearch(String startLocation, String endLocation, String query) {
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("query", query));
		
		if (!TextUtils.isEmpty(startLocation)) {
			nameValuePairs.add(new BasicNameValuePair("ll", startLocation));
		}

		if (!TextUtils.isEmpty(endLocation)) {
			nameValuePairs.add(new BasicNameValuePair("destination", endLocation));
		}
		
		Log.d(TAG, String.format(Locale.getDefault(), "start: %s, end: %s", startLocation, endLocation));
		String jsonResponse = HttpUtils.doPost(SEARCH_URL, nameValuePairs);
		Log.d(TAG, String.format(Locale.getDefault(), "Response length=%d", jsonResponse.length()));
		return jsonResponse;
	}
}
