package com.garudasystems.daytripper.backend.vocifery;

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

import com.garudasystems.daytripper.view.FileCache;
import com.garudasystems.daytripper.view.MemoryCache;

public class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

	private final static String TAG = "BitmapWorkerTask";
	private final WeakReference<ImageView> imageViewRef;
	private final WeakReference<TextView> textViewRef;
	private final FileCache fileCache;
	private final MemoryCache memoryCache;
	private Context context = null;
	
	public BitmapWorkerTask(ImageView imageView, FileCache fileCache, MemoryCache memoryCache) {
		this.fileCache = fileCache;
		this.memoryCache = memoryCache;
		imageViewRef = new WeakReference<ImageView>(imageView);
		textViewRef = null;
	}
	
	public BitmapWorkerTask(TextView textView, Context context, FileCache fileCache, MemoryCache memoryCache) {
		this.context = context;
		this.fileCache = fileCache;
		this.memoryCache = memoryCache;
		textViewRef = new WeakReference<TextView>(textView);
		imageViewRef = null;
	}
	
	@Override
	protected Bitmap doInBackground(String... params) {
		String url = params[0];
		Bitmap bitmap = fetchBitmap(url);
		if (bitmap != null) {
			memoryCache.putBitmap(url, bitmap);
			fileCache.saveBitmapFile(url, bitmap);
		}
		return bitmap;
	}

	@Override
	protected void onPostExecute(Bitmap bitmap) {
		if (imageViewRef != null) {
			ImageView imageView = imageViewRef.get();
			if (imageView != null && bitmap != null) {
				imageView.setImageBitmap(bitmap);
			}
		} else if (textViewRef != null) {
			TextView textView = textViewRef.get();
			if (textView != null && bitmap != null && context != null) {
				Drawable drawable = new BitmapDrawable(context.getResources(), bitmap);
				textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
			}
		}
	}
	
	private Bitmap fetchBitmap(String url) {
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
}
