package com.garudasystems.daytripper.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;
import android.widget.TextView;

import com.garudasystems.daytripper.backend.vocifery.BitmapWorkerTask;

public class ImageLoader {

	private static final String TAG = "ImageLoader";
	private Context context = null;
	private FileCache fileCache = null;
	private MemoryCache memoryCache;

	public ImageLoader(Context context) {
		this.context = context;
		fileCache = new FileCache(context);
		memoryCache = new MemoryCache();
	}
	
	public void displayImage(String url, ImageView imageView) {
		Bitmap bitmap = getBitmap(url);
		if (bitmap != null) {
			imageView.setImageBitmap(bitmap);
		} else {
			BitmapWorkerTask task = new BitmapWorkerTask(imageView, fileCache, memoryCache);
			task.execute(url);
		}
	}
	
	public void displayCompoundDrawable(String url, TextView textView) {
		Bitmap bitmap = getBitmap(url);
		if (bitmap != null) {
			Drawable drawable = new BitmapDrawable(context.getResources(), bitmap);
			textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
		} else {
			BitmapWorkerTask task = new BitmapWorkerTask(textView, context, fileCache, memoryCache);
			task.execute(url);
		}
	}
	
	public void clearAll() {
		memoryCache.clear();
		fileCache.clear();
	}
	
	private Bitmap getBitmap(String url) {
		Bitmap bitmap = memoryCache.getBitmap(url);
		if (bitmap != null) {
			return bitmap;
		}
		
		File file = fileCache.getFile(url);
		if (file != null && file.exists()) {
			bitmap = decodeFile(file);
			if (bitmap != null) {
				return bitmap;
			}
		}
		return bitmap;
	}
	
	private Bitmap decodeFile(File file) {
		Bitmap bitmap = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			bitmap = BitmapFactory.decodeStream(fis);
			fis.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "decodeFile - " + e);
		} catch (IOException e) {
			Log.e(TAG, "decodeFile - " + e);
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {}	
		}	
		return bitmap;
	}
}
