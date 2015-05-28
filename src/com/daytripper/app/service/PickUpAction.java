package com.daytripper.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.daytripper.app.Daytripper;
import com.daytripper.app.util.QueryParser;
import com.mapquest.android.maps.GeoPoint;

public class PickUpAction implements Actionable {

	private final static String PICKUP_QUERY = "pick up";
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
		if (TextUtils.isEmpty(endLocation)) {
			GeoPoint selectedPoint = daytripper.getSelectedPoint();
			if (selectedPoint != null) {
				endLocation = String.format(Locale.getDefault(),
						"%10.6f, %10.6f", selectedPoint.getLatitude(), selectedPoint.getLongitude());
			}
			daytripper.setSelectedPoint(null);
		}
		return doVehicleSearch(startLocation, endLocation);
	}

	@Override
	public String getCustomMessage() {
		return null;
	}
	
	private String doVehicleSearch(String startLocation, String endLocation) {
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters,
				CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParameters, SOCKET_TIMEOUT);

		HttpClient httpClient = new DefaultHttpClient(httpParameters);
		HttpPost post = new HttpPost(SEARCH_URL);
		String jsonResponse = null;
		
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("query", PICKUP_QUERY));
			
			if (!TextUtils.isEmpty(startLocation)) {
				nameValuePairs.add(new BasicNameValuePair("ll", startLocation));
			}

			if (!TextUtils.isEmpty(endLocation)) {
				nameValuePairs.add(new BasicNameValuePair("destination", endLocation));
			}
			
			Log.d(TAG, String.format(Locale.getDefault(), "start: %s, end: %s", startLocation, endLocation));
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = httpClient.execute(post);
			jsonResponse = EntityUtils.toString(response.getEntity());
			Log.d(TAG, String.format(Locale.getDefault(), "Response length=%d", jsonResponse.length()));
		} catch (Exception e) {
			Log.e(TAG, String.format(Locale.getDefault(), "Error in doVehicleSearch() - %s", e.getMessage()));
		}
		return jsonResponse;
	}
}
