package com.vocifery.daytripper.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

public class UberRequestTask extends AsyncTask<Void, Void, String> implements UberRequestConstants {
	
	private UberRequest uberRequest;
	private UberRequestListener uberRequestListener;
	
	private final static String TAG = "UberRequestTask";
	
	UberRequestTask(UberRequest uberRequest, UberRequestListener uberRequestListener) {
		this.uberRequest = uberRequest;
		this.uberRequestListener = uberRequestListener;
	}
	
	@Override
	protected String doInBackground(Void...args) {
		return doPost();
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
	protected void onPostExecute(String response) {
		Log.i(TAG, "onPostExecute()");
		if (response == null) {
			uberRequestListener.onNoResponse();
			return;
		}
	}
	
	private String doPost() {
		String jsonResponse = null;
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			if (!TextUtils.isEmpty(uberRequest.getAccessToken())) {
				nameValuePairs.add(new BasicNameValuePair(PARAM_ACCESS_TOKEN, uberRequest.getAccessToken()));
			}
			
			if (!TextUtils.isEmpty(uberRequest.getCode())) {
				nameValuePairs.add(new BasicNameValuePair(PARAM_CODE, uberRequest.getCode()));
			}
			
			if (!TextUtils.isEmpty(uberRequest.getEndLatitude())) {
				nameValuePairs.add(new BasicNameValuePair(PARAM_END_LATITUDE, uberRequest.getEndLatitude()));
			}
			
			if (!TextUtils.isEmpty(uberRequest.getEndLongitude())) {
				nameValuePairs.add(new BasicNameValuePair(PARAM_END_LONGITUDE, uberRequest.getEndLongitude()));
			}
			
			if (!TextUtils.isEmpty(uberRequest.getProductId())) {
				nameValuePairs.add(new BasicNameValuePair(PARAM_PRODUCT_ID, uberRequest.getProductId()));
			}
			
			if (!TextUtils.isEmpty(uberRequest.getRequestId())) {
				nameValuePairs.add(new BasicNameValuePair(PARAM_REQUEST_ID, uberRequest.getRequestId()));
			}
			
			if (!TextUtils.isEmpty(uberRequest.getStartLatitude())) {
				nameValuePairs.add(new BasicNameValuePair(PARAM_START_LATITUDE, uberRequest.getStartLatitude()));
			}
			
			if (!TextUtils.isEmpty(uberRequest.getStartLongitude())) {
				nameValuePairs.add(new BasicNameValuePair(PARAM_START_LONGITUDE, uberRequest.getStartLongitude()));
			}
			
			if (!TextUtils.isEmpty(uberRequest.getSurgeConfirmationId())) {
				nameValuePairs.add(new BasicNameValuePair(PARAM_SURGE_CONFIRMATION_ID, uberRequest.getSurgeConfirmationId()));
			}
			
			if (!TextUtils.isEmpty(uberRequest.getObject())) {
				nameValuePairs.add(new BasicNameValuePair(PARAM_OBJECT, uberRequest.getObject()));
			}
			
			if (!TextUtils.isEmpty(uberRequest.getVerb())) {
				nameValuePairs.add(new BasicNameValuePair(PARAM_VERB, uberRequest.getVerb()));
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
	
	/*
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
	*/
}