package com.vocifery.daytripper.service;

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

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class DefaultAction implements Actionable {
	
	public final static String RESOURCE_NODE = "resource";
	public final static String PAGE_NODE = "page";
	public final static String COUNT_NODE = "count";
	public final static String TOTAL_NODE = "total";
	public final static String ENTITIES_NODE = "entities";
	public final static String NAME_NODE = "name";
	public final static String URL_NODE = "url";
	public final static String DESC_NODE = "descriptor";
	public final static String EXTENDED_DESC_NODE = "extendedDescriptor";
	public final static String DETAILS_NODE = "details";
	public final static String RATING_URL_NODE = "ratingUrl";
	public final static String REVIEW_COUNT_NODE = "reviewCount";
	public final static String COORDINATE_NODE = "coordinate";
	public final static String LONGITUDE_NODE = "longitude";
	public final static String LATITUDE_NODE = "latitude";
	public final static String IMAGES_NODE = "imageUrls";
	public final static String UTCDATE_NODE = "utcDate";

	private final static String TAG = "DefaultAction";
	
	@Override
	public String getSource() {
		return this.getClass().getName();
	}
	
	@Override
	public String getVerb() {
		return null;
	}
	
	@Override
	public String getObject() {
		return null;
	}
	
	@Override
	public String getCustomMessage() {
		return null;
	}
	
	@Override
	public String doActionWithIntent(Intent intent) {
		String jsonResponse = null;
		String query = intent.getStringExtra(ResponderService.KEY_QUERY);
		String locationString = intent.getStringExtra(ResponderService.KEY_lOCATION);
		String destinationString = intent.getStringExtra(ResponderService.KEY_DESTINATION);
		Integer page = intent.getIntExtra(ResponderService.KEY_PAGE, 0);
		Integer count = intent.getIntExtra(ResponderService.KEY_COUNT, 20);
		
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters,
				CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParameters, SOCKET_TIMEOUT);

		HttpClient httpClient = new DefaultHttpClient(httpParameters);
		HttpPost post = new HttpPost(SEARCH_URL);
		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("query", query));
			if (!TextUtils.isEmpty(locationString)) {
				nameValuePairs.add(new BasicNameValuePair("ll", locationString));
			}

			if (!TextUtils.isEmpty(destinationString)) {
				nameValuePairs.add(new BasicNameValuePair("destination", destinationString));
			}
			
			if (page != null && page > 0) {
				nameValuePairs.add(new BasicNameValuePair("page", page.toString()));
			}
			
			if (count != null && count > 0) {
				nameValuePairs.add(new BasicNameValuePair("count", count.toString()));
			}
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			Log.d(TAG, EntityUtils.toString(post.getEntity()));
			HttpResponse response = httpClient.execute(post);
			jsonResponse = EntityUtils.toString(response.getEntity());
			Log.d(TAG,
					String.format("Response length=%d", jsonResponse.length()));
		} catch (Exception e) {
			Log.e(TAG, String.format("Error in doActionWithIntent() - %s", e.getMessage()));
		}
		return jsonResponse;
	}
}
