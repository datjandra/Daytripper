package com.garudasystems.daytripper.view;

import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.backend.vocifery.SearchResult;

public class ShowListFragment extends Fragment {

	private ListView listView;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.show_list_fragment, container, false); 
		listView = (ListView) rootView.findViewById(R.id.list);
        return rootView;
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Bundle arguments = getArguments();
		if (arguments != null) {
			List<SearchResult> resultList = arguments.getParcelableArrayList(SearchResult.class.getName());
			SearchResultAdapter adapter = new SearchResultAdapter(getActivity(), resultList);
			listView.setAdapter(adapter);
		}
	}
	
	public void refresh(List<SearchResult> resultList) {
		SearchResultAdapter adapter = new SearchResultAdapter(getActivity(), resultList);
		listView.setAdapter(adapter);
	}
}
