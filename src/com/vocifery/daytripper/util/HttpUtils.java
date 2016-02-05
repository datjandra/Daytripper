package com.vocifery.daytripper.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.util.Log;

import com.vocifery.daytripper.actions.Actionable;

@SuppressWarnings("deprecation")
public class HttpUtils {

	private final static String TAG = "HttpUtils";
	
	private HttpUtils() {}
	
	public static String doPost(String url, List<NameValuePair> nameValuePairs) {
		String jsonResponse = null;
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters,
				Actionable.CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParameters, Actionable.SOCKET_TIMEOUT);
		
		HttpClient httpClient = new DefaultHttpClient(httpParameters);
		HttpPost post = new HttpPost(url);
		try {
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = httpClient.execute(post);
			jsonResponse = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter writer = new PrintWriter(sw);
			e.printStackTrace(writer);
			Log.e(TAG, sw.toString());
		}
		return jsonResponse;
	}
}
