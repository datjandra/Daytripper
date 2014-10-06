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

	public final static String SEARCH_URL = "http://vocifery.com/api/v0/search/yelp";
	
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
			LatLngBounds bounds = builder.build();
			map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 15));
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
			if (!json.has("businesses")) {
				return resultList;
			}
			
			JSONArray businessList = json.getJSONArray("businesses");
			int numberBusinesses = businessList.length();
			for (int i=0; i<numberBusinesses; i++) {
				JSONObject business = businessList.getJSONObject(i);
				SearchResult searchResult = new SearchResult();
				searchResult.setName(business.optString("name"));
				searchResult.setDisplayPhone(business.optString("display_phone"));
				searchResult.setReviewCount(business.getInt("review_count"));
				searchResult.setMobileUrl(business.optString("mobile_url"));
				searchResult.setImageUrl(business.optString("image_url"));
				searchResult.setRatingImgUrlSmall(business.optString("rating_img_url_small"));
				searchResult.setRatingImgUrl(business.optString("rating_img_url"));

				if (!business.has("location")) {
					continue;
				}
				JSONObject location = business.getJSONObject("location");
				
				if (!location.has("display_address")) {
					continue;
				}
				JSONArray displayAddress = location.getJSONArray("display_address");
				
				int addressLength = displayAddress.length();
				for (int j=0; j<addressLength; j++) {
					if (j == 0) {
						searchResult.setAddressOne(displayAddress.optString(j));
					} else {
						searchResult.setAddressTwo(displayAddress.optString(j));
					}
				}
				
				if (!location.has("coordinate")) {
					continue;
				}
				JSONObject coordinate = location.getJSONObject("coordinate");
				searchResult.setLatitude(coordinate.getString("latitude"));
				searchResult.setLongitude(coordinate.getString("longitude"));
				resultList.add(searchResult);
			}
			return resultList;
		}
	}

	
}
