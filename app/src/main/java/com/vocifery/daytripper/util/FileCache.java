package com.vocifery.daytripper.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Log;

public class FileCache {

	private File cacheDir;
	private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.PNG;
    private static final int DEFAULT_COMPRESS_QUALITY = 70;
    private static final String TAG = "FileCache";

	public FileCache(Context context) {
		cacheDir = context.getCacheDir();
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
	}
	
	public Bitmap get(String key) {
		Bitmap bitmap = null;
		File bitmapFile = new File(cacheDir, String.valueOf(key.hashCode()));
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(bitmapFile);
			bitmap = BitmapFactory.decodeStream(fis);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "get - " + e);
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {}
		}
		return bitmap;
	}
	
	public void put(String key, Bitmap bitmap) {
		File file = new File(cacheDir, String.valueOf(key.hashCode()));
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			bitmap.compress(DEFAULT_COMPRESS_FORMAT, DEFAULT_COMPRESS_QUALITY, fos);
			fos.flush();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "put - " + e);
		} catch (IOException e) {
			Log.e(TAG, "put - " + e);
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {}	
		}	
	}
	
	public void clear() {
		File[] fileList = cacheDir.listFiles();
		if (fileList == null) {
			return;
		}
		for (File file : fileList) {
			file.delete();
		}
	}
}
