package com.garudasystems.daytripper.view;

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

import android.app.ActionBar;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.backend.vocifery.QueryResponse;
import com.garudasystems.daytripper.backend.vocifery.Result;
import com.garudasystems.daytripper.components.Refreshable;
import com.garudasystems.daytripper.components.ShowListFragment;
import com.garudasystems.daytripper.components.SimplerExpandableListAdapter;
import com.garudasystems.daytripper.components.ViewPagerFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity implements LocationListener,
		Refreshable, TextToSpeech.OnInitListener {
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

	private static final String TAG = "MainActivity";
	private static final int CONNECTION_TIMEOUT = 7000;
	private static final int SOCKET_TIMEOUT = 7000;
	
	private LocationManager locationManager;
	private Location location;
	private long minTime = 5000;
	private float minDistance = 1000.0f;
	private String cachedQuery;
	private TextToSpeech tts;
	ProgressBar mainProgressBar;
	SearchView searchView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setCustomView(R.layout.actionbar_custom);
        		
		tts = new TextToSpeech(this, this);
        mainProgressBar = (ProgressBar) findViewById(R.id.main_progress);
		
		searchView = (SearchView) findViewById(R.id.search_view);
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String text) {
				return true;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				requestLocationUpdates();
				mainProgressBar.setVisibility(View.VISIBLE);
				
				String showListFragmentTag = getFragmentTag(R.id.viewpager, 
						SearchActivityTabAdapter.LIST_FRAGMENT_INDEX);
				if (showListFragmentTag != null) {
					Fragment fragment = getFragmentByTag(showListFragmentTag);
					if (fragment != null) {
						((ShowListFragment) fragment).reset();
					}
				}
				return false;
			}
		});

		if (savedInstanceState == null) {
			FragmentTransaction transaction = getSupportFragmentManager()
					.beginTransaction();
			ViewPagerFragment fragment = new ViewPagerFragment();
			transaction.replace(R.id.sample_content_fragment, fragment);
			transaction.commit();
		}
		
		requestLocationUpdates();
		location = bestLastKnownLocation();
		handleIntent(getIntent());
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_actions, menu);
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.help_menu) {
			final Context context = this;
			final Dialog dialog = new Dialog(context);
			dialog.setContentView(R.layout.help_content);
			
			TextView helpClose = (TextView) dialog.findViewById(R.id.help_close);
			helpClose.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});
			
			String[] titles = {
				getResources().getString(R.string.seatgeek_instruction),
				getResources().getString(R.string.yelp_instruction),
				getResources().getString(R.string.filter_instruction),
				getResources().getString(R.string.sort_instruction),
				getResources().getString(R.string.similarity_instruction),
			};
			
			String[][] contents = {
				getResources().getStringArray(R.array.seatgeek_examples),
				getResources().getStringArray(R.array.yelp_examples),
				getResources().getStringArray(R.array.filter_examples),
				getResources().getStringArray(R.array.sort_examples),
				getResources().getStringArray(R.array.similarity_examples)
			};
			
			SimplerExpandableListAdapter expandableAdapter = new SimplerExpandableListAdapter(context, titles, contents);
			ExpandableListView helpContent = (ExpandableListView) dialog.findViewById(R.id.help_content);
			helpContent.setAdapter(expandableAdapter);
			dialog.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
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
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle bundle) {
	}

	@Override
	protected void onResume() {
		requestLocationUpdates();
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (locationManager != null) {
			locationManager.removeUpdates(this);
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (tts != null) {
			tts.stop();
			tts.shutdown();
		}
		super.onDestroy();
	}

	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = tts.setLanguage(Locale.US);
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e(TAG, "Language is not available.");
			}
		} else {
			Log.e(TAG, "Could not initialize TextToSpeech.");
		}
	}

	@Override
	public void refresh(int page, int count) {
		String locationString = null;
		if (location != null) {
			locationString = location.getLatitude() + ", "
					+ location.getLongitude();
		}
		Log.i(TAG, "refresh - sending query " + cachedQuery + " with location "
				+ locationString);
		new ParseCommandTask().execute(cachedQuery, locationString,
				Integer.toString(page), Integer.toString(count));
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	private static long age(Location updatedLocation) {
		return System.currentTimeMillis() - updatedLocation.getTime();
	}

	private void requestLocationUpdates() {
		if (locationManager == null) {
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		}

		try {
			locationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, minTime, minDistance,
					this);
		} catch (Exception e) {
			try {
				locationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, minTime, minDistance,
						this);
			} catch (Exception ex) {
				Log.e(TAG, "onCreate - " + ex);
			}
		}
	}

	private Location bestLastKnownLocation() {
		Location updatedLocation = null;
		if (locationManager == null) {
			return null;
		}

		updatedLocation = locationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (updatedLocation == null) {
			updatedLocation = locationManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		}
		return updatedLocation;
	}

	private void handleIntent(Intent intent) {
		try {
			if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
				String query = intent.getStringExtra(SearchManager.QUERY);
				if (query == null || query.isEmpty()) {
					Log.i(TAG, "query is null");
					return;
				}
				
				if (searchView.getQuery() == null || searchView.getQuery().length() == 0) {
					searchView.setQuery(query, false);
					searchView.clearFocus();
				}
				cachedQuery = query;

				if (location == null) {
					location = bestLastKnownLocation();
				}

				String locationString = null;
				if (location != null) {
					locationString = location.getLatitude() + ", "
							+ location.getLongitude();
				}

				Log.i(TAG, "handleIntent - sending query " + query
						+ " with location " + locationString);
				new ParseCommandTask().execute(query, locationString);
			}
		} finally {
			if (locationManager != null) {
				locationManager.removeUpdates(this);
			}
		}
	}

	private Fragment getFragmentByTag(String tag) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
		return fragment;
	}

	private void showToast(final String text, final int length) {
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(getApplicationContext(), text, length).show();
			}
		});
	}

	private void say(final String text) {
		try {
			runOnUiThread(new Runnable() {
				public void run() {
					if (tts != null) {
						tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
					}
				}
			});
		} catch (Exception e) {
		}
	}

	private static String getFragmentTag(int viewId, int index) {
		return "android:switcher:" + viewId + ":" + index;
	}

	private void updateMap(List<Result> resultList,
			SupportMapFragment supportMapFragment, boolean reload) {
		GoogleMap map = supportMapFragment.getMap();
		if (map != null) {
			if (reload) {
				map.clear();
			}
			
			map.setMyLocationEnabled(true);
			LatLngBounds.Builder builder = new LatLngBounds.Builder();
			for (Result result : resultList) {
				Double latitude = result.getLatitude();
				Double longitude = result.getLongitude();
				if (latitude != null && longitude != null) {
					LatLng position = new LatLng(latitude, longitude);
					map.addMarker(new MarkerOptions().position(position).title(
							result.getName()));
					builder.include(position);
				}
			}

			if (!resultList.isEmpty()) {
				LatLngBounds bounds = builder.build();
				map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,
						15));
			}
		}
	}

	private class ParseCommandTask extends
			AsyncTask<String, Void, QueryResponse> {

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
			HttpConnectionParams.setConnectionTimeout(httpParameters, CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(httpParameters, SOCKET_TIMEOUT);
		
			HttpClient httpClient = new DefaultHttpClient(httpParameters);
			HttpPost post = new HttpPost(SEARCH_URL);
			String jsonResponse = null;
			try {
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				nameValuePairs.add(new BasicNameValuePair("query", query));
				if (latLong != null && !latLong.isEmpty()) {
					nameValuePairs.add(new BasicNameValuePair("ll", latLong));
				}

				if (pageStr != null && countStr != null) {
					nameValuePairs.add(new BasicNameValuePair("page", pageStr));
					nameValuePairs
							.add(new BasicNameValuePair("count", countStr));
				}
				post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				Log.d(TAG, EntityUtils.toString(post.getEntity()));
				HttpResponse response = httpClient.execute(post);
				jsonResponse = EntityUtils.toString(response.getEntity());
				queryResponse = parseJson(jsonResponse);
				Log.d(TAG,
						String.format("Response length=%d",
								jsonResponse.length()));
			} catch (Exception e) {
				Log.d(TAG, e.getMessage());
			}
			return queryResponse;
		}

		@Override
		protected void onPostExecute(QueryResponse queryResponse) {
			if (mainProgressBar.isShown()) {
				mainProgressBar.setVisibility(View.INVISIBLE);
			}
			
			if (queryResponse == null || queryResponse.getSource() == null
					|| queryResponse.getTotal() == null
					|| queryResponse.getTotal() <= 0) {
				String message = "Sorry I didn't find anything";
				say(message);
				showToast(message, Toast.LENGTH_SHORT);
				return;
			}

			boolean reload = false;
			Integer page = queryResponse.getPage();
			if (page != null && page <= 1) {
				reload = true;
				String message = String.format(Locale.getDefault(),
						"I found %d listings from %s", queryResponse.getTotal(),
						queryResponse.getSource());
				say(message);
				showToast(message, Toast.LENGTH_SHORT);
			}
			
			String showListFragmentTag = getFragmentTag(R.id.viewpager,
					SearchActivityTabAdapter.LIST_FRAGMENT_INDEX);
			if (showListFragmentTag != null) {
				Fragment fragment = getFragmentByTag(showListFragmentTag);
				if (fragment != null) {
					((ShowListFragment) fragment).refreshList(queryResponse, reload);
				}
			}

			String supportMapFragmentTag = getFragmentTag(R.id.viewpager,
					SearchActivityTabAdapter.MAP_FRAGMENT_INDEX);
			if (supportMapFragmentTag != null) {
				Fragment fragment = getFragmentByTag(supportMapFragmentTag);
				if (fragment != null) {
					updateMap(queryResponse.getResultList(),
							(SupportMapFragment) fragment, reload);
				}
			}
		}

		private QueryResponse parseJson(String jsonResponse)
				throws JSONException {
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
					result.setAddressTwo(entity
							.optString(EXTENDED_ADDRESS_NODE));
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
					JSONObject coordinate = entity
							.getJSONObject(COORDINATE_NODE);
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
