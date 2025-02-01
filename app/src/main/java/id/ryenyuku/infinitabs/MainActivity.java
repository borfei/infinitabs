package id.ryenyuku.infinitabs;

import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.SharedPreferences;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.webkit.*;
import android.widget.*;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.net.URI;
import java.net.URLEncoder;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import kotlin.Suppress;


public class MainActivity extends AppCompatActivity {
	
	private FloatingActionButton _fab;
	private HashMap<String, Object> cachedTitle = new HashMap<>();
	private HashMap<String, Object> cachedThumb = new HashMap<>();
	private boolean isIncognito = false;
	private int displayedWV = -1;
	private boolean isLoading = false;
	private final int CHOOSE_FILE_REQUEST_CODE = 1;
	private ValueCallback<Uri[]> filePathCallback;
	private SwipeRefreshLayout swipelayout;
	private boolean isSearching = false;
	
	private ArrayList<HashMap<String, Object>> listQueries = new ArrayList<>();
	private  ArrayList<WebView> webviews = new ArrayList<>();
	
	private LinearLayout root_tabs;
	private LinearLayout root_webview;
	private LinearLayout linear_notab;
	private ListView tabs_list;
	private LinearLayout linear_t;
	private TextView sadface_text;
	private ImageView incognito_image;
	private TextView notab_text;
	private ImageView settings_button;
	private LinearLayout spacing_linear;
	private ImageView incognito_button;
	private LinearLayout tabscounter_linear1;
	private TextView tabscounter_text1;
	private CoordinatorLayout coordinatorlayout;
	private LinearLayout linear_findbar;
	private LinearLayout linear_chromebar;
	private LinearLayout wv_placeholder;
	private ListView query_list;
	private EditText findbar_input;
	private ImageView findbar_findprev_button;
	private ImageView findbar_findnext_button;
	private ImageView findbar_close_button;
	private ProgressBar wv_progressbar;
	private LinearLayout linear_searchbar;
	private EditText urlbar;
	private ImageView refresh_button;
	private ImageView forward_button;
	private LinearLayout tabscounter_linear2;
	private TextView tabscounter_text2;
	
	private SharedPreferences sharedPref;
	private RequestNetwork reqNet;
	private RequestNetwork.RequestListener _reqNet_request_listener;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.main);
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		_fab = findViewById(R.id._fab);
		
		root_tabs = findViewById(R.id.root_tabs);
		root_webview = findViewById(R.id.root_webview);
		linear_notab = findViewById(R.id.linear_notab);
		tabs_list = findViewById(R.id.tabs_list);
		linear_t = findViewById(R.id.linear_t);
		sadface_text = findViewById(R.id.sadface_text);
		incognito_image = findViewById(R.id.incognito_image);
		notab_text = findViewById(R.id.notab_text);
		settings_button = findViewById(R.id.settings_button);
		spacing_linear = findViewById(R.id.spacing_linear);
		incognito_button = findViewById(R.id.incognito_button);
		tabscounter_linear1 = findViewById(R.id.tabscounter_linear1);
		tabscounter_text1 = findViewById(R.id.tabscounter_text1);
		coordinatorlayout = findViewById(R.id.coordinatorlayout);
		linear_findbar = findViewById(R.id.linear_findbar);
		linear_chromebar = findViewById(R.id.linear_chromebar);
		wv_placeholder = findViewById(R.id.wv_placeholder);
		query_list = findViewById(R.id.query_list);
		findbar_input = findViewById(R.id.findbar_input);
		findbar_findprev_button = findViewById(R.id.findbar_findprev_button);
		findbar_findnext_button = findViewById(R.id.findbar_findnext_button);
		findbar_close_button = findViewById(R.id.findbar_close_button);
		wv_progressbar = findViewById(R.id.wv_progressbar);
		linear_searchbar = findViewById(R.id.linear_searchbar);
		urlbar = findViewById(R.id.urlbar);
		refresh_button = findViewById(R.id.refresh_button);
		forward_button = findViewById(R.id.forward_button);
		tabscounter_linear2 = findViewById(R.id.tabscounter_linear2);
		tabscounter_text2 = findViewById(R.id.tabscounter_text2);
		sharedPref = getSharedPreferences("data", Activity.MODE_PRIVATE);
		reqNet = new RequestNetwork(this);
		
		tabs_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				_goToWebScreen(_position);
			}
		});
		
		settings_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				Intent intent = new Intent();
				
				startActivity(intent);
			}
		});
		
		incognito_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				Intent intent = new Intent();
				intent.setClass(getApplicationContext(), MainActivity.class);
				intent.putExtra("incognito", true);
				startActivity(intent);
			}
		});
		
		query_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
				final int _position = _param3;
				urlbar.clearFocus();
				webviews.get(displayedWV).loadUrl(_validateUrl((String)listQueries.get(_position).get("query")));
			}
		});
		
		findbar_input.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				if (displayedWV == -1) {
					return;
				}
				webviews.get(displayedWV).findAllAsync(_charSeq);
				if (_charSeq.length() > 0) {
					findbar_findprev_button.setEnabled(true);
					findbar_findnext_button.setEnabled(true);
					findbar_findprev_button.setAlpha((float)(1));
					findbar_findnext_button.setAlpha((float)(1));
				}
				else {
					findbar_findprev_button.setEnabled(false);
					findbar_findnext_button.setEnabled(false);
					findbar_findprev_button.setAlpha((float)(0.5d));
					findbar_findnext_button.setAlpha((float)(0.5d));
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				
			}
			
			@Override
			public void afterTextChanged(Editable _param1) {
				
			}
		});
		
		findbar_findprev_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (isSearching && displayedWV != -1) {
					webviews.get(displayedWV).findNext(false);
				}
			}
		});
		
		findbar_findnext_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (isSearching && displayedWV != -1) {
					webviews.get(displayedWV).findNext(true);
				}
			}
		});
		
		findbar_close_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				isSearching = false;
				linear_findbar.setVisibility(View.GONE);
				linear_chromebar.setVisibility(View.VISIBLE);
				findbar_input.setText("");
			}
		});
		
		urlbar.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				if (urlbar.hasFocus()) {
					if (_charSeq.trim().length() > 0) {
						_refreshLocalQueries(_charSeq.trim());
						((BaseAdapter)query_list.getAdapter()).notifyDataSetChanged();
						if ((QUERY_MODE == 2) || ((QUERY_MODE == 1) && !isIncognito)) {
							try {
								reqNet.startRequestNetwork(RequestNetworkController.GET, String.format(QUERY_URL, URLEncoder.encode(_charSeq.trim(), StandardCharsets.UTF_8.name())), "searchQuery", _reqNet_request_listener);
							}
							catch (UnsupportedEncodingException e) {
								SketchwareUtil.showMessage(getApplicationContext(), e.getMessage());
							}
						}
					}
					else {
						listQueries.clear();
						((BaseAdapter)query_list.getAdapter()).notifyDataSetChanged();
					}
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				
			}
			
			@Override
			public void afterTextChanged(Editable _param1) {
				
			}
		});
		
		refresh_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				urlbar.clearFocus();
				if (displayedWV != -1) {
					if (isLoading) {
						webviews.get(displayedWV).stopLoading();
					}
					else {
						webviews.get(displayedWV).reload();
					}
				}
			}
		});
		
		forward_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				urlbar.clearFocus();
				if (displayedWV != -1 && webviews.get(displayedWV).canGoForward()) {
					webviews.get(displayedWV).goForward();
				}
			}
		});
		
		tabscounter_linear2.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View _view) {
				isSearching = true;
				linear_chromebar.setVisibility(View.GONE);
				linear_findbar.setVisibility(View.VISIBLE);
				return true;
			}
		});
		
		tabscounter_linear2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_goBackToTabs();
			}
		});
		
		_fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_newTab(NEWTAB_URL, null);
				if (VIEW_WV_ON_ADD) {
					_goToWebScreen(webviews.size() - 1);
				}
			}
		});
		
		_reqNet_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
				final String _tag = _param1;
				final String _response = _param2;
				final HashMap<String, Object> _responseHeaders = _param3;
				if (urlbar.getText().toString().trim().length() > 0) {
					listQueries.clear();
					_refreshLocalQueries(urlbar.getText().toString().trim());
					try {
						ArrayList<Object> objects = new Gson().fromJson(_response, new TypeToken<ArrayList<Object>>(){}.getType());
						for (String query : (ArrayList<String>)objects.get(1)) {
							if (!query.equals(urlbar.getText().toString())) {
								HashMap<String, Object> tmpHashMap = new HashMap<>();
								tmpHashMap.put("query", query);
								tmpHashMap.put("isURL", _isValidUrl(query) || (query.contains(".") && !query.contains(" ")));
								listQueries.add(0, tmpHashMap);
							}
						}
					} catch (Exception e) {
						 
					}
					((BaseAdapter)query_list.getAdapter()).notifyDataSetChanged();
				}
			}
			
			@Override
			public void onErrorResponse(String _param1, String _param2) {
				final String _tag = _param1;
				final String _message = _param2;
				if (urlbar.getText().toString().length() > 0) {
					listQueries.clear();
					_refreshLocalQueries(urlbar.getText().toString().trim());
					((BaseAdapter)query_list.getAdapter()).notifyDataSetChanged();
				}
			}
		};
	}
	
	private void initializeLogic() {
		_earlyInit();
		tabs_list.setAdapter(new ListAdapter(webviews));
		query_list.setAdapter(new Query_listAdapter(listQueries));
		isIncognito = getIntent().getExtras().getBoolean("incognito", false);
		if (isIncognito) {
			incognito_button.setVisibility(View.GONE);
			sadface_text.setVisibility(View.GONE);
		}
		else {
			incognito_image.setVisibility(View.GONE);
		}
		forward_button.setEnabled(false);
		forward_button.setAlpha((float)(0.5d));
		tabs_list.setVisibility(View.GONE);
		query_list.setVisibility(View.GONE);
		root_webview.setVisibility(View.GONE);
		linear_findbar.setVisibility(View.GONE);
	}
	
	@Override
	protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
		super.onActivityResult(_requestCode, _resultCode, _data);
		switch (_requestCode) {
				case CHOOSE_FILE_REQUEST_CODE :
				if (filePathCallback != null) {
						if (_resultCode == Activity.RESULT_OK) {
								filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(_resultCode, _data));
						}
						else {
								filePathCallback.onReceiveValue(null);
						}
						filePathCallback = null;
						break;
				}
		}
		switch (_requestCode) {
			
			default:
			break;
		}
	}

	@Override
	@Suppress(names = "Deprecated")
	public void onBackPressed() {
        super.onBackPressed();
        if (isSearching) {
			isSearching = false;
			linear_findbar.setVisibility(View.GONE);
			linear_chromebar.setVisibility(View.VISIBLE);
			findbar_input.setText("");
		}
		else {
			if (urlbar.hasFocus()) {
				urlbar.clearFocus();
			}
			else {
				if (displayedWV == -1) {
					finish();
				}
				else {
					if (webviews.get(displayedWV).canGoBack()) {
						webviews.get(displayedWV).goBack();
					}
					else {
						_goBackToTabs();
					}
				}
			}
		}
	}
	
	@Override
	protected void onPostCreate(Bundle _savedInstanceState) {
		super.onPostCreate(_savedInstanceState);
		if (!isIncognito) {
			cachedTitle = new Gson().fromJson(sharedPref.getString("cachedTitle", "{}"), new TypeToken<HashMap<String, Object>>(){}.getType());
			// Load saved tabs from shared preferences
			ArrayList<String> tabUrls = new Gson().fromJson(sharedPref.getString("lastTabs", "[]"), new TypeToken<ArrayList<String>>(){}.getType());
			for (String url : tabUrls) {
				_newTab(url, null);
			}
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (!isIncognito) {
			// Save title & thumbnail caches
			sharedPref.edit().putString("cachedTitle", new Gson().toJson(cachedTitle)).commit();
			// Renumerate webviews to get its url, save it to tabUrls, and store it to shared preferences
			ArrayList<String> tabUrls = new ArrayList<>();
			for (WebView webview : webviews) {
				tabUrls.add(webview.getUrl());
			}
			sharedPref.edit().putString("lastTabs", new Gson().toJson(tabUrls)).commit();
		}
	}
	

	public void _refreshTabsCount() {
		if (webviews.size() > 0) {
			linear_notab.setVisibility(View.GONE);
			tabs_list.setVisibility(View.VISIBLE);
		}
		else {
			tabs_list.setVisibility(View.GONE);
			linear_notab.setVisibility(View.VISIBLE);
		}
		if (webviews.size() > 99) {
			tabscounter_text2.setText(":D");
			tabscounter_text1.setText(":D");
		}
		else {
			tabscounter_text2.setText(String.valueOf(webviews.size()));
			tabscounter_text1.setText(String.valueOf(webviews.size()));
		}
	}
	
	
	public void _goBackToTabs() {
		displayedWV = -1;
		_fab.show();
		root_webview.setVisibility(View.GONE);
		root_tabs.setVisibility(View.VISIBLE);
		swipelayout.removeAllViews();
		wv_placeholder.removeAllViews();
		swipelayout = null;
	}
	
	
	public void _refreshLocalQueries(final String _url) {
		boolean validUrl = _isValidUrl(_url) || (_url.contains(".") && !_url.contains(" "));
		if (validUrl) {
			HashMap<String, Object> tmpHashMap = new HashMap<>();
			tmpHashMap.put("query", _url);
			tmpHashMap.put("isURL", true);
			if (listQueries.size() > 0) {
				listQueries.set(listQueries.size() - 1, tmpHashMap);
			}
			else {
				listQueries.add(tmpHashMap);
			}
		}
		HashMap<String, Object> tmpHashMap = new HashMap<>();
		tmpHashMap.put("query", _url);
		tmpHashMap.put("isURL", false);
		if (listQueries.size() > (validUrl? 1 : 0)) {
			listQueries.set(listQueries.size() - (validUrl? 2 : 1), tmpHashMap);
		}
		else {
			listQueries.add(0, tmpHashMap);
		}
	}
	
	
	public void _earlyInit() {
		urlbar.setOnEditorActionListener((view, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    if (displayedWV != -1 && (!view.getText().toString().trim().isEmpty())) {
                        webviews.get(displayedWV).loadUrl(_validateUrl(view.getText().toString().trim()));
                        view.clearFocus();
                    }

                    return true;
                }

                return false;
            });
		urlbar.setOnFocusChangeListener(new OnFocusChangeListener() {
			    @Override
			    public void onFocusChange(View view, boolean hasFocus) {
					if (hasFocus) {
						query_list.setVisibility(View.VISIBLE);
						if (!((TextView) view).getText().toString().trim().isEmpty()) {
							_refreshLocalQueries(((TextView)view).getText().toString().trim());
							((BaseAdapter)query_list.getAdapter()).notifyDataSetChanged();
							if (QUERY_MODE == 2 || (QUERY_MODE == 1 && !isIncognito)) {
								try {
									reqNet.startRequestNetwork(RequestNetworkController.GET, String.format(QUERY_URL, URLEncoder.encode(((TextView)view).getText().toString().trim(), StandardCharsets.UTF_8.name())), "searchQuery", _reqNet_request_listener);
								}
								catch (UnsupportedEncodingException e) {
									SketchwareUtil.showMessage(getApplicationContext(), e.getMessage());
								}
							}
						} else {
							listQueries.clear();
							((BaseAdapter)query_list.getAdapter()).notifyDataSetChanged();
						}
					}
				else {
					query_list.setVisibility(View.GONE);
					android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE); imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
					if (displayedWV != -1) {
						urlbar.setText(webviews.get(displayedWV).getUrl());
					}
					        }
				    }
		});
		// Below are some codes that is injected to parent area, it is NOT executed inside
	}
	
	private final String NEWTAB_URL = "https://www.google.com";
	private final String SEARCH_URL = "https://www.google.com/search?q=%s";
	private final String QUERY_URL = "https://www.google.com/complete/search?client=chrome&q=%s";
	private final String HTTP_PREFIX = " http://%s";
	private final int COOKIES_MODE = 1;
	private final int SAFEBROWSING_MODE = 1;
	private final int DOMSTORAGE_MODE = 1;
	private final int CACHE_MODE = 1;
	private final int DBAPI_MODE = 1;
	private final boolean ENABLE_AUTOPLAY = false;
	private final boolean ENABLE_JAVASCRIPT = true;
	private final int QUERY_MODE = 1;
	private final boolean VIEW_WV_ON_ADD = false;
	private final ArrayList<String> VALID_PROTOCOLS = new ArrayList<>(Arrays.asList("http", "https", "file", "ftp", "intent", "mailto", "about"));
	private final ArrayList<String> VALID_HOST_PROTOCOLS = new ArrayList<>(Arrays.asList("http", "https", "file", "ftp"));
	private DownloadListener webDownloadListener = new DownloadListener()
	   {
		  @Override  
		   public void onDownloadStart(String url, String userAgent,
		        String contentDisposition, String mimeType,
		        long contentLength) {
			    DownloadManager.Request request = new DownloadManager.Request(
			            Uri.parse(url));
			    request.setMimeType(mimeType);
			    String cookies = CookieManager.getInstance().getCookie(url);
			    request.addRequestHeader("cookie", cookies);
			    request.addRequestHeader("User-Agent", userAgent);
			    request.setDescription("Downloading file...");
			    request.setTitle(URLUtil.guessFileName(url, contentDisposition,
			            mimeType));
			    request.allowScanningByMediaScanner();
			    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			    request.setDestinationInExternalPublicDir(
			            Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
			                    url, contentDisposition, mimeType));
			    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
			    dm.enqueue(request);
			    Toast.makeText(getApplicationContext(), "Downloading File",
			            Toast.LENGTH_LONG).show();
		}};
	private WebViewClient webViewClient = new WebViewClient() {
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			((BaseAdapter)tabs_list.getAdapter()).notifyDataSetChanged();
			if (displayedWV != -1 && swipelayout.getChildAt(0) == view) {
				if (!urlbar.hasFocus()) {
					urlbar.setText(url);
				}
				if (webviews.get(displayedWV).canGoForward()) {
					forward_button.setEnabled(true);
					forward_button.setAlpha((float)(1));
				}
				else {
					forward_button.setEnabled(false);
					forward_button.setAlpha((float)(0.5d));
				}
				isLoading = true;
				refresh_button.setImageResource(R.drawable.ic_close_24);
				wv_progressbar.setVisibility(View.VISIBLE);
				wv_progressbar.setProgress(0);
			}
		}
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			((BaseAdapter)tabs_list.getAdapter()).notifyDataSetChanged();
			if (displayedWV != -1 && swipelayout.getChildAt(0) == view) {
				if (!urlbar.hasFocus()) {
					urlbar.setText(url);
				}
				if (webviews.get(displayedWV).canGoForward()) {
					forward_button.setEnabled(true);
					forward_button.setAlpha((float)(1));
				}
				else {
					forward_button.setEnabled(false);
					forward_button.setAlpha((float)(0.5d));
				}
				isLoading = false;
				refresh_button.setImageResource(R.drawable.ic_refresh_24);
				swipelayout.setRefreshing(false);
				wv_progressbar.setVisibility(View.INVISIBLE);
			}
		}
		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
			if (request.getUrl().getScheme().equals("intent")) {
				try {
					Intent intent = Intent.parseUri(request.getUrl().toString(), Intent.URI_INTENT_SCHEME);
					if (intent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
						startActivity(intent);
						return (true);
					}
					else {
						SketchwareUtil.showMessage(getApplicationContext(), "No supported application found to handle this operation.");
					}
				}
				catch (URISyntaxException e) {
				}
			}
			return false;
		}
	};
	private WebChromeClient webChromeClient = new WebChromeClient() {
		@Override
		public void onReceivedIcon(WebView view, Bitmap icon) {
			super.onReceivedIcon(view, icon);
			((BaseAdapter)tabs_list.getAdapter()).notifyDataSetChanged();
			cachedThumb.put(_stripUrl(view.getUrl(), false), icon);
		}
		
		@Override
		public void onReceivedTitle(WebView view, String title) {
			super.onReceivedTitle(view, title);
			((BaseAdapter)tabs_list.getAdapter()).notifyDataSetChanged();
			cachedTitle.put(_stripUrl(view.getUrl(), true), title);
		}
		
		@Override
		public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
			MainActivity.this.filePathCallback = filePathCallback;
			startActivityForResult(fileChooserParams.createIntent(), CHOOSE_FILE_REQUEST_CODE);
			return true;
		}
		
		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			if (displayedWV != -1 && wv_placeholder.getChildAt(0) == view) {
				wv_progressbar.setProgress(newProgress);
				if ((newProgress < 100) && !isLoading) {
					isLoading = true;
					refresh_button.setImageResource(R.drawable.ic_close_24);
					wv_progressbar.setVisibility(View.VISIBLE);
				}
			}
		}
		
		@Override
		public boolean onCreateWindow (WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
			if (!isUserGesture) {
				SketchwareUtil.showMessage(getApplicationContext(), "POPUP BLOCKED");
				return false;
			}
			swipelayout.removeAllViews();
			wv_placeholder.removeAllViews();
			swipelayout = null;
			_newTab(null, null);
			_goToWebScreen(webviews.size() - 1);
			WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
			transport.setWebView(webviews.get(webviews.size() - 1));
			resultMsg.sendToTarget();
			return true;
		}
	};
		public class ListAdapter extends BaseAdapter {
				ArrayList<WebView> _data;
				public ListAdapter(ArrayList<WebView> _arr) {
						_data = _arr;
				}
				
				@Override
				public int getCount() {
						return _data.size();
				}
				
				@Override
				public WebView getItem(int _index) {
						return _data.get(_index);
				}
				
				@Override
				public long getItemId(int _index) {
						return _index;
				}
		
				@Override
				public View getView(final int _position, View _view, ViewGroup _viewGroup) {
						LayoutInflater _inflater = getLayoutInflater();
						View _v = _view;
						if (_v == null) {
								_v = _inflater.inflate(R.layout.tabs, null);
						}
						
						final ImageView thumb_image = (ImageView) _v.findViewById(R.id.thumb_image);
						final TextView title_text = (TextView) _v.findViewById(R.id.title_text);
						final ImageView delete_button = (ImageView) _v.findViewById(R.id.delete_button);
			// Set the title and favicon to their webview's title and favicon
			title_text.setText(webviews.get(_position).getTitle());
			if (webviews.get(_position).getFavicon() == null) {
				thumb_image.setImageResource(R.drawable.ic_language_24);
			}
			else {
				thumb_image.setImageBitmap(webviews.get(_position).getFavicon());
			}
			delete_button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					webviews.get(_position).destroy();
					webviews.remove(_position);
					((BaseAdapter)tabs_list.getAdapter()).notifyDataSetChanged();
					_refreshTabsCount();
				}
			});
						return _v;
				}
	}
	
	
	public void _goToWebScreen(final int _position) {
		displayedWV = _position;
		_fab.hide();
		root_tabs.setVisibility(View.GONE);
		root_webview.setVisibility(View.VISIBLE);
		urlbar.setText(webviews.get(_position).getUrl());
		if (webviews.get(_position).canGoForward()) {
			forward_button.setEnabled(true);
			forward_button.setAlpha((float)(1.0d));
		}
		else {
			forward_button.setEnabled(false);
			forward_button.setAlpha((float)(0.5d));
		}
		isLoading = false;
		refresh_button.setImageResource(R.drawable.ic_refresh_24);
		wv_progressbar.setVisibility(View.INVISIBLE);
		wv_progressbar.setProgress(0);
		swipelayout = new SwipeRefreshLayout(MainActivity.this);
		swipelayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
		swipelayout.setOnRefreshListener(() -> {
			if (displayedWV != -1) {
				webviews.get(displayedWV).reload();
			}
		});
		swipelayout.addView(webviews.get(_position));
		wv_placeholder.addView(swipelayout);
		wv_placeholder.requestFocus();
	}

	
	public String _stripUrl(final String _url, final boolean _isDetailed) {
		URI uri = null;
		try {
			uri = new URI(_url);
			if (_isDetailed) {
				String tmpParsedUrl = "";
				if (uri.getAuthority() != null) {
					tmpParsedUrl = uri.getAuthority();
				}
				if (uri.getPath() != null && (!uri.getPath().equals("/") || uri.getQuery() != null)) {
					tmpParsedUrl = tmpParsedUrl.concat(uri.getPath());
				}
				if (uri.getQuery() != null) {
					tmpParsedUrl = tmpParsedUrl.concat(uri.getQuery());
				}

				return (tmpParsedUrl);
			} else {
				if (uri.getAuthority() == null) {
					if (uri.getPath() != null) {
						return (uri.getPath());
					}
				} else {
					return (uri.getAuthority());
				}
			}
		} catch (URISyntaxException ignored) {
		}
		return (_url);
	}
	
	
	public String _validateUrl(final String _url) {
		if (_isValidUrl(_url)) {
			return (_url);
		}
		else {
			if (_url.contains(".") && !_url.contains(" ")) {
				return (String.format(HTTP_PREFIX, _url));
			}
			else {
				try {
					String parsedUrl = URLEncoder.encode(_url, StandardCharsets.UTF_8.name());
					return String.format(SEARCH_URL, parsedUrl);
				}
				catch (UnsupportedEncodingException e) {
					SketchwareUtil.showMessage(getApplicationContext(), e.getMessage());
				}
			}
		}
		return ("about:blank");
	}
	
	
	public boolean _isValidUrl(final String _url) {
		int colon = _url.indexOf(":");
		if (colon < 3) return false;
		String proto = _url.substring((int)(0), (int)(colon)).toLowerCase();
		if (!VALID_PROTOCOLS.contains(proto)) return false;
		try {
			URI uri = new URI(_url);
			if (VALID_HOST_PROTOCOLS.contains(proto)) {
				if (uri.getHost() == null && uri.getPath() == null) return false;
			}
			return true;
		}
		catch (URISyntaxException e) {
		}
		return (false);
	}
	
	
	public void _newTab(final String _url, final WebView _view) {
		// Initialise a webview
		WebView webview;
		if (_view == null) {
			webview = new WebView(MainActivity.this);
		}
		else {
			webview = _view;
		}
		webview.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
		webview.setWebViewClient(webViewClient);
		webview.setWebChromeClient(webChromeClient);
		webview.setDownloadListener(webDownloadListener);
		// Do some settings
		WebSettings webSettings = webview.getSettings();
		CookieManager cookieManager = CookieManager.getInstance();
		if (isIncognito) {
			webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
			webSettings.setDomStorageEnabled(false);
			webSettings.setDatabaseEnabled(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                webSettings.setSafeBrowsingEnabled(false);
            }
            cookieManager.setAcceptCookie(false);
			cookieManager.setAcceptThirdPartyCookies(webview, false);
		}
		else {
			webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
			webSettings.setDomStorageEnabled(true);
			webSettings.setDatabaseEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                webSettings.setSafeBrowsingEnabled(true);
            }
            cookieManager.setAcceptCookie(true);
			cookieManager.setAcceptThirdPartyCookies(webview, true);
		}
		webSettings.setBuiltInZoomControls(true);
		webSettings.setDisplayZoomControls(false);
		webSettings.setMediaPlaybackRequiresUserGesture(!ENABLE_AUTOPLAY);
		webSettings.setJavaScriptEnabled(ENABLE_JAVASCRIPT);
		webSettings.setSupportMultipleWindows(true);
		// Load the url
		if (_url != null) {
			webview.loadUrl(_url);
		}
		// Add the webview to the array
		webviews.add(webview);
		((BaseAdapter)tabs_list.getAdapter()).notifyDataSetChanged();
		_refreshTabsCount();
	}
	
	public class Query_listAdapter extends BaseAdapter {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public Query_listAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public int getCount() {
			return _data.size();
		}
		
		@Override
		public HashMap<String, Object> getItem(int _index) {
			return _data.get(_index);
		}
		
		@Override
		public long getItemId(int _index) {
			return _index;
		}
		
		@Override
		public View getView(final int _position, View _v, ViewGroup _container) {
			LayoutInflater _inflater = getLayoutInflater();
			View _view = _v;
			if (_view == null) {
				_view = _inflater.inflate(R.layout.search_queries, null);
			}
			
			final LinearLayout linear_root = _view.findViewById(R.id.linear_root);
			final ImageView thumb_image = _view.findViewById(R.id.thumb_image);
			final LinearLayout linear_subroot = _view.findViewById(R.id.linear_subroot);
			final TextView title_text = _view.findViewById(R.id.title_text);
			final TextView subtitle_text = _view.findViewById(R.id.subtitle_text);
			
			title_text.setText(_data.get((int)_position).get("query").toString());
			if ((boolean)_data.get(_position).get("isURL")) {
				thumb_image.setImageResource(R.drawable.ic_language_24);
				String tmpParsedUrl = _validateUrl((String)_data.get(_position).get("query"));
				subtitle_text.setText(tmpParsedUrl);
				if (cachedTitle.containsKey(_stripUrl(tmpParsedUrl, true))) {
					title_text.setText(cachedTitle.get(_stripUrl(tmpParsedUrl, true)).toString());
				}
				if (cachedThumb.containsKey(_stripUrl(tmpParsedUrl, false))) {
					thumb_image.setImageBitmap((Bitmap)cachedThumb.get(_stripUrl(tmpParsedUrl, false)));
				}
				subtitle_text.setVisibility(View.VISIBLE);
			}
			else {
				thumb_image.setImageResource(R.drawable.ic_search_24);
				subtitle_text.setVisibility(View.GONE);
			}
			
			return _view;
		}
	}
}
