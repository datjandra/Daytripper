package com.vocifery.daytripper.ui.components;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebSettings;

import com.vocifery.daytripper.R;
import com.vocifery.daytripper.model.Locatable;
import com.vocifery.daytripper.model.Result;
import com.vocifery.daytripper.service.RequestConstants;
import com.vocifery.daytripper.util.StringConstants;

import java.util.ArrayList;
import java.util.List;

public class ResultFragment extends Fragment implements
		AbsListView.OnScrollListener, RequestConstants, StringConstants {

	private int currentPage;
	private int previousLastItem = 0;
	private Refreshable refreshable;
	private ArrayList<String> items;
	private WebView webView;
	
	public static final String RESULT_LIST_STATE = ResultFragment.class.getName() + "." + Result.class.getName();
	public static final String LIST_INSTANCE_STATE = "ListInstanceState";
	public static final String CURRENT_PAGE_STATE = "CurrentPageState";
	public static final String CHUNK_SIZE_STATE = "ChunkSizeState";
	public static final String TOTAL_STATE = "TotalState";
	public static final String PREVIOUS_LAST_ITEM_STATE = "PreviousLastItemState";
	public static final String SOURCE_STATE = "SourceState";
	public static final String ROUTE_STATE = "RouteState";
	public static final String INTENT_STATE = "IntentState";
	public static final String TAG = "ResultFragment";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_result,
				container, false);
		webView = (WebView) rootView.findViewById(R.id.webview);

		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setPluginState(WebSettings.PluginState.ON);

		webView.setWebViewClient(new WebViewClient());
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onDestroy() {
		this.refreshable = null;
		super.onDestroy();
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedState) {
	    super.onSaveInstanceState(savedState);
	}

	public void refreshList(String response, boolean reload) {
		if (reload) {
			items.clear();
		}
		items.add(response);
	}

	public void updateWebviewContent(String html) {
		webView.loadData(html, "text/html", "utf-8");
	}

	public void updateWebviewUrl(String url) {
		webView.loadUrl(url);
	}

	public void setRefreshable(Refreshable refreshable) {
		this.refreshable = refreshable;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.AbsListView.OnScrollListener#onScroll(android.widget.
	 * AbsListView, int, int, int)
	 */
	@Override
	public void onScroll(AbsListView absListView, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {}

	private class ViewHolder {
		TextView response;
	}
	
	private class SearchResultAdapter extends ArrayAdapter<String> {
		
		private String intent;
		private List<Locatable> route;
		
		public SearchResultAdapter(Context context) {
			super(context, R.layout.list_row, items);
			this.intent = intent;
			this.route = route;
		}
		
		@Override
        public int getCount() {
			if (items != null) {
				return items.size();
			}
			return 0;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			ViewHolder holder = null;
			
			if (row == null) {
				row = LayoutInflater.from(getContext()).inflate(R.layout.list_row, parent, false);
				holder = new ViewHolder();
				holder.response = (TextView) row.findViewById(R.id.response);
				holder.response.setClickable(true);
				holder.response.setMovementMethod(LinkMovementMethod.getInstance());
				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}
			holder.response.setText(Html.fromHtml(getItem(position)));
			return row;
		}
	}
	
	private void stripUnderlines(TextView textView) {
        Spannable s = (Spannable) textView.getText();
        URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
        for (URLSpan span: spans) {
            int start = s.getSpanStart(span);
            int end = s.getSpanEnd(span);
            s.removeSpan(span);
            span = new URLSpanNoUnderline(span.getURL());
            s.setSpan(span, start, end, 0);
        }
        textView.setText(s);
    }
	
	private class URLSpanNoUnderline extends URLSpan {
        public URLSpanNoUnderline(String url) {
            super(url);
        }
        
        @Override public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }
}
