package com.daytripper.app.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

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

import android.text.TextUtils;
import android.util.Log;

public class UberRequestClient implements UberRequestConstants {

	private final static String TAG = "UberRequestClient";
	
	public final static String doPost(UberRequest uberRequest) {
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
}
