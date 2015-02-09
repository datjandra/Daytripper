package com.garudasystems.daytripper.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.backend.vocifery.QueryResponse;
import com.garudasystems.daytripper.backend.vocifery.Result;
import com.garudasystems.daytripper.util.ImageLoader;
import com.garudasystems.daytripper.view.SearchActivity;

public class ShowListFragment extends Fragment implements
		AbsListView.OnScrollListener {

	private ListView listView;
	private ProgressBar progressBar;
	private ImageView attributionLogo;
	private List<Result> allItems;
	private SearchResultAdapter adapter;
	private String source;
	private int currentPage;
	private int total;
	private int chunkSize;
	private int previousLastItem = 0;
	private SearchActivity searchActivity;
	private Map<String, String> sourceLogos;
	private ImageLoader imageLoader;
	
	public static final String TAG = "ShowListFragment";
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		imageLoader = new ImageLoader(getActivity());
		imageLoader.addImageCache(getFragmentManager());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		sourceLogos = new HashMap<String, String>();
		final String[] vendors = getResources().getStringArray(
				R.array.vendor_array);
		final String[] logos = getResources()
				.getStringArray(R.array.logo_array);
		for (int i = 0; i < vendors.length; i++) {
			sourceLogos.put(vendors[i], logos[i]);
		}

		View rootView = inflater.inflate(R.layout.show_list_fragment,
				container, false);
		listView = (ListView) rootView.findViewById(R.id.list);
		listView.setOnScrollListener(this);
		progressBar = (ProgressBar) rootView.findViewById(R.id.fetch_progress);
		attributionLogo = (ImageView) rootView
				.findViewById(R.id.attribution_logo);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		allItems = new ArrayList<Result>();
		this.searchActivity = (SearchActivity) getActivity();
		adapter = new SearchResultAdapter(getActivity());
		listView.setAdapter(adapter);
	}
	
	@Override
	public void onDestroy() {
		imageLoader.clearCache();
		super.onDestroy();
	}

	public void refreshList(QueryResponse queryResponse) {
		if (queryResponse != null && queryResponse.getTotal() > 0) {
			this.source = queryResponse.getSource();
			this.currentPage = queryResponse.getPage();
			this.total = queryResponse.getTotal();
			this.chunkSize = queryResponse.getChunk();

			if (source != null && sourceLogos.containsKey(source)) {
				String uri = "@drawable/" + sourceLogos.get(source);
				int imageResource = getResources().getIdentifier(uri,
						"drawable", getActivity().getPackageName());
				attributionLogo.setImageResource(imageResource);
			}
		}

		List<Result> resultList = queryResponse.getResultList();
		if (allItems != null && resultList != null && !resultList.isEmpty()) {
			allItems.addAll(resultList);
			adapter.notifyDataSetChanged();
		}

		if (progressBar != null) {
			progressBar.setVisibility(View.INVISIBLE);
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
					Log.d(TAG,
							String.format("Loading page %d", currentPage + 1));
					if (progressBar != null) {
						progressBar.setVisibility(View.VISIBLE);
					}
					searchActivity.refresh(currentPage + 1, chunkSize);
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
		attributionLogo.setImageDrawable(null);
		allItems = new ArrayList<Result>();
		adapter = new SearchResultAdapter(getActivity());
		listView.setAdapter(adapter);
	}
	
	private class ViewHolder {
		TextView name;
		TextView reviews;
		TextView price;
		TextView firstLine;
		TextView secondLine;
		TextView phone;
		ImageView photoOne;
		ImageView photoTwo;
		ImageView ratingImage;
	}
	
	private class SearchResultAdapter extends ArrayAdapter<Result> {
		
		public SearchResultAdapter(Context context) {
			super(context, R.layout.list_row, allItems);
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
				holder.reviews = (TextView) row.findViewById(R.id.rating_count);
				holder.ratingImage = (ImageView) row.findViewById(R.id.rating_img);
				holder.price = (TextView) row.findViewById(R.id.price);
				holder.firstLine = (TextView) row.findViewById(R.id.firstLine);
				holder.secondLine = (TextView) row.findViewById(R.id.secondLine);
				holder.phone = (TextView) row.findViewById(R.id.phone);
				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}
			
			Result result = getItem(position);
			String imageOneUrl = result.getImageOneUrl();
			if (imageOneUrl != null && !imageOneUrl.equals("null")) {
				imageLoader.loadImage(imageOneUrl, holder.photoOne);
				holder.photoOne.setVisibility(View.VISIBLE);
			} else {
				holder.photoOne.setVisibility(View.GONE);
			}
			
			String imageTwoUrl = result.getImageTwoUrl();
			if (imageTwoUrl != null && !imageTwoUrl.equals("null")) {
				imageLoader.loadImage(imageTwoUrl, holder.photoTwo);
				holder.photoTwo.setVisibility(View.VISIBLE);
			} else {
				holder.photoTwo.setVisibility(View.GONE);
			}
			
			holder.name.setText(Html.fromHtml("<a href='" + result.getMobileUrl() + "'>" + result.getName() + "</a>"));
			holder.price.setText(result.getDeal());
			holder.firstLine.setText(result.getAddressOne());
			holder.secondLine.setText(result.getAddressTwo());
			holder.phone.setText(result.getDetails());
			
			String ratingImageUrl = result.getRatingImgUrl();
			Integer reviewCount = result.getReviewCount();
			if (ratingImageUrl == null || ratingImageUrl.equals("null") || reviewCount == null || reviewCount.intValue() == 0) {
				holder.reviews.setVisibility(View.GONE);
				holder.ratingImage.setVisibility(View.GONE);
			} else {
				imageLoader.loadImage(ratingImageUrl, holder.ratingImage);
				holder.ratingImage.setVisibility(View.VISIBLE);
				holder.reviews.setVisibility(View.VISIBLE);
				holder.reviews.setText("(" + result.getReviewCount().toString() + ")");
			}
			return row;
		}
	}
}
