package com.vocifery.daytripper.ui.components;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.vocifery.daytripper.R;

public class ResultFragment extends Fragment {

	private WebView webView;

	public static final String TAG = "ResultFragment";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_result,
				container, false);
		webView = (WebView) rootView.findViewById(R.id.webview);
		webView.setBackgroundColor(Color.TRANSPARENT);

		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setPluginState(WebSettings.PluginState.ON);

		webView.setWebViewClient(new WebViewClient());
		return rootView;
	}

	public void updateWebviewContent(String html) {
		webView.loadData(html, "text/html", "utf-8");
	}

	public void updateWebviewUrl(String url) {
		webView.loadUrl(url);
	}
}
