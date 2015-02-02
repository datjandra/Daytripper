package com.garudasystems.daytripper.view;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.backend.vocifery.Example;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";
	
	private ProgressBar progressBar;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        final String[] instructions = getResources().getStringArray(R.array.instruction_array); 
        final String[] examples = getResources().getStringArray(R.array.example_array); 
        final String[] logos = getResources().getStringArray(R.array.image_array); 
        List<Example> exampleList = new ArrayList<Example>();
        for (int i=0; i<examples.length; i++) {
        	exampleList.add(new Example(instructions[i], examples[i], logos[i]));
        }
        
        ExampleAdapter adapter = new ExampleAdapter(this, exampleList);
        ListView listView = (ListView) findViewById(R.id.examples);
        listView.setAdapter(adapter);
        
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_main_actions, menu);

	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		    LinearLayout customLayout = (LinearLayout) menu.findItem(R.id.custom).getActionView();
		    progressBar = (ProgressBar) customLayout.findViewById(R.id.progress_bar);
		    
		    SearchView searchView = (SearchView) customLayout.findViewById(R.id.search_view);
		    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener( ) {
		    	@Override
		        public boolean onQueryTextChange(String text) {
		    		return true;
		    	}
		    	
		    	@Override
		        public boolean onQueryTextSubmit(String query) {
		    		progressBar.setVisibility(View.VISIBLE);
		    		return false;
		        }
		    });
	    }
		return true;
	}
}
