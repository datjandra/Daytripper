package com.daytripper.app.ui.components;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daytripper.app.R;
import com.daytripper.app.ui.SearchActivityTabAdapter;

public class ViewPagerFragment extends Fragment {

	private ViewPager viewPager;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sample, container, false);
    }
	
	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        viewPager = (ViewPager) view.findViewById(R.id.viewpager);
        viewPager.setAdapter(new SearchActivityTabAdapter(this.getFragmentManager()));
        viewPager.requestTransparentRegion(viewPager);
	}
}
