package com.vocifery.daytripper.ui;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.vocifery.daytripper.util.AssetUtils;

import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;

import java.io.File;

public class Daytripper extends Application {

	public final static String USERNAME_KEY = "com.vocifery.daytripper.Daytripper.USERNAME";
	
	private String lastQuery;
	private String neuraEventName;
	private String neuraEventDetails;
	private static Context context;
	private static Chat chatSession;

	private static final String TAG = "Daytripper";
	
	public void onCreate() {
		this.initChatbot();
		Daytripper.context = getApplicationContext();
		super.onCreate();
	}

	public String getLastQuery() {
		return lastQuery;
	}

	
	public final static Context getAppContext() {
        return Daytripper.context;
    }

	public final static Chat getChatSession() { return Daytripper.chatSession; }

	private void initChatbot() {
		try {
			String botDir = "bots";
			String botName = "alice2";
			AssetUtils.copyFileOrDir(botDir, this);
			File botPath = getExternalFilesDir(null);
			Bot bot = new Bot(botName, botPath.getAbsolutePath());
			chatSession = new Chat(bot);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}
}
