package com.vocifery.daytripper.ui.components;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.beyondar.android.fragment.BeyondarFragmentSupport;
import com.beyondar.android.world.GeoObject;
import com.beyondar.android.world.World;
import com.vocifery.daytripper.R;

public class ARFragment extends DialogFragment {

	private BeyondarFragmentSupport beyondar;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		GeoObject test = new GeoObject(2l);
		test.setGeoPosition(41.90518966360719d, 2.56582424468222d);
		test.setImageUri("http://beyondar.github.io/beyondar/images/logo_512.png");
		test.setName("Online image");
		
		World world = new World(getActivity());
		world.addBeyondarObject(test);
		
		beyondar = new BeyondarFragmentSupport();
		beyondar.setWorld(world);
		beyondar.showFPS(true);
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.headsup_view, container, false);
		FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
		transaction.add(R.id.parentLayout, beyondar).commit();
		return view;
	}
	
	public BeyondarFragmentSupport getBeyondar() {
		return beyondar;
	}
	
	public static ARFragment newInstance() {
		ARFragment arFragment = new ARFragment();
		return arFragment;
	}
}
