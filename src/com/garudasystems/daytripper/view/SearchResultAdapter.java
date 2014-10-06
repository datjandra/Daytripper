package com.garudasystems.daytripper.view;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.garudasystems.daytripper.R;
import com.garudasystems.daytripper.backend.vocifery.SearchResult;

public class SearchResultAdapter extends ArrayAdapter<SearchResult> {

	private static final String TAG = "SearchResultAdapter";
	private ImageLoader imageLoader = null;
	
	static class ViewHolder {
		TextView name;
		TextView reviews;
		TextView price;
		TextView firstLine;
		TextView secondLine;
		TextView phone;
		ImageView photo;
		Drawable ratingImage;
	}
	
	public SearchResultAdapter(Context context, List<SearchResult> results) {
		super(context, R.layout.list_row, results);
		imageLoader = new ImageLoader(context);
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
			holder.photo = (ImageView) row.findViewById(R.id.photo);
			holder.reviews = (TextView) row.findViewById(R.id.rating);
			holder.price = (TextView) row.findViewById(R.id.price);
			holder.firstLine = (TextView) row.findViewById(R.id.firstLine);
			holder.secondLine = (TextView) row.findViewById(R.id.secondLine);
			holder.phone = (TextView) row.findViewById(R.id.phone);
			row.setTag(holder);
		} else {
			holder = (ViewHolder) row.getTag();
		}
		
		SearchResult result = getItem(position);
		imageLoader.displayImage(result.getImageUrl(), holder.photo);
		imageLoader.displayCompoundDrawable(result.getRatingImgUrl(), holder.reviews);
		holder.name.setText(Html.fromHtml("<a href='" + result.getMobileUrl() + "'>" + result.getName() + "</a>"));
		holder.reviews.setText("(" + result.getReviewCount().toString() + ")");
		holder.price.setText(result.getDeal());
		holder.firstLine.setText(result.getAddressOne());
		holder.secondLine.setText(result.getAddressTwo());
		holder.phone.setText(result.getDisplayPhone());
		return row;
	}
	
	public void clearAll() {
		imageLoader.clearAll();
	}
}
