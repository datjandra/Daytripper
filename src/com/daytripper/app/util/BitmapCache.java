package com.daytripper.app.util;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;

public class BitmapCache {

	private FileCache diskCache;
	private LruCache<String, Bitmap> memCache;
	private final Object cacheLock = new Object();
	private boolean cacheStarting = true;
	private final Context context;
	
	private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
	private static final String DISK_CACHE_SUBDIR = "thumbs";
	private static final int DISK_CACHE_INDEX = 0;
	private static final int compressQuality = 70;
	private static final CompressFormat compressFormat = CompressFormat.PNG;

	public final static String TAG = "BitmapCache";
	
	private BitmapCache(Context context) {
		this.context = context;
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int memCacheSize = maxMemory / 8;
		memCache = new LruCache<String, Bitmap>(memCacheSize) {
			@Override
	        protected int sizeOf(String key, Bitmap bitmap) {
				// The cache size will be measured in kilobytes rather than
	            // number of items.
	            return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
	        }
	    };
		
		File cacheDir = getDiskCacheDir(context, DISK_CACHE_SUBDIR);
		new InitDiskCacheTask().execute(cacheDir);
	}
	
	public static BitmapCache getInstance(Context context, FragmentManager fragmentManager) {
        // Search for, or create an instance of the non-UI RetainFragment
        final RetainFragment retainFragment = findOrCreateRetainFragment(fragmentManager);

        // See if we already have an ImageCache stored in RetainFragment
        BitmapCache imageCache = (BitmapCache) retainFragment.getObject();

        // No existing ImageCache, create one and store it in RetainFragment
        if (imageCache == null) {
            imageCache = new BitmapCache(context);
            retainFragment.setObject(imageCache);
        }
        return imageCache;
    }
	
	public Bitmap getBitmapFromMemCache(String key) {
	    return memCache.get(key);
	}
	
	public Bitmap getBitmapFromDiskCache(String key) {
	    synchronized (cacheLock) {
	        // Wait while disk cache is started from background thread
	        while (cacheStarting) {
	            try {
	                cacheLock.wait();
	            } catch (InterruptedException e) {}
	        }

	        if (diskCache != null) {
	        	return diskCache.get(key);
	        }
	    }
	    return null;
	}

	public void clearCache() {
	}
	
	public void addBitmapToCache(String key, Bitmap bitmap) {
	    // Add to memory cache as before
	    if (getBitmapFromMemCache(key) == null) {
	        memCache.put(key, bitmap);
	    }
	    
	    // Also add to disk cache
	    synchronized (cacheLock) {
	        if (diskCache != null && diskCache.get(key) == null) {
	        	diskCache.put(key, bitmap);
	        }
	    }
	}

	private final static File getDiskCacheDir(Context context, String uniqueName) {
		final String cachePath = context.getCacheDir().getPath();
	    return new File(cachePath + File.separator + uniqueName);
	}
	
	private class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
	    @Override
	    protected Void doInBackground(File... params) {
	        synchronized (cacheLock) {
	        	diskCache = new FileCache(context);
				cacheStarting = false; // Finished initialization
				cacheLock.notifyAll(); // Wake any waiting threads
	        }
	        return null;
	    }
	}
	
	/**
     * Locate an existing instance of this Fragment or if not found, create and
     * add it using FragmentManager.
     *
     * @param fm The FragmentManager manager to use.
     * @return The existing instance of the Fragment or the new instance if just
     *         created.
     */
    private static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
        //BEGIN_INCLUDE(find_create_retain_fragment)
        // Check to see if we have retained the worker fragment.
        RetainFragment mRetainFragment = (RetainFragment) fm.findFragmentByTag(TAG);

        // If not retained (or first time running), we need to create and add it.
        if (mRetainFragment == null) {
            mRetainFragment = new RetainFragment();
            fm.beginTransaction().add(mRetainFragment, TAG).commitAllowingStateLoss();
        }

        return mRetainFragment;
        //END_INCLUDE(find_create_retain_fragment)
    }

    /**
     * A simple non-UI Fragment that stores a single Object and is retained over configuration
     * changes. It will be used to retain the ImageCache object.
     */
    public static class RetainFragment extends Fragment {
        private Object mObject;

        /**
         * Empty constructor as per the Fragment documentation
         */
        public RetainFragment() {}

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Make sure this Fragment is retained over a configuration change
            setRetainInstance(true);
        }

        /**
         * Store a single object in this Fragment.
         *
         * @param object The object to store
         */
        public void setObject(Object object) {
            mObject = object;
        }

        /**
         * Get the stored object.
         *
         * @return The stored object
         */
        public Object getObject() {
            return mObject;
        }
    }
}
