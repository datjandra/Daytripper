package com.garudasystems.daytripper.view;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.backend.vocifery.Example;

public class ExampleAdapter extends ArrayAdapter<Example> {

	static class ExampleHolder {
		TextView instruction;
		TextView example;
		ImageView logo;
	}
	
	public ExampleAdapter(Context context, List<Example> examples) {
		super(context, R.layout.intro_row, examples);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		ExampleHolder holder = null;
		
		if (row == null) {
			row = LayoutInflater.from(getContext()).inflate(R.layout.intro_row, parent, false);
			holder = new ExampleHolder();
			holder.instruction = (TextView) row.findViewById(R.id.instruction);
			holder.example = (TextView) row.findViewById(R.id.example);
			holder.logo = (ImageView) row.findViewById(R.id.logo);
			row.setTag(holder);
		} else {
			holder = (ExampleHolder) row.getTag();
		}
		
		Example example = getItem(position);
		Drawable logo = stringToDrawable(example.getLogo(), row.getContext());
		holder.logo.setImageDrawable(logo);
		holder.instruction.setText(example.getInstruction());
		holder.example.setText(example.getExample());
		return row;
	}
	
	private Drawable stringToDrawable(String uri, Context context) {
		int imageResource = context.getResources().getIdentifier(uri, "drawable", context.getPackageName());
		Drawable image = context.getResources().getDrawable(imageResource);
		return image;
	}
}
