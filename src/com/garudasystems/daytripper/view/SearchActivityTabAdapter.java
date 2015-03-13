package com.garudasystems.daytripper.view;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;

import com.garudasystems.daytripper.components.ShowListFragment;
import com.garudasystems.daytripper.components.map.ShowMapFragment;

public class SearchActivityTabAdapter extends FragmentPagerAdapter {

	private SparseArray<String> sparseArray = new SparseArray<String>();
	
	public final static int LIST_FRAGMENT_INDEX = 0;
	public final static int MAP_FRAGMENT_INDEX = 1;
	
	public SearchActivityTabAdapter(FragmentManager fm) {
        super(fm);
    }
	
	@Override
	public Fragment getItem(int tabIndex) {
		switch (tabIndex) {
			case LIST_FRAGMENT_INDEX:
				ShowListFragment showListFragment = new ShowListFragment();
				sparseArray.put(LIST_FRAGMENT_INDEX, showListFragment.getTag());
				return showListFragment;
			
			case MAP_FRAGMENT_INDEX:
				ShowMapFragment showMapFragment = new ShowMapFragment();
				sparseArray.put(MAP_FRAGMENT_INDEX, showMapFragment.getTag());
				return showMapFragment;	
		}
		return null;
	}

	@Override
	public int getCount() {
		return 2;
	}
	
	@Override
	public String getPageTitle(int tabIndex) {
		switch (tabIndex) {
			case LIST_FRAGMENT_INDEX:
				return "List";

			case MAP_FRAGMENT_INDEX:
				return "Map";
		}
		return null;
	}
	
	public String getFragmentTag(Integer index) {
		return sparseArray.get(index);
	}
}
