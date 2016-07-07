package com.vocifery.daytripper.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.util.Log;

public class ResourceUtils {

	private static final String TAG = "ResourceUtils";

	private ResourceUtils() {}
	
	public final static String readTextFromResource(Context context, int resourceID) {
		String newline = System.getProperty("line.separator") ;
		InputStream raw = context.getResources().openRawResource(resourceID);
		StringBuilder contentBuilder = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(raw));
		try {
			String line;
			while ((line = br.readLine()) != null) {
				contentBuilder.append(line);
				contentBuilder.append(newline);
			}
			br.close();
		} catch (IOException e) {
			Log.i(TAG, "readTextFromResource - " + e.getMessage());
		}  finally {
			try {
				br.close();
			} catch (Exception e) {}
		}
		return contentBuilder.toString();
	}
}
