package com.vocifery.daytripper.util;

import android.content.Context;
import android.content.res.AssetManager;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by deboratjandra on 7/17/16.
 */
public class AssetUtils {

    private static final String TAG = "AssetUtils";

    private AssetUtils() {
    }

    public static void copyFileOrDir(String path, Context context) {
        AssetManager assetManager = context.getAssets();
        String assets[] = null;
        try {
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFile(path, context);
            } else {
                File dir = new File(context.getExternalFilesDir(null), path);
                if (dir.isDirectory() && dir.exists()) {
                    Log.i(TAG, String.format("directory %s exists", dir.getName()));
                    return;
                }

                if (!dir.exists()) {
                    dir.mkdir();
                }

                for (int i = 0; i < assets.length; ++i) {
                    copyFileOrDir(path + File.separator + assets[i], context);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private static void copyFile(String filename, Context context) {
        File newFile = new File(context.getExternalFilesDir(null), filename);
        if (newFile.exists()) {
            Log.i(TAG, String.format("file %s exists", newFile.getName()));
            return;
        }

        AssetManager assetManager = context.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(filename);
            out = new FileOutputStream(newFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}
