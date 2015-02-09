package com.garudasystems.daytripper.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

	private final static String TAG = "BitmapWorkerTask";
	private final WeakReference<ImageView> imageViewRef;
	private final WeakReference<TextView> textViewRef;
	private final BitmapCache imageCache;
	private final Context context;
	private final boolean sample;
	private final Object data;
	private int imgWidth;
	private int imgHeight;
	
	
	public BitmapWorkerTask(Object data, ImageView imageView, Context context, BitmapCache imageCache, int imgWidth, int imgHeight) {
		this.imageCache = imageCache;
		this.context = context;
		imageViewRef = new WeakReference<ImageView>(imageView);
		textViewRef = null;
		this.imgWidth = imgWidth;
		this.imgHeight = imgHeight;
		this.sample = true;
		this.data = data;
	}
	
	public BitmapWorkerTask(Object data, TextView textView, Context context, BitmapCache imageCache) {
		this.imageCache = imageCache;
		this.context = context;
		textViewRef = new WeakReference<TextView>(textView);
		imageViewRef = null;
		this.sample = false;
		this.data = data;
	}
	
	public Object getData() {
		return data;
	}
	
	@Override
	protected Bitmap doInBackground(String... params) {
		Bitmap bitmap = null;
		String url = params[0];
		if (imageCache != null && !isCancelled()) {
			bitmap = imageCache.getBitmapFromDiskCache(url);
		}
		
		if (bitmap == null && imageCache != null && !isCancelled()) {
			bitmap = sample ? fetchBitmap(url) : fetchSampledBitmap(url, imgWidth, imgHeight);
		}
		
		if (bitmap != null) {
			if (imageCache != null) {
				imageCache.addBitmapToCache(url, bitmap);
			}
		}
		return bitmap;
	}

	@Override
	protected void onPostExecute(Bitmap bitmap) {
		if (isCancelled()) {
			bitmap = null;
        }
		
		if (bitmap != null) {
			if (imageViewRef != null) {
				final ImageView imageView = imageViewRef.get();
				final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
				if (this == bitmapWorkerTask && imageView != null) {
					imageView.setImageBitmap(bitmap);
				}
			} else if (textViewRef != null) {
				final TextView textView = textViewRef.get();
				final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(textView);
				if (this == bitmapWorkerTask && textView != null) {
					Drawable drawable = new BitmapDrawable(context.getResources(), bitmap);
					textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
				}
			}
		}
	}
	
	private final static Bitmap fetchBitmap(String url) {
		Bitmap bitmap = null;
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.connect();
			InputStream input = connection.getInputStream();
			bitmap = BitmapFactory.decodeStream(input);
		} catch (MalformedURLException e) {
			Log.e(TAG, "fetchBitmap - " + e);
		} catch (IOException e) {
			Log.e(TAG, "fetchBitmap - " + e);
		}
		return bitmap;
	}
	
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
	    // Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;
	
	    if (height > reqHeight || width > reqWidth) {
	        final int halfHeight = height / 2;
	        final int halfWidth = width / 2;
	
	        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
	        // height and width larger than the requested height and width.
	        while ((halfHeight / inSampleSize) > reqHeight
	                && (halfWidth / inSampleSize) > reqWidth) {
	            inSampleSize *= 2;
	        }
	    }
	    return inSampleSize;
	}
	
	private final static Bitmap fetchSampledBitmap(String url, int reqWidth, int reqHeight) {
		Bitmap bitmap = null;
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.connect();
			InputStream input = connection.getInputStream();
			
			final BitmapFactory.Options options = new BitmapFactory.Options();
		    options.inJustDecodeBounds = true;
		    BitmapFactory.decodeStream(input, null, options);
		    
		    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		    options.inJustDecodeBounds = false;
			bitmap = BitmapFactory.decodeStream(input, null, options);
		} catch (MalformedURLException e) {
			Log.e(TAG, "fetchSampledBitmap - " + e);
		} catch (IOException e) {
			Log.e(TAG, "fetchSampledBitmap - " + e);
		}
		return bitmap;
	}
	
	private final static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
	   if (imageView != null) {
	       final Drawable drawable = imageView.getDrawable();
	       if (drawable instanceof AsyncDrawable) {
	           final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
	           return asyncDrawable.getBitmapWorkerTask();
	       }
	    }
	    return null;
	}
	
	private final static BitmapWorkerTask getBitmapWorkerTask(TextView textView) {
	   if (textView != null) {
	       final Drawable drawable = textView.getCompoundDrawables()[0];
	       if (drawable instanceof AsyncDrawable) {
	           final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
	           return asyncDrawable.getBitmapWorkerTask();
	       }
	    }
	    return null;
	}
}
