package com.garudasystems.daytripper.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.backend.vocifery.SearchResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class ListResultsActivity extends FragmentActivity implements LocationListener {

	public final static String SEARCH_URL = "http://vocifery.com/api/v0/query";
	private final static List<SearchResult> TEST_DATA = new ArrayList<SearchResult>() {
		private static final long serialVersionUID = 1L;

		{
			add(new SearchResult("Mama's", "1701 Stockton St", "San Francisco, CA 94133", "(415) 362-6421"));
			add(new SearchResult("Sam's Grill", "374 Bush Street", "San Francisco, CA 94104", "415-421-0594"));
			add(new SearchResult("The Slanted Door", "1 Ferry Building, #3", "San Francisco, CA 94111", "(415) 861-8032"));
		}
	};

	private static final String TAG = "ListResultsActivity";
	private static final long TIME_BETWEEN_READINGS = 5 * 60 * 1000;
	
	// default minimum time between new readings
	private long minTime = TIME_BETWEEN_READINGS;

	// default minimum distance between old and new readings.
	private float minDistance = 1000.0f;
	
	private LocationManager locationManager;
	private Location location;
	private SearchResultAdapter adapter;
	
	private ListView listView;
	private GoogleMap map;
	private SupportMapFragment mapFragment;
	
	private final static String ROOT_NODE = "entities";
	private final static String NAME_NODE = "name";
	private final static String DETAILS_NODE = "details";
	private final static String REVIEW_COUNT_NODE = "reviewCount";
	private final static String URL_NODE = "url";
	private final static String RATING_URL_NODE = "ratingUrl";
	private final static String IMAGE_URLS_NODE = "imageUrls";
	private final static String LOCATION_NODE = "location";
	private final static String EXTENDED_ADDRESS_NODE = "extendedAddress";
	private final static String COORDINATE_NODE = "coordinate";
	private final static String LATITUDE_NODE = "latitude";
	private final static String LONGITUDE_NODE = "longitude";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list);
		
		if (locationManager == null) {
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		}
		
		try {
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, this);
		} catch (Exception e) {
			try {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);
			} catch (Exception ex) {
				Log.e(TAG, "onCreate - " + ex);
			}
		}
		handleIntent(getIntent());
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_main_actions, menu);
	    
	    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	    final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
	    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
	    searchView.setIconifiedByDefault(false);
		return true;
	}
	
	@Override
	public void onLocationChanged(Location updatedLocation) {
		if (location == null) {
			location = updatedLocation;
		} else if (updatedLocation != null) {
			if (age(updatedLocation) < age(location)) {
				location = updatedLocation;
			}
		}
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onDestroy() {
		if (adapter != null) {
			adapter.clearAll();
		}
		super.onDestroy();
	}
	
	public void toggleMap(View view) {
		 boolean on = ((ToggleButton) view).isChecked();
		 if (on) {
			 if (listView != null) {		
				 listView.setVisibility(View.GONE);
			 }
			 if (mapFragment != null) {
				 mapFragment.getView().setVisibility(View.VISIBLE);
			 }	 
		 } else {
			 if (listView != null) {		
				 listView.setVisibility(View.VISIBLE);
			 }
			 if (mapFragment != null) {
				 mapFragment.getView().setVisibility(View.GONE);
			 }
		 }
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (locationManager == null) {
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		}
		
		location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (location == null) {
			location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		}
		
		try {
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, this);
		} catch (Exception e) {
			try {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);
			} catch (Exception ex) {
				Log.e(TAG, "onCreate - " + ex);
			}
		}
	}
	
	@Override
	protected void onPause() {
		// unregister for location updates
		if (locationManager != null) {
			locationManager.removeUpdates(this);
		}
		super.onPause();
	}
	
	@Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (locationManager == null) {
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		}
        handleIntent(intent);
    }
	
	private void initializeMap(List<SearchResult> resultList) {
		if (resultList == null || resultList.isEmpty()) {
			return;
		}
		
		mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment));
		map = mapFragment.getMap();
		if (map != null) {
			map.setMyLocationEnabled(true);		
			LatLngBounds.Builder builder = new LatLngBounds.Builder();
			for (SearchResult result : resultList) {
				Double latitude = result.getLatitude();
				Double longitude = result.getLongitude();
				if (latitude != null && longitude != null) {
					LatLng position = new LatLng(latitude, longitude);
					map.addMarker(new MarkerOptions().position(position).title(result.getName()));
					builder.include(position);
				}
			}
			LatLngBounds bounds = builder.build();
			map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 15));
		}
	}
	
	private void handleIntent(Intent intent) {
		try {
			if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
				checkConnection();
				String query = intent.getStringExtra(SearchManager.QUERY);
				if (query == null || query.isEmpty()) {
					return;
				}

				if (mapFragment != null && mapFragment.getView().getVisibility() == View.VISIBLE) {
					mapFragment.getView().setVisibility(View.GONE);
					listView.setVisibility(View.VISIBLE);
				}
				
				String locationString = null;
				if (location != null) {
					locationString = location.getLatitude() + ", "
							+ location.getLongitude();
				}
				Log.i(TAG, "handleIntent - sending query " + query + " with location " + locationString);
				new ParseCommandTask().execute(query, locationString);
			}
		} finally {
			if (locationManager != null) {
				locationManager.removeUpdates(this);
			}
		}
	}
	
	private void checkConnection() {
		Context context = getApplicationContext();
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    if (activeNetworkInfo == null || !activeNetworkInfo.isConnectedOrConnecting()) {
	    	Toast.makeText(context, "Network not available", Toast.LENGTH_SHORT).show();
	    }
	}
	
	private long age(Location updatedLocation) {
		return System.currentTimeMillis() - updatedLocation.getTime();
	}
	
	private Context getContext() {
		return this;
	}
	
	private class ParseCommandTask extends AsyncTask<String, Void, List<SearchResult>> {

		@Override
		protected List<SearchResult> doInBackground(String... params) {
			List<SearchResult> resultList = null;
			String query = params[0];
			String latLong = params[1];
			
			HttpClient httpClient = new DefaultHttpClient();
			HttpPost post = new HttpPost(SEARCH_URL);
			String jsonResponse = null;
			try {
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		        nameValuePairs.add(new BasicNameValuePair("query", query));
		        if (latLong != null && !latLong.isEmpty()) {
		        	nameValuePairs.add(new BasicNameValuePair("ll", latLong));
		        }
		        post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				
		        Log.d(TAG, EntityUtils.toString(post.getEntity()));
		        HttpResponse response = httpClient.execute(post);
		        jsonResponse = EntityUtils.toString(response.getEntity());
		        Log.d(TAG, jsonResponse);
				resultList = parseJson(jsonResponse);
			} catch (HttpResponseException e) {
				Log.e(TAG, "doInBackground - " + e);
			} catch (IOException e) {
				Log.e(TAG, "doInBackground - " + e);
			} catch (JSONException e) {
				Log.e(TAG, "doInBackground - " + e);
			} 
			return resultList;
		}
		
		@Override
		protected void onPostExecute(List<SearchResult> resultList) {
			if (resultList == null || resultList.isEmpty()) {
				Toast.makeText(getApplicationContext(), "No results found", Toast.LENGTH_SHORT).show();
				return;
			}
			
			initializeMap(resultList);
			adapter = new SearchResultAdapter(getContext(), resultList);
			
	 	    listView = (ListView) findViewById(R.id.list);
	 	    listView.setAdapter(adapter);
		}
		
		private List<SearchResult> parseJson(String result) throws JSONException {
			List<SearchResult> resultList = new ArrayList<SearchResult>();
			JSONObject json = new JSONObject(result);
			if (!json.has(ROOT_NODE)) {
				return resultList;
			}
			
			JSONArray entryList = json.getJSONArray(ROOT_NODE);
			int numberEntries = entryList.length();
			for (int i=0; i<numberEntries; i++) {
				JSONObject entry = entryList.getJSONObject(i);
				SearchResult searchResult = new SearchResult();
				
				if (entry.has(NAME_NODE)) {
					searchResult.setName(entry.optString(NAME_NODE));
				}
				
				if (entry.has(URL_NODE)) {
					searchResult.setMobileUrl(entry.optString(URL_NODE));
				}
				
				if (entry.has(LOCATION_NODE)) {
					searchResult.setAddressOne(entry.optString(LOCATION_NODE));
				}
				
				if (entry.has(EXTENDED_ADDRESS_NODE)) {
					searchResult.setAddressTwo(entry.optString(EXTENDED_ADDRESS_NODE));
				}
				
				if (entry.has(DETAILS_NODE)) {
					searchResult.setDetails(entry.optString(DETAILS_NODE));
				}
				
				if (entry.has(RATING_URL_NODE)) {
					searchResult.setRatingImgUrlSmall(entry.optString(RATING_URL_NODE));
				}
				
				if (entry.has(REVIEW_COUNT_NODE)) {
					searchResult.setReviewCount(entry.getInt(REVIEW_COUNT_NODE));
				}
				
				if (entry.has(IMAGE_URLS_NODE)) {
					JSONArray imageUrls = entry.getJSONArray(IMAGE_URLS_NODE);
					int numberImages = imageUrls.length();
					for (int j=0; j<numberImages; j++) {
						if (j == 0) {
							searchResult.setImageOneUrl(imageUrls.optString(j));
						} else {
							searchResult.setImageTwoUrl(imageUrls.optString(j));
						}
					}
				}
				
				if (entry.has(COORDINATE_NODE)) {
					JSONObject coordinate = entry.getJSONObject(COORDINATE_NODE);
					searchResult.setLatitude(coordinate.optDouble(LATITUDE_NODE));
					searchResult.setLongitude(coordinate.optDouble(LONGITUDE_NODE));
				}
				resultList.add(searchResult);
			}
			return resultList;
		}
	}
}
