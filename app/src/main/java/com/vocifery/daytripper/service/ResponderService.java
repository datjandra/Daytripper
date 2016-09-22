package com.vocifery.daytripper.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.vocifery.daytripper.ui.Daytripper;
import com.vocifery.daytripper.util.StringUtils;

import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.AIMLProcessorExtension;
import org.alicebot.ab.Chat;
import org.alicebot.ab.ParseState;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.jsoup.Jsoup;
import org.w3c.dom.Node;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ResponderService extends IntentService implements RequestConstants, AIMLProcessorExtension {

	public static final String RESPONSE_ACTION = "com.vocifery.daytripper.RESPONSE_ACTION";
	public static final String USER_ACTION = "com.vocifery.daytripper.USER_ACTION";
	public static final String ROBOT_ACTION = "com.vocifery.daytripper.ROBOT_ACTION";

	public static final String EXTRA_TEXT_MESSAGE = "com.vocifery.daytripper.extra.TEXT_MESSAGE";
	public static final String EXTRA_URL_MESSAGE = "com.vocifery.daytripper.extra.URL_MESSAGE";
	public final static String EXTRA_CONTENT_MESSAGE = "com.vocifery.daytripper.extra.CONTENT_MESSAGE";
	
	public static final String KEY_QUERY = "com.vocifery.daytripper.QUERY";
	public static final String KEY_lOCATION = "com.vocifery.daytripper.LOCATION";

	public static final String VOICE_FLAG = "voice";
	public static final String NEURA_USER_ARRIVED_HOME = "userArrivedHome";
	public static final String NEURA_USER_LEFT_HOME = "userLeftHome";
	public static final String NEURA_USER_ARRIVED_TO_WORK = "userArrivedToWork";
	public static final String NEURA_USER_LEFT_WORK = "userLeftWork";

	private static final String TAG = "ResponderService";
	private static final Map<String,String> KEYWORD_HASHES = new HashMap<String,String>();
	private static final int NGRAM_SIZE = 3;
		
	static {
		DoubleMetaphone encoder = new DoubleMetaphone();
		KEYWORD_HASHES.put(encoder.encode("meetup events"), "meetup events");
		KEYWORD_HASHES.put(encoder.encode("meetup groups"), "meetup groups");
		KEYWORD_HASHES.put(encoder.encode("near here"), "near here");
	}

	public ResponderService() {
		super("ResponderService");
		AIMLProcessor.extension = this;
		Log.i(TAG, "constructed");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "onCreate()");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "onHandleIntent()");
		String query = intent.getStringExtra(KEY_QUERY);
		query = query.toLowerCase(Locale.getDefault()).replace("\'", "");
		
		DoubleMetaphone encoder = new DoubleMetaphone();
		Set<String> ngrams = StringUtils.extractNgrams(query, NGRAM_SIZE);
		for (String segment : ngrams) {
			String hash = encoder.encode(segment);
			if (KEYWORD_HASHES.containsKey(hash)) {
				query = query.replace(segment, KEYWORD_HASHES.get(hash));
				break;
			}
		}

		final Daytripper daytripper = (Daytripper) getApplicationContext();
		final Chat chatSession = daytripper.getChatSession();
		String response = chatSession.multisentenceRespond(query);

		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(RESPONSE_ACTION);
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

		String url = chatSession.predicates.get("url");
		if (!TextUtils.isEmpty(url) && !url.equalsIgnoreCase("unknown")) {
			chatSession.predicates.remove("url");
			broadcastIntent.putExtra(EXTRA_URL_MESSAGE, url);
		} else {
			broadcastIntent.putExtra(EXTRA_CONTENT_MESSAGE, response);
		}

		String voice = chatSession.predicates.get(VOICE_FLAG);
		if (!TextUtils.isEmpty(voice) && !voice.equalsIgnoreCase("unknown")) {
			chatSession.predicates.remove(VOICE_FLAG);
			broadcastIntent.putExtra(VOICE_FLAG, voice);
		}

		String userArrivedHome = chatSession.predicates.get(NEURA_USER_ARRIVED_HOME);
		if (!TextUtils.isEmpty(userArrivedHome) && !userArrivedHome.equalsIgnoreCase("unknown")) {
			chatSession.predicates.remove(NEURA_USER_ARRIVED_HOME);
			broadcastIntent.putExtra(NEURA_USER_ARRIVED_HOME, userArrivedHome);
		}

		String userLeftHome = chatSession.predicates.get(NEURA_USER_LEFT_HOME);
		if (!TextUtils.isEmpty(userLeftHome) && !userLeftHome.equalsIgnoreCase("unknown")) {
			chatSession.predicates.remove(NEURA_USER_LEFT_HOME);
			broadcastIntent.putExtra(NEURA_USER_LEFT_HOME, userLeftHome);
		}

		String userArrivedToWork = chatSession.predicates.get(NEURA_USER_ARRIVED_TO_WORK);
		if (!TextUtils.isEmpty(userArrivedToWork) && !userArrivedToWork.equalsIgnoreCase("unknown")) {
			chatSession.predicates.remove(NEURA_USER_ARRIVED_TO_WORK);
			broadcastIntent.putExtra(NEURA_USER_ARRIVED_TO_WORK, userArrivedToWork);
		}

		String userLeftWork = chatSession.predicates.get(NEURA_USER_LEFT_WORK);
		if (!TextUtils.isEmpty(userLeftWork) && !userLeftWork.equalsIgnoreCase("unknown")) {
			chatSession.predicates.remove(NEURA_USER_LEFT_WORK);
			broadcastIntent.putExtra(NEURA_USER_LEFT_WORK, userLeftWork);
		}

		broadcastIntent.putExtra(EXTRA_TEXT_MESSAGE, Jsoup.parse(response).text());
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
	}

	public Set<String> extensionTagSet() {
		return new HashSet<String>(Arrays.asList(new String[] {
				"search",
				"location",
				"url",
				VOICE_FLAG,
				NEURA_USER_ARRIVED_HOME,
				NEURA_USER_LEFT_HOME,
				NEURA_USER_ARRIVED_TO_WORK,
				NEURA_USER_LEFT_WORK
		}));
	}

	public String recursEval(Node node, ParseState ps) {
		String tagContent = AIMLProcessor.evalTagContent(node, ps, null);
		String nodeName = node.getNodeName();
		Log.i(TAG, String.format("%s - %s", nodeName, tagContent));
		switch (nodeName) {
			case "url": {
					String url = tagContent;
					ps.chatSession.predicates.put("url", url);
				}
				break;

			case "search":
				try {
					String url = String.format("http://www.google.com/#q=%s", URLEncoder.encode(tagContent, "utf-8"));
					ps.chatSession.predicates.put("url", url);
				} catch (UnsupportedEncodingException e) {
					Log.e(TAG, e.getMessage(), e);
				}
				break;

			case "location":
				try {
					String url = String.format("http://maps.google.com/?q=%s", URLEncoder.encode(tagContent, "utf-8"));
					ps.chatSession.predicates.put("url", url);
				} catch (UnsupportedEncodingException e) {
					Log.e(TAG, e.getMessage(), e);
				}
				break;

			case VOICE_FLAG:
			case NEURA_USER_ARRIVED_HOME:
			case NEURA_USER_LEFT_HOME:
			case NEURA_USER_ARRIVED_TO_WORK:
			case NEURA_USER_LEFT_WORK:
				ps.chatSession.predicates.put(nodeName, tagContent);
				break;

			default:
				break;
		}
		return tagContent;
	}
}
