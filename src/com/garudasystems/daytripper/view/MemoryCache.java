package com.garudasystems.daytripper.view;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.graphics.Bitmap;

public class MemoryCache {

	private final Map<String, Bitmap> bitmapCache = 
			Collections.synchronizedMap(new LinkedHashMap<String, Bitmap>());
	private final static long CACHE_LIMIT = 1000000;
	
	private long cacheSize = 0;
	
	public Bitmap getBitmap(String url) {
		if (!bitmapCache.containsKey(url)) {
			return null;
		}
		return bitmapCache.get(url);
	}
	
	public void putBitmap(String url, Bitmap bitmap) {
		if (!bitmapCache.containsKey(url)) {
			bitmapCache.put(url, bitmap);
			long bitmapSize = getBitmapSize(bitmap);
			cacheSize += bitmapSize;
			resize();
		}
	}
	
	public void clear() {
		bitmapCache.clear();
		cacheSize = 0;
	}
	
	private void resize() {
		if (cacheSize > CACHE_LIMIT) {
			Iterator<Entry<String, Bitmap>> entries = bitmapCache.entrySet().iterator();
			while (entries.hasNext()) {
				Entry<String,Bitmap> entry = entries.next();
				long bitmapSize = getBitmapSize(entry.getValue());
				cacheSize -= bitmapSize;
				entries.remove();
				if (cacheSize <= CACHE_LIMIT) {
					break;
				}
			}
		}
	}
	
	private long getBitmapSize(Bitmap bitmap) {
        if(bitmap==null) {
            return 0;
        } 
        return (bitmap.getRowBytes() * bitmap.getHeight());
    }

}
