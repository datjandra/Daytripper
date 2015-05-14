package com.daytripper.app.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class UberAuthActivity extends FragmentActivity {

	private static final String REDIRECT_URI = "http://com.daytripper.app.redirectUrl";
	private static final String CODE ="code";
	
	private WebView webView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    webView.setWebViewClient(new WebViewClient(){
	          @Override
	          public void onPageFinished(WebView view, String url) {
	                
	          }
	          
	          @Override
	          public boolean shouldOverrideUrlLoading(WebView view, String authorizationUrl) {
	        	  if (authorizationUrl.startsWith(REDIRECT_URI)){
	        		  Uri uri = Uri.parse(authorizationUrl);
	        		  String code = uri.getQueryParameter(CODE);
	        		  if (code == null) {
	        			  return true;
	        		  }
	        	  }
	        	  return true;
	          }
	    });
	}
}
