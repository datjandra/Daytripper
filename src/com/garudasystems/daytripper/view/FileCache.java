package com.garudasystems.daytripper.view;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
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
	
	public File getFile(String url) {
		return new File(cacheDir, String.valueOf(url.hashCode()));
	}
	
	public void saveBitmapFile(String url, Bitmap bitmap) {
		File file = new File(cacheDir, String.valueOf(url.hashCode()));
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			bitmap.compress(DEFAULT_COMPRESS_FORMAT, DEFAULT_COMPRESS_QUALITY, fos);
			fos.flush();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "saveBitmapFile - " + e);
		} catch (IOException e) {
			Log.e(TAG, "saveBitmapFile - " + e);
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
