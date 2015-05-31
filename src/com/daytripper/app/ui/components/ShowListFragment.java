package com.daytripper.app.ui.components;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daytripper.app.Daytripper;
import com.daytripper.app.R;
import com.daytripper.app.service.Actionable;
import com.daytripper.app.service.UberRequest;
import com.daytripper.app.service.UberRequestClient;
import com.daytripper.app.service.UberRequestConstants;
import com.daytripper.app.service.UberRequestListener;
import com.daytripper.app.util.ImageLoader;
import com.daytripper.app.util.ResourceUtils;
import com.daytripper.app.util.StringConstants;
import com.daytripper.app.vocifery.model.Locatable;
import com.daytripper.app.vocifery.model.QueryResponse;
import com.daytripper.app.vocifery.model.Result;
import com.daytripper.app.vocifery.model.Searchable;

public class ShowListFragment extends Fragment implements
		AbsListView.OnScrollListener, UberRequestConstants, StringConstants {

	private ListView listView;
	private ImageView attributionLogo;
	private ArrayList<Searchable> allItems;
	private SearchResultAdapter adapter;
	private String source;
	private int currentPage;
	private int total;
	private int chunkSize;
	private int previousLastItem = 0;
	private int selected;
	private Refreshable refreshable;
	private Map<String, String> sourceLogos;
	private ImageLoader imageLoader;
	private Parcelable listInstanceState;
	private ArrayList<Locatable> route;
	private String intent;
	private WebView webView;
	
	public static final String RESULT_LIST_STATE = ShowListFragment.class.getName() + "." + Result.class.getName();
	public static final String LIST_INSTANCE_STATE = "ListInstanceState";
	public static final String CURRENT_PAGE_STATE = "CurrentPageState";
	public static final String CHUNK_SIZE_STATE = "ChunkSizeState";
	public static final String TOTAL_STATE = "TotalState";
	public static final String PREVIOUS_LAST_ITEM_STATE = "PreviousLastItemState";
	public static final String SOURCE_STATE = "SourceState";
	public static final String ROUTE_STATE = "RouteState";
	public static final String INTENT_STATE = "IntentState";
	public static final String TAG = "ShowListFragment";
	
	private final static String CLIENT_ID = "Cshqu6pqTo9hPRF1Q1zwaAzQ8CuyZzBY";
	private final static String UBER_AUTH_URL = "https://login.uber.com/oauth/authorize?response_type=code&scope=request&client_id=" + CLIENT_ID;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		imageLoader = new ImageLoader(getActivity());
		imageLoader.addImageCache(getFragmentManager());
		
		sourceLogos = new HashMap<String, String>();
		final String[] vendors = getResources().getStringArray(
				R.array.vendor_array);
		final String[] logos = getResources()
				.getStringArray(R.array.logo_array);
		for (int i = 0; i < vendors.length; i++) {
			sourceLogos.put(vendors[i], logos[i]);
		}
		
		if (savedInstanceState != null) {
			ArrayList<Result> resultList = savedInstanceState.getParcelableArrayList(RESULT_LIST_STATE);
			if (resultList != null) {
				if (allItems == null) {
					allItems = new ArrayList<Searchable>();
				}
				allItems.addAll(resultList);
				
				final Daytripper daytripper = ((Daytripper) getActivity().getApplicationContext());
				daytripper.setAllItems(allItems);
			}
			
			ArrayList<Locatable> locatablelist = savedInstanceState.getParcelableArrayList(ROUTE_STATE);
			if (route == null) {
				if (route == null) {
					route = new ArrayList<Locatable>();
				}
				route.addAll(locatablelist);
			}
			
			listInstanceState = savedInstanceState.getParcelable(LIST_INSTANCE_STATE);
			chunkSize = savedInstanceState.getInt(CHUNK_SIZE_STATE, 0);
			currentPage = savedInstanceState.getInt(CURRENT_PAGE_STATE, 0);
			total = savedInstanceState.getInt(TOTAL_STATE, 0);
			previousLastItem = savedInstanceState.getInt(PREVIOUS_LAST_ITEM_STATE, 0);
			source = savedInstanceState.getString(SOURCE_STATE);
			intent = savedInstanceState.getString(INTENT_STATE);
		} else {
			allItems = new ArrayList<Searchable>();
			route = new ArrayList<Locatable>();
			
			final Daytripper daytripper = ((Daytripper) getActivity().getApplicationContext());
			daytripper.setAllItems(allItems);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.show_list_fragment,
				container, false);
		listView = (ListView) rootView.findViewById(R.id.list);
		listView.setOnScrollListener(this);
		
		attributionLogo = (ImageView) rootView
				.findViewById(R.id.attribution_logo);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Activity activity = getActivity();
		adapter = new SearchResultAdapter(activity, intent, route);
		listView.setAdapter(adapter);
		
		if (listInstanceState != null) {
			listView.onRestoreInstanceState(listInstanceState);
		}
		
		if (source != null && sourceLogos.containsKey(source)) {
			String uri = "@drawable/" + sourceLogos.get(source);
			int imageResource = getResources().getIdentifier(uri,
					"drawable", getActivity().getPackageName());
			if (imageResource > 0) {
				attributionLogo.setImageResource(imageResource);
			}
		}
		
		if (activity instanceof Refreshable) {
			this.setRefreshable((Refreshable) activity);
		}
	}
	
	@Override
	public void onDestroy() {
		imageLoader.clearCache();
		this.refreshable = null;
		final Daytripper daytripper = ((Daytripper) getActivity().getApplicationContext());
		daytripper.setAllItems(null);
		super.onDestroy();
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedState) {
	    super.onSaveInstanceState(savedState);
	    savedState.putInt(CURRENT_PAGE_STATE, currentPage);
	    savedState.putInt(CHUNK_SIZE_STATE, chunkSize);
	    savedState.putInt(PREVIOUS_LAST_ITEM_STATE, previousLastItem);
	    savedState.putInt(TOTAL_STATE, total);
	    savedState.putString(SOURCE_STATE, source);
	    savedState.putParcelableArrayList(RESULT_LIST_STATE, allItems);
	    savedState.putParcelableArrayList(ROUTE_STATE, route);
	    savedState.putParcelable(LIST_INSTANCE_STATE, listView.onSaveInstanceState());
	    savedState.putString(INTENT_STATE, intent);
	}

	public void setRefreshable(Refreshable refreshable) {
		this.refreshable = refreshable;
	}
	
	public void refreshList(QueryResponse queryResponse, boolean reload) {
		if (queryResponse != null && queryResponse.getTotal() > 0) {
			this.source = queryResponse.getSource();
			this.currentPage = queryResponse.getPage();
			this.total = queryResponse.getTotal();
			this.chunkSize = queryResponse.getChunk();
			this.intent = queryResponse.getIntent();
			
			Log.i(TAG, "page = " + this.currentPage);
			Log.i(TAG, "count = " + this.chunkSize);
			Log.i(TAG, "total = " + this.total);
			
			if (source != null && sourceLogos.containsKey(source)) {
				String uri = "@drawable/" + sourceLogos.get(source);
				int imageResource = getResources().getIdentifier(uri,
						"drawable", getActivity().getPackageName());
				if (imageResource > 0) {
					attributionLogo.setImageResource(imageResource);
				}
			}
		}
		
		List<Locatable> locatableList = queryResponse.getRoute();
		if (locatableList != null && !locatableList.isEmpty()) {
			if (reload) {
				route.clear();
			}
			route.addAll(locatableList);
			adapter.updateRoute(route);
		}
		adapter.updateIntent(intent);

		List<Result> resultList = queryResponse.getResultList();
		if (resultList != null && !resultList.isEmpty()) {
			if (reload) {
				allItems.clear();
				this.previousLastItem = 0;
			}
			allItems.addAll(resultList);
			adapter.notifyDataSetChanged();
		}
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
		int lastItem = firstVisibleItem + visibleItemCount;
		if (lastItem == totalItemCount) {
			if (previousLastItem != lastItem) {
				if ((total - totalItemCount) > 0) {
					currentPage = Math.max(currentPage, 1);
					Log.d(TAG,
							String.format("Loading page %d", currentPage + 1));
					if (refreshable != null) {
						refreshable.refresh(currentPage + 1, chunkSize);
					}
				}
				previousLastItem = lastItem;
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {}

	public void reset() {
		this.source = null;
		this.currentPage = 0;
		this.total = 0;
		this.chunkSize = 0;
		this.previousLastItem = 0;
		this.listInstanceState = null;
		this.attributionLogo.setImageDrawable(null);
		this.allItems = new ArrayList<Searchable>();
		this.route = new ArrayList<Locatable>();
		this.intent = null;
		this.adapter = new SearchResultAdapter(getActivity(), this.intent, this.route);
		this.listView.setAdapter(adapter);
		
		final Daytripper daytripper = ((Daytripper) getActivity().getApplicationContext());
		daytripper.setAllItems(allItems);
	}
	
	private Map<String,String> readPrefs() {
		Map<String,String> params = new HashMap<String,String>();
		SharedPreferences sharedPrefs = getActivity().getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
		Map<String,?> allPrefs = sharedPrefs.getAll();
		Set<String> entryKeys = allPrefs.keySet();
		for (String key : entryKeys) {
			params.put(key, allPrefs.get(key).toString());
		}
		return params;
	}
	
	private void startUberRequest(Locatable start, Locatable end, String productId) {
		Log.i(TAG, "startUberRequest()");
		Activity activity = getActivity();
		final UberRequestListener uberRequestListener = 
				(activity instanceof UberRequestListener ? (UberRequestListener) activity : null);
		final UberRequest uberRequest = new UberRequest();
		uberRequestListener.sendingRequest();
		
		final Dialog uberDialog = new Dialog(activity);
		uberDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		uberDialog.setContentView(R.layout.uber_web_content);
		uberDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				uberRequestListener.stopRequest();
			}
		});

		TextView uberClose = (TextView) uberDialog.findViewById(R.id.uber_close);
		uberClose.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				uberDialog.dismiss();
			}
		});
		
		webView = (WebView) uberDialog.findViewById(R.id.uber_view);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.i(TAG, "Override url -> " + url);
				Uri uri = Uri.parse(url);
				String code = uri.getQueryParameter(UberRequestConstants.PARAM_CODE);
				if (!TextUtils.isEmpty(code)) {
					uberRequest.setCode(code);
					new UberRequestTask(uberRequestListener).execute(uberRequest);
					return true;
				}
				return false;
			}
		});
		
		/*
		webView.setOnTouchListener(new View.OnTouchListener() {
	        @Override
	        public boolean onTouch(View v, MotionEvent event) {
	            switch (event.getAction()) {
	                case MotionEvent.ACTION_DOWN:
	                case MotionEvent.ACTION_UP:
	                    if (!v.hasFocus()) {
	                        v.requestFocus();
	                    }
	                    break;
	            }
	            return false;
	        }
	    });
	    */
		
		webView.setHorizontalScrollBarEnabled(true);
		webView.setVerticalScrollBarEnabled(true);
		webView.clearHistory();
	    webView.clearFormData();
	    webView.clearCache(true);
		
		WebSettings webSettings = webView.getSettings();
        webSettings.setSaveFormData(false);
        webSettings.setSavePassword(false);
		
        Map<String,String> prefs = readPrefs();
		if (prefs.containsKey(PARAM_REQUEST_ID)) {
    		uberRequest.setRequestId(prefs.get(PARAM_REQUEST_ID));
    	}
    	if (prefs.containsKey(PARAM_ACCESS_TOKEN)) {
    		uberRequest.setAccessToken(prefs.get(PARAM_ACCESS_TOKEN));
    	}
    	
    	uberRequest.setProductId(productId);
    	uberRequest.setStartLatitude(start.getLatitude().toString());
    	uberRequest.setEndLatitude(end.getLatitude().toString());
    	uberRequest.setStartLongitude(start.getLongitude().toString());
    	uberRequest.setEndLongitude(end.getLongitude().toString());
    	uberRequest.setVerb(VERB_CALL);
    	uberRequest.setObject(OBJECT_UBER);
    	uberRequest.setTestMode(true);
    	uberRequest.setMethod(HttpPost.METHOD_NAME);
        
    	try {
			String authUrl = String.format("%s&redirect_uri=%s", UBER_AUTH_URL, URLEncoder.encode(Actionable.ENTITY_ACTION_URL, "UTF-8"));
			Log.i(TAG, "authUrl -> " + authUrl);
			webView.loadUrl(authUrl);
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, e.getMessage());
		}   
    	uberDialog.show();
    	
    	/*
		AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle);
		builder.setCustomTitle(customTitle);
		builder.setView(webView);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
		        uberRequestListener.stopRequest();
		    }
		});					
		builder.show();
		*/
	}
	
	private void showUberRequest(String jsonString) throws JSONException {
		Log.i(TAG, "showUberRequest()");
		JSONObject json = new JSONObject(jsonString);
		
		String requestId = json.getString(FIELD_REQUEST_ID);
		String accessToken = json.getString(FIELD_ACCESS_TOKEN);
		SharedPreferences sharedPrefs = getActivity().getSharedPreferences(Daytripper.class.getName(), Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putString(FIELD_REQUEST_ID, requestId);
		editor.putString(FIELD_ACCESS_TOKEN, accessToken);
		editor.commit();
		
		String messageTemplate = ResourceUtils.readTextFromResource(getActivity(), R.raw.message_template);
		webView.loadData(String.format(messageTemplate, requestId), "text/html", "UTF-8");
	}
	
	private class ViewHolder {
		TextView name;
		TextView reviews;
		TextView price;
		TextView snippet;
		ImageView photoOne;
		ImageView photoTwo;
		ImageView ratingImage;
		LinearLayout ratingsContainer;
		RelativeLayout itemFooter;
		Button goButton;
	}
	
	private class SearchResultAdapter extends ArrayAdapter<Searchable> {
		
		private String intent;
		private List<Locatable> route;
		
		public SearchResultAdapter(Context context, String intent, List<Locatable> route) {
			super(context, R.layout.list_row, allItems);
			this.intent = intent;
			this.route = route;
		}
		
		public void updateRoute(List<Locatable> route) {
			this.route = route;
		}
		
		public void updateIntent(String intent) {
			this.intent = intent;
		}
		
		@Override
        public int getCount() {
			if (allItems != null) {
				return allItems.size();
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
				holder.name = (TextView) row.findViewById(R.id.name);
				holder.name.setClickable(true);
				holder.name.setMovementMethod(LinkMovementMethod.getInstance());
				holder.photoOne = (ImageView) row.findViewById(R.id.photo_one);
				holder.photoTwo = (ImageView) row.findViewById(R.id.photo_two);
				holder.reviews = (TextView) row.findViewById(R.id.rating_count);
				holder.ratingImage = (ImageView) row.findViewById(R.id.rating_img);
				holder.price = (TextView) row.findViewById(R.id.price);
				holder.snippet = (TextView) row.findViewById(R.id.snippet);
				holder.ratingsContainer = (LinearLayout) row.findViewById(R.id.ratingsContainer);
				holder.itemFooter = (RelativeLayout) row.findViewById(R.id.item_footer);
				holder.goButton = (Button) row.findViewById(R.id.go_button);
				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}
			
			final Searchable result = getItem(position);
			String imageOneUrl = result.getImageOneUrl();
			if (imageOneUrl != null && !imageOneUrl.equals("null")) {
				imageLoader.loadImage(imageOneUrl, holder.photoOne);
				holder.photoOne.setVisibility(View.VISIBLE);
			} else {
				holder.photoOne.setVisibility(View.GONE);
			}
			
			String imageTwoUrl = result.getImageTwoUrl();
			if (imageTwoUrl != null && !imageTwoUrl.equals("null")) {
				imageLoader.loadImage(imageTwoUrl, holder.photoTwo);
				holder.photoTwo.setVisibility(View.VISIBLE);
			} else {
				holder.photoTwo.setVisibility(View.GONE);
			}
			
			holder.name.setText(Html.fromHtml(
					String.format(Locale.getDefault(), 
							"%d.  <a href='%s'>%s</a>", 
							position + 1, result.getMobileUrl(), result.getName())));
			holder.price.setText(result.getDeal());
			
			StringBuilder builder = new StringBuilder();
			String addressOne = result.getAddressOne();
			if (!TextUtils.isEmpty(addressOne)) {
				builder.append(addressOne + NEWLINE);
			}
			
			String addressTwo = result.getAddressTwo();
			if (!TextUtils.isEmpty(addressTwo)) {
				builder.append(addressTwo + NEWLINE);
			}
			
			String details = result.getDetails();
			if (!TextUtils.isEmpty(details)) {
				builder.append(details + NEWLINE);
			}
			
			String snippet = builder.toString().trim();
			if (!TextUtils.isEmpty(snippet)) {
				holder.snippet.setText(builder.toString().trim());
				holder.snippet.setVisibility(View.VISIBLE);
			} else {
				holder.snippet.setVisibility(View.GONE);
			}
			
			
			/*
			if (intent != null && intent.equals(Intention.PRICES.getValue())) {
				holder.itemFooter.setVisibility(View.VISIBLE);
				holder.goButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						Locatable start = route.get(0);
						Locatable end = route.get(route.size()-1);
						startUberRequest(start, end, result.getId());
					}
				});
			} else {
				holder.itemFooter.setVisibility(View.GONE);
			}
			*/
			
			String ratingImageUrl = result.getRatingImgUrl();
			Integer reviewCount = result.getReviewCount();
			if (TextUtils.isEmpty(ratingImageUrl) || ratingImageUrl.equals("null") || reviewCount == null || reviewCount.intValue() == 0) {
				holder.ratingsContainer.setVisibility(View.GONE);
			} else {
				imageLoader.loadImage(ratingImageUrl, holder.ratingImage);
				holder.ratingsContainer.setVisibility(View.VISIBLE);
				holder.reviews.setText("(" + result.getReviewCount().toString() + ")");
			}
			return row;
		}
	}
	
	private class UberRequestTask extends AsyncTask<UberRequest, Void, String> {

		private UberRequestListener uberRequestListener;
		
		private UberRequestTask(UberRequestListener uberRequestListener) {
			this.uberRequestListener = uberRequestListener;
		}
		
		@Override
    	protected void onPostExecute(String jsonResponse) {
			if (uberRequestListener != null) {
				uberRequestListener.onRequestSent();
			}
			try {
				showUberRequest(jsonResponse);
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
				if (uberRequestListener != null) {
					uberRequestListener.onNoResponse();
				}
			}
		}
		
		@Override
		protected String doInBackground(UberRequest... params) {
			return UberRequestClient.doPost(params[0]);
		}
	}
}
