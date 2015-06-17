package com.vocifery.daytripper.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.vocifery.daytripper.R;

public class ImageLoader {

	private static final String TAG = "ImageLoader";
	private final Context context;
	public BitmapCache imageCache;
	final private int imgWidth;
	final private int imgHeight;
	private static ImageLoader INSTANCE = null;
	
	private static final int MESSAGE_CLEAR = 0;
    private static final int MESSAGE_INIT_DISK_CACHE = 1;
    private static final int MESSAGE_FLUSH = 2;
    private static final int MESSAGE_CLOSE = 3;
	
	private ImageLoader(Context context) {
		this.context = context;
		this.imgWidth = (int) Math.floor(context.getResources().getDimension(R.dimen.image_thumbnail_size));
		this.imgHeight = (int) Math.floor(context.getResources().getDimension(R.dimen.image_thumbnail_size));
	}
	
	public final static ImageLoader getInstance(Context context) {
		if (INSTANCE == null) {
			INSTANCE = new ImageLoader(context);
		}
		return INSTANCE;
	}
	
	public void loadImage(String url, ImageView imageView) {
		Bitmap bitmap = getBitmap(url);
		if (bitmap != null) {
			imageView.setImageBitmap(bitmap);
		} else if (cancelPotentialWork(url, imageView)) {
			final BitmapWorkerTask task = new BitmapWorkerTask(url, imageView, context, imageCache, imgWidth, imgHeight);
			final AsyncDrawable asyncDrawable =
	                new AsyncDrawable(context.getResources(), null, task);
			imageView.setImageDrawable(asyncDrawable);
			task.execute(url);
		}
	}
	
	public void loadCompoundDrawable(String url, TextView textView) {
		Bitmap bitmap = getBitmap(url);
		if (bitmap != null) {
			BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
			textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
		} else if (cancelPotentialWork(url, textView)) {
			final BitmapWorkerTask task = new BitmapWorkerTask(url, textView, context, imageCache);
			final AsyncDrawable asyncDrawable = new AsyncDrawable(context.getResources(), null, task);
			textView.setCompoundDrawablesWithIntrinsicBounds(asyncDrawable, null, null, null);
			task.execute(url);
		}
	}
	
	public void addImageCache(FragmentManager fm) {
		this.imageCache = BitmapCache.getInstance(context, fm);
	}
	
	public void clearCache() {
		imageCache.clearCache();
	}
	
	public Bitmap getBitmapFromCache(String url) {
		Bitmap bitmap = imageCache.getBitmapFromMemCache(url);
		if (bitmap != null) {
			return bitmap;
		}
		return imageCache.getBitmapFromDiskCache(url);
	}
	
	public Bitmap fetchBitmap(String url) {
		return BitmapWorkerTask.fetchBitmap(url);
	}
	
	private static boolean cancelPotentialWork(Object data, ImageView imageView) {
	    final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
	    if (bitmapWorkerTask != null) {
	        final Object bitmapData = bitmapWorkerTask.getData();
	        // If bitmapData is not yet set or it differs from the new data
	        if (bitmapData == null || !bitmapData.equals(data)) {
	            // Cancel previous task
	            bitmapWorkerTask.cancel(true);
	        } else {
	            // The same work is already in progress
	            return false;
	        }
	    }
	    // No task associated with the ImageView, or an existing task was cancelled
	    return true;
	}
	
	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
	   if (imageView != null) {
	       final Drawable drawable = imageView.getDrawable();
	       if (drawable instanceof AsyncDrawable) {
	           final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
	           return asyncDrawable.getBitmapWorkerTask();
	       }
	    }
	    return null;
	}
	
	private static boolean cancelPotentialWork(Object data, TextView textView) {
	    final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(textView);
	    if (bitmapWorkerTask != null) {
	        final Object bitmapData = bitmapWorkerTask.getData();
	        // If bitmapData is not yet set or it differs from the new data
	        if (bitmapData == null || !bitmapData.equals(data)) {
	            // Cancel previous task
	            bitmapWorkerTask.cancel(true);
	        } else {
	            // The same work is already in progress
	            return false;
	        }
	    }
	    // No task associated with the ImageView, or an existing task was cancelled
	    return true;
	}
	
	private static BitmapWorkerTask getBitmapWorkerTask(TextView textView) {
	   if (textView != null) {
	       final Drawable drawable = textView.getCompoundDrawables()[0];
	       if (drawable instanceof AsyncDrawable) {
	           final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
	           return asyncDrawable.getBitmapWorkerTask();
	       }
	    }
	    return null;
	}
	
	private Bitmap getBitmap(String url) {
		Bitmap bitmap = imageCache.getBitmapFromMemCache(url);
		if (bitmap != null) {
			return bitmap;
		}
		return null;
	}
}
