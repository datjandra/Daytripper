package com.garudasystems.daytripper.components;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.view.SearchActivityTabAdapter;

public class ViewPagerFragment extends Fragment {

	private UnswipeableViewPager viewPager;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sample, container, false);
    }
	
	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        viewPager = (UnswipeableViewPager) view.findViewById(R.id.viewpager);
        viewPager.setAdapter(new SearchActivityTabAdapter(this.getFragmentManager()));
        viewPager.requestTransparentRegion(viewPager);
	}
}
