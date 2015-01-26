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
import com.garudasystems.daytripper.backend.vocifery.Result;

public class SearchResultAdapter extends ArrayAdapter<Result> {

	private static final String TAG = "SearchResultAdapter";
	private ImageLoader imageLoader = null;
	
	static class ViewHolder {
		TextView name;
		TextView reviews;
		TextView price;
		TextView firstLine;
		TextView secondLine;
		TextView phone;
		ImageView photoOne;
		ImageView photoTwo;
		Drawable ratingImage;
	}
	
	public SearchResultAdapter(Context context, List<Result> results) {
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
			holder.photoOne = (ImageView) row.findViewById(R.id.photo_one);
			holder.photoTwo = (ImageView) row.findViewById(R.id.photo_two);
			holder.reviews = (TextView) row.findViewById(R.id.rating);
			holder.price = (TextView) row.findViewById(R.id.price);
			holder.firstLine = (TextView) row.findViewById(R.id.firstLine);
			holder.secondLine = (TextView) row.findViewById(R.id.secondLine);
			holder.phone = (TextView) row.findViewById(R.id.phone);
			row.setTag(holder);
		} else {
			holder = (ViewHolder) row.getTag();
		}
		
		Result result = getItem(position);
		String imageOneUrl = result.getImageOneUrl();
		if (imageOneUrl != null && !imageOneUrl.equals("null")) {
			imageLoader.displayImage(imageOneUrl, holder.photoOne);
		} else {
			holder.photoOne.setVisibility(View.GONE);
		}
		
		String imageTwoUrl = result.getImageTwoUrl();
		if (imageTwoUrl != null && !imageTwoUrl.equals("null")) {
			imageLoader.displayImage(imageTwoUrl, holder.photoTwo);
		} else {
			holder.photoTwo.setVisibility(View.GONE);
		}
		
		holder.name.setText(Html.fromHtml("<a href='" + result.getMobileUrl() + "'>" + result.getName() + "</a>"));
		holder.price.setText(result.getDeal());
		holder.firstLine.setText(result.getAddressOne());
		holder.secondLine.setText(result.getAddressTwo());
		holder.phone.setText(result.getDetails());
		
		String ratingImageUrl = result.getRatingImgUrl();
		Integer reviewCount = result.getReviewCount();
		if (ratingImageUrl == null || ratingImageUrl.equals("null") || reviewCount == null || reviewCount.intValue() == 0) {
			holder.reviews.setVisibility(View.GONE);
		} else {
			imageLoader.displayCompoundDrawable(ratingImageUrl, holder.reviews);
			holder.reviews.setText("(" + result.getReviewCount().toString() + ")");
		}
		return row;
	}
	
	public void clearAll() {
		imageLoader.clearAll();
	}
}
