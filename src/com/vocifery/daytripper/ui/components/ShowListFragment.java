package com.vocifery.daytripper.ui.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vocifery.daytripper.R;
import com.vocifery.daytripper.model.Locatable;
import com.vocifery.daytripper.model.QueryResponse;
import com.vocifery.daytripper.model.Result;
import com.vocifery.daytripper.model.Searchable;
import com.vocifery.daytripper.service.RequestConstants;
import com.vocifery.daytripper.ui.Daytripper;
import com.vocifery.daytripper.util.ImageLoader;
import com.vocifery.daytripper.util.StringConstants;

public class ShowListFragment extends Fragment implements
		AbsListView.OnScrollListener, RequestConstants, StringConstants {

	private ListView listView;
	private ImageView attributionLogo;
	private ArrayList<Searchable> allItems;
	private SearchResultAdapter adapter;
	private String source;
	private int currentPage;
	private int total;
	private int chunkSize;
	private int previousLastItem = 0;
	private Refreshable refreshable;
	private Map<String, String> sourceLogos;
	private ImageLoader imageLoader;
	private Parcelable listInstanceState;
	private ArrayList<Locatable> route;
	private String intent;
	
	public static final String RESULT_LIST_STATE = ShowListFragment.class.getName() + "." + Result.class.getName();
	public static final String LIST_INSTANCE_STATE = "ListInstanceState";
	public static final String CURRENT_PAGE_STATE = "CurrentPageState";
	public static final String CHUNK_SIZE_STATE = "ChunkSizeState";
	public static final String TOTAL_STATE = "TotalState";
	public static final String PREVIOUS_LAST_ITEM_STATE = "PreviousLastItemState";
	public static final String SOURCE_STATE = "SourceState";
	public static final String ROUTE_STATE = "RouteState";
	public static final String INTENT_STATE = "IntentState";
	public static final String TAG = "ShowListFragment";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		imageLoader = ImageLoader.getInstance(Daytripper.getAppContext());
		imageLoader.addImageCache(getFragmentManager());
		
		sourceLogos = new HashMap<String, String>();
		final String[] vendors = getResources().getStringArray(
				R.array.vendor_array);
		final String[] logos = getResources()
				.getStringArray(R.array.logo_array);
		for (int i = 0; i < vendors.length; i++) {
			sourceLogos.put(vendors[i], logos[i]);
		}
		
		if (savedInstanceState != null) {
			ArrayList<Result> resultList = savedInstanceState.getParcelableArrayList(RESULT_LIST_STATE);
			if (resultList != null) {
				if (allItems == null) {
					allItems = new ArrayList<Searchable>();
				}
				allItems.addAll(resultList);
				
				final Daytripper daytripper = ((Daytripper) getActivity().getApplicationContext());
				daytripper.setAllItems(allItems);
			}
			
			ArrayList<Locatable> locatablelist = savedInstanceState.getParcelableArrayList(ROUTE_STATE);
			if (route == null) {
				if (route == null) {
					route = new ArrayList<Locatable>();
				}
				route.addAll(locatablelist);
			}
			
			listInstanceState = savedInstanceState.getParcelable(LIST_INSTANCE_STATE);
			chunkSize = savedInstanceState.getInt(CHUNK_SIZE_STATE, 0);
			currentPage = savedInstanceState.getInt(CURRENT_PAGE_STATE, 0);
			total = savedInstanceState.getInt(TOTAL_STATE, 0);
			previousLastItem = savedInstanceState.getInt(PREVIOUS_LAST_ITEM_STATE, 0);
			source = savedInstanceState.getString(SOURCE_STATE);
			intent = savedInstanceState.getString(INTENT_STATE);
		} else {
			allItems = new ArrayList<Searchable>();
			route = new ArrayList<Locatable>();
			
			final Daytripper daytripper = ((Daytripper) getActivity().getApplicationContext());
			daytripper.setAllItems(allItems);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.show_list_fragment,
				container, false);
		listView = (ListView) rootView.findViewById(R.id.list);
		listView.setOnScrollListener(this);
		
		attributionLogo = (ImageView) rootView
				.findViewById(R.id.attribution_logo);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Activity activity = getActivity();
		adapter = new SearchResultAdapter(activity, intent, route);
		listView.setAdapter(adapter);
		
		if (listInstanceState != null) {
			listView.onRestoreInstanceState(listInstanceState);
		}
		
		if (source != null && sourceLogos.containsKey(source)) {
			String uri = "@drawable/" + sourceLogos.get(source);
			int imageResource = getResources().getIdentifier(uri,
					"drawable", getActivity().getPackageName());
			if (imageResource > 0) {
				attributionLogo.setImageResource(imageResource);
			}
		}
		
		if (activity instanceof Refreshable) {
			this.setRefreshable((Refreshable) activity);
		}
	}
	
	@Override
	public void onDestroy() {
		imageLoader.clearCache();
		this.refreshable = null;
		final Daytripper daytripper = ((Daytripper) getActivity().getApplicationContext());
		daytripper.setAllItems(null);
		super.onDestroy();
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedState) {
	    super.onSaveInstanceState(savedState);
	    savedState.putInt(CURRENT_PAGE_STATE, currentPage);
	    savedState.putInt(CHUNK_SIZE_STATE, chunkSize);
	    savedState.putInt(PREVIOUS_LAST_ITEM_STATE, previousLastItem);
	    savedState.putInt(TOTAL_STATE, total);
	    savedState.putString(SOURCE_STATE, source);
	    savedState.putParcelableArrayList(RESULT_LIST_STATE, allItems);
	    savedState.putParcelableArrayList(ROUTE_STATE, route);
	    savedState.putParcelable(LIST_INSTANCE_STATE, listView.onSaveInstanceState());
	    savedState.putString(INTENT_STATE, intent);
	}

	public void setRefreshable(Refreshable refreshable) {
		this.refreshable = refreshable;
	}
	
	public void refreshList(QueryResponse queryResponse, boolean reload) {
		if (queryResponse != null && queryResponse.getTotal() > 0) {
			this.source = queryResponse.getSource();
			this.currentPage = queryResponse.getPage();
			this.total = queryResponse.getTotal();
			this.chunkSize = queryResponse.getChunk();
			this.intent = queryResponse.getIntent();
			
			Log.i(TAG, "page = " + this.currentPage);
			Log.i(TAG, "count = " + this.chunkSize);
			Log.i(TAG, "total = " + this.total);
			
			if (source != null && sourceLogos.containsKey(source)) {
				String uri = "@drawable/" + sourceLogos.get(source);
				int imageResource = getResources().getIdentifier(uri,
						"drawable", getActivity().getPackageName());
				if (imageResource > 0) {
					attributionLogo.setImageResource(imageResource);
				}
			}
		}
		
		List<Locatable> locatableList = queryResponse.getRoute();
		if (locatableList != null && !locatableList.isEmpty()) {
			if (reload) {
				route.clear();
			}
			route.addAll(locatableList);
			adapter.updateRoute(route);
		}
		adapter.updateIntent(intent);

		List<Result> resultList = queryResponse.getResultList();
		if (resultList != null && !resultList.isEmpty()) {
			if (reload) {
				allItems.clear();
				this.previousLastItem = 0;
			}
			allItems.addAll(resultList);
			adapter.notifyDataSetChanged();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.AbsListView.OnScrollListener#onScroll(android.widget.
	 * AbsListView, int, int, int)
	 */
	@Override
	public void onScroll(AbsListView absListView, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		int lastItem = firstVisibleItem + visibleItemCount;
		if (lastItem == totalItemCount) {
			if (previousLastItem != lastItem) {
				if ((total - totalItemCount) > 0) {
					currentPage = Math.max(currentPage, 1);
					Log.d(TAG,
							String.format("Loading page %d", currentPage + 1));
					if (refreshable != null) {
						refreshable.refresh(currentPage + 1, chunkSize);
					}
				}
				previousLastItem = lastItem;
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {}

	public void reset() {
		this.source = null;
		this.currentPage = 0;
		this.total = 0;
		this.chunkSize = 0;
		this.previousLastItem = 0;
		this.listInstanceState = null;
		this.attributionLogo.setImageDrawable(null);
		this.allItems = new ArrayList<Searchable>();
		this.route = new ArrayList<Locatable>();
		this.intent = null;
		this.adapter = new SearchResultAdapter(getActivity(), this.intent, this.route);
		this.listView.setAdapter(adapter);
		
		final Daytripper daytripper = ((Daytripper) getActivity().getApplicationContext());
		daytripper.setAllItems(allItems);
	}
	
	public ArrayList<Searchable> getAllItems() {
		return allItems;
	}
	
	private class ViewHolder {
		TextView name;
		TextView reviews;
		TextView listIndex;
		TextView snippet;
		ImageView photoOne;
		ImageView photoTwo;
		ImageView ratingImage;
		LinearLayout ratingsContainer;
		RelativeLayout imagesContainer;
		RelativeLayout addressContainer;
	}
	
	private class SearchResultAdapter extends ArrayAdapter<Searchable> {
		
		private String intent;
		private List<Locatable> route;
		
		public SearchResultAdapter(Context context, String intent, List<Locatable> route) {
			super(context, R.layout.list_row, allItems);
			this.intent = intent;
			this.route = route;
		}
		
		public void updateRoute(List<Locatable> route) {
			this.route = route;
		}
		
		public void updateIntent(String intent) {
			this.intent = intent;
		}
		
		@Override
        public int getCount() {
			if (allItems != null) {
				return allItems.size();
			}
			return 0;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			ViewHolder holder = null;
			
			if (row == null) {
				row = LayoutInflater.from(getContext()).inflate(R.layout.list_row, parent, false);
				holder = new ViewHolder();
				holder.name = (TextView) row.findViewById(R.id.name);
				holder.name.setClickable(true);
				holder.name.setMovementMethod(LinkMovementMethod.getInstance());
				
				holder.photoOne = (ImageView) row.findViewById(R.id.photo_one);
				holder.photoTwo = (ImageView) row.findViewById(R.id.photo_two);
				holder.ratingImage = (ImageView) row.findViewById(R.id.rating_img);
				holder.reviews = (TextView) row.findViewById(R.id.rating_count);
				holder.snippet = (TextView) row.findViewById(R.id.snippet);
				holder.listIndex = (TextView) row.findViewById(R.id.list_index);
				holder.ratingsContainer = (LinearLayout) row.findViewById(R.id.ratingsContainer);
				holder.imagesContainer = (RelativeLayout) row.findViewById(R.id.images);
				holder.addressContainer = (RelativeLayout) row.findViewById(R.id.address);
				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}
			
			final Searchable result = getItem(position);
			String imageOneUrl = result.getImageOneUrl();
			if (imageOneUrl != null && !imageOneUrl.equals("null")) {
				imageLoader.loadImage(imageOneUrl, holder.photoOne);
				holder.photoOne.setVisibility(View.VISIBLE);
				((LinearLayout.LayoutParams) holder.imagesContainer.getLayoutParams()).weight = 0.25f;
				((LinearLayout.LayoutParams) holder.addressContainer.getLayoutParams()).weight = 0.75f;
			} else {
				holder.photoOne.setVisibility(View.GONE);
				((LinearLayout.LayoutParams) holder.imagesContainer.getLayoutParams()).weight = 0f;
				((LinearLayout.LayoutParams) holder.addressContainer.getLayoutParams()).weight = 1f;
			}
			
			String imageTwoUrl = result.getImageTwoUrl();
			if (imageTwoUrl != null && !imageTwoUrl.equals("null")) {
				imageLoader.loadImage(imageTwoUrl, holder.photoTwo);
				holder.photoTwo.setVisibility(View.VISIBLE);
			} else {
				holder.photoTwo.setVisibility(View.GONE);
			}
			
			holder.listIndex.setText(String.format(Locale.getDefault(), "%d.", position + 1));
			holder.name.setText(Html.fromHtml(
					String.format(Locale.getDefault(), 
							"<a href='%s'>%s</a>", result.getMobileUrl(), result.getName())));
			stripUnderlines(holder.name);
			
			StringBuilder builder = new StringBuilder();
			String addressOne = result.getAddressOne();
			if (!TextUtils.isEmpty(addressOne)) {
				builder.append(addressOne + NEWLINE);
			}
			
			String addressTwo = result.getAddressTwo();
			if (!TextUtils.isEmpty(addressTwo)) {
				builder.append(addressTwo + NEWLINE);
			}
			
			String details = result.getDetails();
			if (!TextUtils.isEmpty(details)) {
				builder.append(details + NEWLINE);
			}
			
			String snippet = builder.toString().trim();
			if (!TextUtils.isEmpty(snippet)) {
				holder.snippet.setText(builder.toString().trim());
				holder.snippet.setVisibility(View.VISIBLE);
			} else {
				holder.snippet.setVisibility(View.GONE);
			}
			
			String ratingImageUrl = result.getRatingImgUrl();
			Integer reviewCount = result.getReviewCount();
			if (TextUtils.isEmpty(ratingImageUrl) || 
					ratingImageUrl.equals("null") || 
					reviewCount == null || 
					reviewCount.intValue() == 0) {
				holder.ratingsContainer.setVisibility(View.GONE);
			} else {
				imageLoader.loadImage(ratingImageUrl, holder.ratingImage);
				holder.reviews.setText("(" + result.getReviewCount().toString() + ")");
				holder.ratingsContainer.setVisibility(View.VISIBLE);
			}
			return row;
		}
	}
	
	private void stripUnderlines(TextView textView) {
        Spannable s = (Spannable) textView.getText();
        URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
        for (URLSpan span: spans) {
            int start = s.getSpanStart(span);
            int end = s.getSpanEnd(span);
            s.removeSpan(span);
            span = new URLSpanNoUnderline(span.getURL());
            s.setSpan(span, start, end, 0);
        }
        textView.setText(s);
    }
	
	private class URLSpanNoUnderline extends URLSpan {
        public URLSpanNoUnderline(String url) {
            super(url);
        }
        
        @Override public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }
}
