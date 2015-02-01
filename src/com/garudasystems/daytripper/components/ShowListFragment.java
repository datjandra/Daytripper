package com.garudasystems.daytripper.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.backend.vocifery.QueryResponse;
import com.garudasystems.daytripper.backend.vocifery.Result;
import com.garudasystems.daytripper.view.SearchActivity;
import com.garudasystems.daytripper.view.SearchResultAdapter;

public class ShowListFragment extends Fragment implements AbsListView.OnScrollListener {

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
	private Map<String,String> sourceLogos;
	
	public static final String TAG = "ShowListFragment";
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		sourceLogos = new HashMap<String,String>();
		final String[] vendors = getResources().getStringArray(R.array.vendor_array);  
        final String[] logos = getResources().getStringArray(R.array.logo_array); 
        for (int i=0; i<vendors.length; i++) {
        	sourceLogos.put(vendors[i], logos[i]);
        }
        
		View rootView = inflater.inflate(R.layout.show_list_fragment, container, false); 
		listView = (ListView) rootView.findViewById(R.id.list);
		listView.setOnScrollListener(this);
		progressBar = (ProgressBar) rootView.findViewById(R.id.fetch_progress);
		attributionLogo = (ImageView) rootView.findViewById(R.id.attribution_logo);
        return rootView;
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		allItems = new ArrayList<Result>();
		this.searchActivity = (SearchActivity) getActivity();
		adapter = new SearchResultAdapter(searchActivity, allItems);
		listView.setAdapter(adapter);
	}
	
	public void refreshList(QueryResponse queryResponse) {
		if (queryResponse != null && queryResponse.getTotal() > 0) {
			this.source = queryResponse.getSource();
			this.currentPage = queryResponse.getPage();
			this.total = queryResponse.getTotal();
			this.chunkSize = queryResponse.getChunk();
			
			if (source != null && sourceLogos.containsKey(source)) {
				String uri = "@drawable/" + sourceLogos.get(source);
				int imageResource = getResources().getIdentifier(uri, "drawable", getActivity().getPackageName());  
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
	
	/* (non-Javadoc)
	 * @see android.widget.AbsListView.OnScrollListener#onScroll(android.widget.AbsListView, int, int, int)
	 */
	@Override
	public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		int lastItem = firstVisibleItem + visibleItemCount;
		if (lastItem == totalItemCount) {
			if (previousLastItem != lastItem) {
				if ((total - totalItemCount) > 0) {
					Log.d(TAG, String.format("Loading page %d", currentPage+1));
					if (progressBar != null) {
						progressBar.setVisibility(View.VISIBLE);
					}
					searchActivity.refresh(currentPage+1, chunkSize);
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
		adapter = new SearchResultAdapter(searchActivity, allItems);
		listView.setAdapter(adapter);
	}
}
