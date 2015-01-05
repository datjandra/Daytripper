package com.garudasystems.daytripper.view;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.SearchView;
import android.widget.Toast;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.backend.vocifery.SearchResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class SearchActivity extends FragmentActivity 
		implements ActionBar.TabListener, LocationListener {

	public final static String SEARCH_URL = "http://vocifery.com/api/v0/query";
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
	
	private static final String TAG = "SearchActivity";
	private static final String[] TABS = { "List", "Map" };
	
	private ViewPager viewPager;
	private SearchActivityTabAdapter adapter;
	
	private LocationManager locationManager;
	private Location location;
	// default minimum time between new readings
	private long minTime = 5000;
	// default minimum distance between old and new readings.
	private float minDistance = 1000.0f;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        
        initTabs();
        initLocationManager();
        handleIntent(getIntent());
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main_actions, menu);

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) menu.findItem(R.id.action_search)
				.getActionView();
		searchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));
		searchView.setIconifiedByDefault(false);
		return super.onCreateOptionsMenu(menu);
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
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle bundle) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		viewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
		
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
	
	private void initTabs() {
		final ActionBar actionBar = getActionBar();
	    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	    
	    adapter = new SearchActivityTabAdapter(getSupportFragmentManager());
	    viewPager = (ViewPager) findViewById(R.id.container);
	    viewPager.setAdapter(adapter);
	    viewPager.requestTransparentRegion(viewPager);
	    
	    for (String tabName : TABS) {
            actionBar.addTab(actionBar
            	.newTab()
            	.setText(tabName)
            	.setTabListener(this));
        }
	    
	    viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
 
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }
 
            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });
	}
	
	private void initLocationManager() {
		if (locationManager == null) {
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		}
		
		try {
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, this);
		} catch (Exception e) {
			try {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);
			} catch (Exception ex) {
				Log.e(TAG, "initLocationManager - " + ex);
			}
		}
	}
	
	private boolean isConnected() {
		Context context = getApplicationContext();
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return (activeNetworkInfo == null || !activeNetworkInfo.isConnectedOrConnecting());
	}
	
	private long age(Location updatedLocation) {
		return System.currentTimeMillis() - updatedLocation.getTime();
	}
	
	private void handleIntent(Intent intent) {
		try {
			if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
				/*
				if (!isConnected()) {
			    	Toast.makeText(getApplicationContext(), "Network not available", Toast.LENGTH_SHORT).show();
					return;
				}
				*/
				
				String query = intent.getStringExtra(SearchManager.QUERY);
				if (query == null || query.isEmpty()) {
					return;
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
	
	private void refreshMap(List<SearchResult> resultList, SupportMapFragment supportMapFragment) {
		GoogleMap map = supportMapFragment.getMap();
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
			
			if (!resultList.isEmpty()) {
				LatLngBounds bounds = builder.build();
				map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 15));
			}
		}
	}
	
	private Fragment getFragmentByTag(String tag) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
		return fragment;
	}
	
	private static String getFragmentTag(int viewId, int index) {
	     return "android:switcher:" + viewId + ":" + index;
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
			
			String showListFragmentTag = getFragmentTag(R.id.container, SearchActivityTabAdapter.LIST_FRAGMENT_INDEX);
			if (showListFragmentTag != null) {
				Fragment fragment = getFragmentByTag(showListFragmentTag);
				if (fragment != null) {
					((ShowListFragment) fragment).refresh(resultList);
				}
			}
		
			String supportMapFragmentTag = getFragmentTag(R.id.container, SearchActivityTabAdapter.MAP_FRAGMENT_INDEX);
			if (supportMapFragmentTag != null) {
				Fragment fragment = getFragmentByTag(supportMapFragmentTag);
				if (fragment != null) {
					refreshMap(resultList, (SupportMapFragment) fragment);
				}
			}
		}
		
		private List<SearchResult> parseJson(String result) throws JSONException {
			List<SearchResult> resultList = new ArrayList<SearchResult>();
			JSONObject json = new JSONObject(result);
			if (!json.has(ENTITIES_NODE)) {
				return resultList;
			}
			
			String inputFormat = "yyyy-MM-dd'T'HH:mm:ss";
			String outputFormat = "EEE, MMM d h:mm a";
			final SimpleDateFormat inputFormatter = new SimpleDateFormat(inputFormat, Locale.US);
			final SimpleDateFormat outputFormatter = new SimpleDateFormat(outputFormat, Locale.US);
			JSONArray entities = json.getJSONArray(ENTITIES_NODE);
			int numberEntities = entities.length();
			for (int i=0; i<numberEntities; i++) {
				JSONObject entity = entities.getJSONObject(i);
				SearchResult searchResult = new SearchResult();
				
				if (entity.has(NAME_NODE)) {
					searchResult.setName(entity.optString(NAME_NODE));
				}
				
				if (entity.has(URL_NODE)) {
					searchResult.setMobileUrl(entity.optString(URL_NODE));
				}
				
				if (entity.has(LOCATION_NODE)) {
					searchResult.setAddressOne(entity.optString(LOCATION_NODE));
				}
				
				if (entity.has(EXTENDED_ADDRESS_NODE)) {
					searchResult.setAddressTwo(entity.optString(EXTENDED_ADDRESS_NODE));
				}
				
				if (entity.has(DETAILS_NODE)) {
					String details = entity.optString(DETAILS_NODE);
					try {
						searchResult.setDetails(outputFormatter.format(inputFormatter.parse(details)));
					} catch (ParseException e) {
						searchResult.setDetails(details);
					}
				}
				
				if (entity.has(RATING_URL_NODE)) {
					searchResult.setRatingImgUrl(entity.optString(RATING_URL_NODE));
				}
				
				if (entity.has(REVIEW_COUNT_NODE)) {
					searchResult.setReviewCount(entity.optInt(REVIEW_COUNT_NODE));
				}
				
				if (entity.has(COORDINATE_NODE)) {
					JSONObject coordinate = entity.getJSONObject(COORDINATE_NODE);
					searchResult.setLatitude(coordinate.optDouble(LATITUDE_NODE));
					searchResult.setLongitude(coordinate.optDouble(LONGITUDE_NODE));
				}
				
				if (entity.has(IMAGES_NODE)) {
					JSONArray images = entity.getJSONArray(IMAGES_NODE);
					int numberImages = images.length();
					for (int j=0; j<numberImages; j++) {
						if (j == 0) {
							searchResult.setImageOneUrl(images.optString(j));
						} else {
							searchResult.setImageTwoUrl(images.optString(j));
						}
					}
				}
				resultList.add(searchResult);
			}
			return resultList;
		}
	}

	
}
