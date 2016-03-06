package com.vocifery.daytripper.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.vocifery.daytripper.model.Locatable;
import com.vocifery.daytripper.model.LocatableItem;
import com.vocifery.daytripper.model.QueryResponse;
import com.vocifery.daytripper.model.Result;

public abstract class QueryResponseConverter {

	public final static String MESSAGE_NODE = "message";
	public final static String ID_NODE = "id";
	public final static String SOURCE_NODE = "source";
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
	public final static String CENTER_NODE = "center";
	public final static String DESTINATION_NODE = "destination";
	public final static String INTENT_NODE = "intent";
	public final static String CATEGORIES_NODE = "categories";
	
	public final static String EXTRA_PRODUCT_ID = "product_id";
	public final static String EXTRA_START_LATITUDE = "start_latitude";
	public final static String EXTRA_START_LONGITUDE = "start_longitude";
	public final static String EXTRA_END_LATITUDE = "end_latitude";
	public final static String EXTRA_END_LONGITUDE = "end_longitude";
	
	private static final String TAG = "QueryResponseConverter";
	
	public final static QueryResponse parseJson(String jsonResponse) throws JSONException {
		JSONObject json = new JSONObject(jsonResponse);
		QueryResponse response = new QueryResponse();
		if (json.has(MESSAGE_NODE)) {
			response.setMessage(json.getString(MESSAGE_NODE));
		}
		
		if (json.has(SOURCE_NODE)) {
			response.setSource(json.getString(SOURCE_NODE));
		}
		
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

		if (json.has(INTENT_NODE)) {
			response.setIntent(json.getString(INTENT_NODE));
		}
		
		LocatableItem start = null;
		if (!json.isNull(CENTER_NODE) && json.has(CENTER_NODE)) {
			JSONObject center = json.getJSONObject(CENTER_NODE);
			Double lat = center.optDouble(LATITUDE_NODE);
			Double lon = center.optDouble(LONGITUDE_NODE);
			
			start = new LocatableItem();
			start.setName("Pick-up");
			start.setDetails(String.format(Locale.getDefault(),  "%.3f, %.3f", lat, lon));
			start.setLatitude(lat);
			start.setLongitude(lon);
		}
		
		LocatableItem end = null;
		if (!json.isNull(DESTINATION_NODE) && json.has(DESTINATION_NODE)) {
			JSONObject destination = json.getJSONObject(DESTINATION_NODE);
			Double lat = destination.optDouble(LATITUDE_NODE);
			Double lon = destination.optDouble(LONGITUDE_NODE);
			
			end = new LocatableItem();
			end.setName("Drop-off");
			end.setDetails(String.format(Locale.getDefault(),  "%.3f, %.3f", lat, lon));
			end.setLatitude(lat);
			end.setLongitude(lon);
		}
		
		if (start != null && end != null) {
			List<Locatable> route = new ArrayList<Locatable>();
			route.add(start);
			route.add(end);
			response.setRoute(route);
		}
		
		List<Result> resultList = new ArrayList<Result>();
		if (!json.has(ENTITIES_NODE)) {
			response.setResultList(resultList);
			return response;
		}

		String inputFormat = "yyyy-MM-dd'T'HH:mm:ss";
		String outputFormat = "EEE, MMM d h:mm a z";
		final SimpleDateFormat inputFormatter = new SimpleDateFormat(inputFormat, Locale.getDefault());
		inputFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		final SimpleDateFormat outputFormatter = new SimpleDateFormat(
				outputFormat, Locale.getDefault());
		JSONArray entities = json.getJSONArray(ENTITIES_NODE);
		int numberEntities = entities.length();
		for (int i = 0; i < numberEntities; i++) {
			JSONObject entity = entities.getJSONObject(i);
			Result result = new Result();
			if (entity.has(ID_NODE)) {
				result.setId(entity.optString(ID_NODE));
			}

			if (entity.has(NAME_NODE)) {
				result.setName(entity.optString(NAME_NODE));
			}

			if (entity.has(URL_NODE)) {
				result.setMobileUrl(entity.optString(URL_NODE));
			}

			if (!entity.isNull(DESC_NODE)) {
				result.setAddressOne(entity.optString(DESC_NODE));
			}

			if (!entity.isNull(EXTENDED_DESC_NODE)) {
				result.setAddressTwo(entity.optString(EXTENDED_DESC_NODE));
			}
			
			if (!entity.isNull(DETAILS_NODE)) {
				result.setDetails(entity.optString(DETAILS_NODE));
			} else if (!entity.isNull(UTCDATE_NODE)) {
				try {
					result.setDetails(outputFormatter.format(inputFormatter
							.parse(entity.getString(UTCDATE_NODE))));
				} catch (ParseException e) {}
			}

			if (entity.has(RATING_URL_NODE)) {
				result.setRatingImgUrl(entity.optString(RATING_URL_NODE));
			}

			if (entity.has(REVIEW_COUNT_NODE)) {
				result.setReviewCount(entity.optInt(REVIEW_COUNT_NODE));
			}

			if (!entity.isNull(COORDINATE_NODE) && entity.has(COORDINATE_NODE)) {
				JSONObject coordinate = entity.getJSONObject(COORDINATE_NODE);
				result.setLatitude(coordinate.optDouble(LATITUDE_NODE));
				result.setLongitude(coordinate.optDouble(LONGITUDE_NODE));
			}

			if (!entity.isNull(IMAGES_NODE) && entity.has(IMAGES_NODE)) {
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
			
			if (!entity.isNull(CATEGORIES_NODE) && entity.has(CATEGORIES_NODE)) {
				JSONArray categories = entity.getJSONArray(CATEGORIES_NODE);
				int numberCategories = categories.length();
				for (int j=0; j<numberCategories; j++) {
					String category = categories.optString(j);
					if (!TextUtils.isEmpty(category)) {
						response.addCategory(category);
					}
				}
			}
			resultList.add(result);
		}
		
		Log.i(TAG, "number of results = " + resultList.size());
		response.setResultList(resultList);
		return response;
	}
}
