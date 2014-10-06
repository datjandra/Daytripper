package com.garudasystems.daytripper.view;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ListView;
import android.widget.SearchView;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.backend.vocifery.Example;

public class MainActivity extends Activity {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        final String[] phrases = getResources().getStringArray(R.array.phrase_array); 
        final String[] examples = getResources().getStringArray(R.array.example_array); 
        final String[] logos = getResources().getStringArray(R.array.logo_array); 
        List<Example> exampleList = new ArrayList<Example>();
        for (int i=0; i<phrases.length; i++) {
        	exampleList.add(new Example(phrases[i], examples[i], logos[i]));
        }
        
        ExampleAdapter adapter = new ExampleAdapter(this, exampleList);
        ListView listView = (ListView) findViewById(R.id.examples);
        listView.setAdapter(adapter);
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_main_actions, menu);

	    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	    SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
	    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
	    searchView.setIconifiedByDefault(false);
		return true;
	}
}
