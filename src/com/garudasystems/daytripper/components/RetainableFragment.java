package com.garudasystems.daytripper.components;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.backend.vocifery.QueryResponse;
import com.garudasystems.daytripper.backend.vocifery.Result;

public class RetainableFragment extends Fragment {

	public static final String TAG = "RetainableFragment";
	
	private ParseCommandTask parseCommandTask;
	
	/**
     * Fragment initialization.  We want to be retained and
     * start our thread.
     */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
    
    @Override
    public void onAttach(Activity activity) {
      super.onAttach(activity);
      if (parseCommandTask != null && activity instanceof Refreshable) {
    	  parseCommandTask.setRefreshable((Refreshable) activity);
      }
    }
    
    /**
     * This is called when the fragment is going away.  It is NOT called
     * when the fragment is being propagated between activity instances.
     */
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	if (parseCommandTask != null && !parseCommandTask.isCancelled()) {
    		parseCommandTask.cancel(true);
    	}
    }
    
    /**
     * This is called right before the fragment is detached from its
     * current activity instance.
     */
    @Override
    public void onDetach() {
    	super.onDetach();
    	if (parseCommandTask != null) {
    		parseCommandTask.setRefreshable(null);
    	}
    }
    
    public void startWork(Refreshable refreshable, String query, String locationString, int page, int count) {		
    	if (parseCommandTask != null && parseCommandTask.getStatus() != AsyncTask.Status.FINISHED) {
    		refreshable.requestDenied(getActivity().getResources().getString(R.string.request_denied_message));
    		return;
    	}
    	
		parseCommandTask = new ParseCommandTask(refreshable);
		if (page > 0 && count > 0) {
			parseCommandTask.execute(query, locationString, Integer.toString(page), Integer.toString(count));
		} else {
			parseCommandTask.execute(query, locationString);
		}
	}
	
    
    private final static class ParseCommandTask extends AsyncTask<String, Void, QueryResponse> {

    	public final static String SEARCH_URL = "http://vocifery.com/api/v0/query";
    	public final static String RESOURCE_NODE = "resource";
    	public final static String PAGE_NODE = "page";
    	public final static String COUNT_NODE = "count";
    	public final static String TOTAL_NODE = "total";
    	public final static String ENTITIES_NODE = "entities";
    	public final static String NAME_NODE = "name";
    	public final static String URL_NODE = "url";
    	public final static String LOCATION_NODE = "location";
    	public final static String EXTENDED_ADDRESS_NODE = "extendedAddress";
    	public final static String DETAILS_NODE = "details";
    	public final static String RATING_URL_NODE = "ratingUrl";
    	public final static String REVIEW_COUNT_NODE = "reviewCount";
    	public final static String COORDINATE_NODE = "coordinate";
    	public final static String LONGITUDE_NODE = "longitude";
    	public final static String LATITUDE_NODE = "latitude";
    	public final static String IMAGES_NODE = "imageUrls";

    	private static final int CONNECTION_TIMEOUT = 7000;
    	private static final int SOCKET_TIMEOUT = 7000;

    	private Refreshable refreshable;

    	public ParseCommandTask(Refreshable refreshable) {
    		this.refreshable = refreshable;
    	}
    	
    	public synchronized void setRefreshable(Refreshable refreshable) {
    		this.refreshable = refreshable;
    	}

    	@Override
    	protected QueryResponse doInBackground(String... params) {
    		QueryResponse queryResponse = null;
    		String query = params[0];
    		String latLong = params[1];

    		String pageStr = null;
    		String countStr = null;
    		if (params.length >= 4) {
    			pageStr = params[2];
    			countStr = params[3];
    		}

    		HttpParams httpParameters = new BasicHttpParams();
    		HttpConnectionParams.setConnectionTimeout(httpParameters,
    				CONNECTION_TIMEOUT);
    		HttpConnectionParams.setSoTimeout(httpParameters, SOCKET_TIMEOUT);

    		HttpClient httpClient = new DefaultHttpClient(httpParameters);
    		HttpPost post = new HttpPost(SEARCH_URL);
    		String jsonResponse = null;
    		try {
    			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
    			nameValuePairs.add(new BasicNameValuePair("query", query));
    			if (latLong != null && !latLong.isEmpty()) {
    				nameValuePairs.add(new BasicNameValuePair("ll", latLong));
    			}

    			if (pageStr != null && countStr != null) {
    				nameValuePairs.add(new BasicNameValuePair("page", pageStr));
    				nameValuePairs.add(new BasicNameValuePair("count", countStr));
    			}
    			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

    			Log.d(TAG, EntityUtils.toString(post.getEntity()));
    			HttpResponse response = httpClient.execute(post);
    			jsonResponse = EntityUtils.toString(response.getEntity());
    			queryResponse = parseJson(jsonResponse);
    			Log.d(TAG,
    					String.format("Response length=%d", jsonResponse.length()));
    		} catch (Exception e) {
    			Log.e(TAG, String.format("Error in doInBackground() - %s", e.getMessage()));
    		}
    		return queryResponse;
    	}

    	@Override
    	protected synchronized void onPostExecute(QueryResponse response) {
    		if (refreshable != null) {
    			refreshable.receivedResponse(response);
    		}
    	}

    	private QueryResponse parseJson(String jsonResponse) throws JSONException {
    		JSONObject json = new JSONObject(jsonResponse);
    		QueryResponse response = new QueryResponse();
    		if (json.has(RESOURCE_NODE)) {
    			response.setSource(json.getString(RESOURCE_NODE));
    		}

    		if (json.has(PAGE_NODE)) {
    			response.setPage(json.getInt(PAGE_NODE));
    		}

    		if (json.has(COUNT_NODE)) {
    			response.setChunk(json.getInt(COUNT_NODE));
    		}

    		if (json.has(TOTAL_NODE)) {
    			response.setTotal(json.getInt(TOTAL_NODE));
    		}

    		List<Result> resultList = new ArrayList<Result>();
    		if (!json.has(ENTITIES_NODE)) {
    			response.setResultList(resultList);
    			return response;
    		}

    		String inputFormat = "yyyy-MM-dd'T'HH:mm:ss";
    		String outputFormat = "EEE, MMM d h:mm a";
    		final SimpleDateFormat inputFormatter = new SimpleDateFormat(
    				inputFormat, Locale.US);
    		final SimpleDateFormat outputFormatter = new SimpleDateFormat(
    				outputFormat, Locale.US);
    		JSONArray entities = json.getJSONArray(ENTITIES_NODE);
    		int numberEntities = entities.length();
    		for (int i = 0; i < numberEntities; i++) {
    			JSONObject entity = entities.getJSONObject(i);
    			Result result = new Result();

    			if (entity.has(NAME_NODE)) {
    				result.setName(entity.optString(NAME_NODE));
    			}

    			if (entity.has(URL_NODE)) {
    				result.setMobileUrl(entity.optString(URL_NODE));
    			}

    			if (entity.has(LOCATION_NODE)) {
    				result.setAddressOne(entity.optString(LOCATION_NODE));
    			}

    			if (entity.has(EXTENDED_ADDRESS_NODE)) {
    				result.setAddressTwo(entity.optString(EXTENDED_ADDRESS_NODE));
    			}

    			if (entity.has(DETAILS_NODE)) {
    				String details = entity.optString(DETAILS_NODE);
    				try {
    					result.setDetails(outputFormatter.format(inputFormatter
    							.parse(details)));
    				} catch (ParseException e) {
    					result.setDetails(details);
    				}
    			}

    			if (entity.has(RATING_URL_NODE)) {
    				result.setRatingImgUrl(entity.optString(RATING_URL_NODE));
    			}

    			if (entity.has(REVIEW_COUNT_NODE)) {
    				result.setReviewCount(entity.optInt(REVIEW_COUNT_NODE));
    			}

    			if (entity.has(COORDINATE_NODE)) {
    				JSONObject coordinate = entity.getJSONObject(COORDINATE_NODE);
    				result.setLatitude(coordinate.optDouble(LATITUDE_NODE));
    				result.setLongitude(coordinate.optDouble(LONGITUDE_NODE));
    			}

    			if (entity.has(IMAGES_NODE)) {
    				JSONArray images = entity.getJSONArray(IMAGES_NODE);
    				int numberImages = images.length();
    				for (int j = 0; j < numberImages; j++) {
    					if (j == 0) {
    						result.setImageOneUrl(images.optString(j));
    					} else {
    						result.setImageTwoUrl(images.optString(j));
    					}
    				}
    			}
    			resultList.add(result);
    		}
    		response.setResultList(resultList);
    		return response;
    	}
    }

}
