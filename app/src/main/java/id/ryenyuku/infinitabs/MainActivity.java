package id.ryenyuku.infinitabs;

import android.annotation.SuppressLint;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.SharedPreferences;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.*;
import android.widget.*;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.net.URI;
import java.net.URLEncoder;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
	private final static String HTTP_PREFIX = " http://%s";

	private final static ArrayList<String> VALID_PROTOCOLS = new ArrayList<>(Arrays.asList("http", "https", "file", "ftp", "intent", "mailto", "about"));
	private final static ArrayList<String> VALID_HOST_PROTOCOLS = new ArrayList<>(Arrays.asList("http", "https", "file", "ftp"));

	private boolean isIncognito = false;
	private boolean isLoading = false;
	private boolean isSearching = false;
	private int displayedWebView = -1;
	private ValueCallback<Uri[]> filePathCallback;
	
	private final ArrayList<HashMap<String, Object>> listQueries = new ArrayList<>();
	private final ArrayList<WebView> webViewList = new ArrayList<>();
	private final HashMap<String, Object> cachedThumb = new HashMap<>();
	private HashMap<String, Object> cachedTitle = new HashMap<>();

	private FloatingActionButton fab;
	private LinearLayout rootTabs;
	private LinearLayout rootWebView;
	private LinearLayout linearNoTab;
	private ListView tabsList;
    private TextView tabsCounterText;
	private SwipeRefreshLayout swipeLayout;
    private LinearLayout linearFindBar;
	private LinearLayout linearChromeBar;
	private LinearLayout webViewPlaceholder;
	private ListView queryList;
    private LinearProgressIndicator webViewProgressIndicator;
	private EditText findBarInput;
	private ImageView findBarFindPrevButton;
	private ImageView findBarFindNextButton;
    private EditText urlBarInput;
	private ImageView refreshButton;
	private ImageView forwardButton;
    private TextView webViewTabsCounterText;
	
	private SharedPreferences sharedPreferences;
	private RequestNetwork requestNetwork;
	private RequestNetwork.RequestListener requestNetworkRequestListener;

	private String newTabUrl = "https://www.google.com";
	private String searchUrl = "https://www.google.com/search?q=%s";
	private String queryUrl = "https://www.google.com/complete/search?client=chrome&q=%s";

	private final DownloadListener webDownloadListener = (url, userAgent, contentDisposition, mimeType, contentLength) -> {
		String cookies = CookieManager.getInstance().getCookie(url);
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

		request.setMimeType(mimeType);
		request.addRequestHeader("cookie", cookies);
		request.addRequestHeader("User-Agent", userAgent);
		request.setDescription("Downloading");
		request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
		request.allowScanningByMediaScanner();
		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
				URLUtil.guessFileName(url, contentDisposition, mimeType));

		DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		dm.enqueue(request);
	};
	private final WebViewClient webViewClient = new WebViewClient() {
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			((BaseAdapter)tabsList.getAdapter()).notifyDataSetChanged();

			if (displayedWebView != -1 && swipeLayout.getChildAt(0) == view) {
				if (!urlBarInput.hasFocus()) {
					urlBarInput.setText(url);
				}
				if (webViewList.get(displayedWebView).canGoForward()) {
					forwardButton.setEnabled(true);
					forwardButton.setAlpha(1.0f);
				} else {
					forwardButton.setEnabled(false);
					forwardButton.setAlpha(0.5f);
				}

				isLoading = true;
				refreshButton.setImageResource(R.drawable.ic_close_24);
				webViewProgressIndicator.setVisibility(View.VISIBLE);
				webViewProgressIndicator.setProgress(0);
			}
		}
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			((BaseAdapter)tabsList.getAdapter()).notifyDataSetChanged();

			if (displayedWebView != -1 && swipeLayout.getChildAt(0) == view) {
				if (!urlBarInput.hasFocus()) {
					urlBarInput.setText(url);
				}
				if (webViewList.get(displayedWebView).canGoForward()) {
					forwardButton.setEnabled(true);
					forwardButton.setAlpha(1.0f);
				} else {
					forwardButton.setEnabled(false);
					forwardButton.setAlpha(0.5f);
				}

				isLoading = false;
				refreshButton.setImageResource(R.drawable.ic_refresh_24);
				swipeLayout.setRefreshing(false);
				webViewProgressIndicator.setVisibility(View.INVISIBLE);
			}
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
			if (Objects.equals(request.getUrl().getScheme(), "intent")) {
				try {
					Intent intent = Intent.parseUri(request.getUrl().toString(), Intent.URI_INTENT_SCHEME);

					if (intent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
						startActivity(intent);
						return (true);
					} else {
						Toast.makeText(MainActivity.this, R.string.web_view_request_handler_not_found, Toast.LENGTH_LONG).show();
					}
				} catch (URISyntaxException ignored) {}
			}

			return false;
		}
	};
	private final WebChromeClient webChromeClient = new WebChromeClient() {
		@Override
		public void onReceivedIcon(WebView view, Bitmap icon) {
			super.onReceivedIcon(view, icon);
			((BaseAdapter)tabsList.getAdapter()).notifyDataSetChanged();
			cachedThumb.put(stripUrl(view.getUrl(), false), icon);
		}

		@Override
		public void onReceivedTitle(WebView view, String title) {
			super.onReceivedTitle(view, title);
			((BaseAdapter)tabsList.getAdapter()).notifyDataSetChanged();
			cachedTitle.put(stripUrl(view.getUrl(), true), title);
		}

		@Override
		public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
			MainActivity.this.filePathCallback = filePathCallback;
			fileChooserResultLauncher.launch(fileChooserParams.createIntent());
			return true;
		}

		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			if (displayedWebView != -1 && webViewPlaceholder.getChildAt(0) == view) {
				webViewProgressIndicator.setProgressCompat(newProgress, true);

				if ((newProgress < 100) && !isLoading) {
					isLoading = true;
					refreshButton.setImageResource(R.drawable.ic_close_24);
					webViewProgressIndicator.setVisibility(View.VISIBLE);
				}
			}
		}

		@Override
		public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
			if (!isUserGesture) {
				return false;
			}

			swipeLayout.removeAllViews();
			webViewPlaceholder.removeAllViews();
			swipeLayout = null;

			newTab(null);
			goToWebScreen(webViewList.size() - 1);

			WebView.WebViewTransport transport = (WebView.WebViewTransport)resultMsg.obj;
			transport.setWebView(webViewList.get(webViewList.size() - 1));
			resultMsg.sendToTarget();
			return true;
		}
	};
	private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
		@Override
		public void handleOnBackPressed() {
			if (isSearching) {
				isSearching = false;
				linearFindBar.setVisibility(View.GONE);
				linearChromeBar.setVisibility(View.VISIBLE);
				findBarInput.setText("");
			} else {
				if (urlBarInput.hasFocus()) {
					urlBarInput.clearFocus();
				} else {
					if (displayedWebView == -1) {
						finish();
					} else {
						if (webViewList.get(displayedWebView).canGoBack()) {
							webViewList.get(displayedWebView).goBack();
						} else {
							goBackToTabs();
						}
					}
				}
			}
		}
	};
	private final ActivityResultLauncher<Intent> fileChooserResultLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    final int _resultCode = result.getResultCode();
                    final Intent _data = result.getData();

                    if (filePathCallback != null) {
                        if (_resultCode == Activity.RESULT_OK) {
                            filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(_resultCode, _data));
                        } else {
                            filePathCallback.onReceiveValue(null);
                        }
                        filePathCallback = null;
                    }
                }
            }
	);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		EdgeToEdge.enable(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		fab = findViewById(R.id.fab);
		rootTabs = findViewById(R.id.root_tabs);
		rootWebView = findViewById(R.id.root_web_view);
		linearNoTab = findViewById(R.id.linear_no_tab);
		tabsList = findViewById(R.id.tabs_list);
		tabsCounterText = findViewById(R.id.tabs_counter_text);
		linearFindBar = findViewById(R.id.linear_find_bar);
		linearChromeBar = findViewById(R.id.linear_chrome_bar);
		webViewPlaceholder = findViewById(R.id.web_view_placeholder);
		queryList = findViewById(R.id.query_list);
		findBarInput = findViewById(R.id.find_bar_input);
		findBarFindPrevButton = findViewById(R.id.find_bar_find_prev_button);
		findBarFindNextButton = findViewById(R.id.find_bar_find_next_button);
		webViewProgressIndicator = findViewById(R.id.web_view_progress_indicator);
		urlBarInput = findViewById(R.id.url_bar_input);
		refreshButton = findViewById(R.id.refresh_button);
		forwardButton = findViewById(R.id.forward_button);
		webViewTabsCounterText = findViewById(R.id.web_view_tabs_counter_text);

		ImageView settingsButton = findViewById(R.id.settings_button);
		ImageView findBarCloseButton = findViewById(R.id.find_bar_close_button);

		TextView sadFaceText = findViewById(R.id.sad_face_text);
		ImageView incognitoImage = findViewById(R.id.incognito_image);
		ImageView incognitoButton = findViewById(R.id.incognito_button);

		sharedPreferences = getSharedPreferences("data", Activity.MODE_PRIVATE);
		requestNetwork = new RequestNetwork(this);

		getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

		ViewUtils.adjustPositionForSystemBarInsets(rootTabs, false, false, true, true);
		ViewUtils.adjustPositionForSystemBarInsets(rootWebView, false, false, true, true);
		ViewUtils.adjustPositionForSystemBarInsets(fab, false, false, false, true);

		tabsList.setOnItemClickListener((p1, p2, position, p4) -> goToWebScreen(position));

		settingsButton.setOnClickListener(_view -> {
			// TODO: Navigate user to settings activity here
		});

		incognitoButton.setOnClickListener(_view -> {
			Intent intent = new Intent();
			intent.setClass(getApplicationContext(), MainActivity.class);
			intent.putExtra("incognito", true);
			startActivity(intent);
		});

		queryList.setOnItemClickListener((p1, p2, position, p4) -> {
			final String rawQuery = (String) listQueries.get(position).get("query");
			final String validatedQuery = validateUrl(rawQuery);
			urlBarInput.clearFocus();
			webViewList.get(displayedWebView).loadUrl(validatedQuery);
		});

		findBarInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence charSequence, int _param2, int _param3, int count) {
				if (displayedWebView == -1) {
					return;
				}

				webViewList.get(displayedWebView).findAllAsync(charSequence.toString());

				if (count != 0) {
					findBarFindPrevButton.setEnabled(true);
					findBarFindNextButton.setEnabled(true);
					findBarFindPrevButton.setAlpha(1.0f);
					findBarFindNextButton.setAlpha(1.0f);
				} else {
					findBarFindPrevButton.setEnabled(false);
					findBarFindNextButton.setEnabled(false);
					findBarFindPrevButton.setAlpha(0.5f);
					findBarFindNextButton.setAlpha(0.5f);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {}

			@Override
			public void afterTextChanged(Editable p1) {}
		});

		findBarFindPrevButton.setOnClickListener(_view -> {
			if (isSearching && displayedWebView != -1) {
				webViewList.get(displayedWebView).findNext(false);
			}
		});

		findBarFindNextButton.setOnClickListener(_view -> {
			if (isSearching && displayedWebView != -1) {
				webViewList.get(displayedWebView).findNext(true);
			}
		});

		findBarCloseButton.setOnClickListener(_view -> {
			isSearching = false;
			linearFindBar.setVisibility(View.GONE);
			linearChromeBar.setVisibility(View.VISIBLE);
			findBarInput.setText("");
		});

		urlBarInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence charSequence, int p2, int p3, int p4) {
				final String url = charSequence.toString();

				if (urlBarInput.hasFocus()) {
					if (!url.trim().isEmpty()) {
						refreshLocalQueries(url.trim());
						((BaseAdapter)queryList.getAdapter()).notifyDataSetChanged();

						if (!isIncognito) {
							String encodedUrl = Uri.parse(url.trim()).buildUpon().build().toString();
							requestNetwork.startRequestNetwork(RequestNetworkController.GET,
									String.format(queryUrl, encodedUrl),
									"searchQuery", requestNetworkRequestListener);
						}
					} else {
						listQueries.clear();
						((BaseAdapter)queryList.getAdapter()).notifyDataSetChanged();
					}
				}
			}

			@Override
			public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {}

			@Override
			public void afterTextChanged(Editable p1) {}
		});

		refreshButton.setOnClickListener(_view -> {
			urlBarInput.clearFocus();

			if (displayedWebView != -1) {
				if (isLoading) {
					webViewList.get(displayedWebView).stopLoading();
				} else {
					webViewList.get(displayedWebView).reload();
				}
			}
		});

		forwardButton.setOnClickListener(_view -> {
			urlBarInput.clearFocus();

			if (displayedWebView != -1 && webViewList.get(displayedWebView).canGoForward()) {
				webViewList.get(displayedWebView).goForward();
			}
		});

		webViewTabsCounterText.setOnLongClickListener(_view -> {
			isSearching = true;
			linearChromeBar.setVisibility(View.GONE);
			linearFindBar.setVisibility(View.VISIBLE);
			return true;
		});

		webViewTabsCounterText.setOnClickListener(_view -> goBackToTabs());
		fab.setOnClickListener(_view -> newTab(newTabUrl));

		requestNetworkRequestListener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String p1, String response, HashMap<String, Object> headers) {
				if (!urlBarInput.getText().toString().trim().isEmpty()) {
					listQueries.clear();
					refreshLocalQueries(urlBarInput.getText().toString().trim());

					try {
						ArrayList<Object> objects = new Gson().fromJson(response, new TypeToken<ArrayList<Object>>(){}.getType());
						for (String query : (ArrayList<String>) objects.get(1)) {
							if (!query.equals(urlBarInput.getText().toString())) {
								HashMap<String, Object> tmpHashMap = new HashMap<>();
								tmpHashMap.put("query", query);
								tmpHashMap.put("isURL", isValidUrl(query) || (query.contains(".") && !query.contains(" ")));
								listQueries.add(0, tmpHashMap);
							}
						}
					} catch (Exception ignored) {}

					((BaseAdapter)queryList.getAdapter()).notifyDataSetChanged();
				}
			}

			@Override
			public void onErrorResponse(String p1, String message) {
				if (!urlBarInput.getText().toString().isEmpty()) {
					listQueries.clear();
					refreshLocalQueries(urlBarInput.getText().toString().trim());
					((BaseAdapter)queryList.getAdapter()).notifyDataSetChanged();
				}
			}
		};

		urlBarInput.setOnEditorActionListener((view, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
				if (displayedWebView != -1 && (!view.getText().toString().trim().isEmpty())) {
					final String validatedUrl = validateUrl(view.getText().toString().trim());
					webViewList.get(displayedWebView).loadUrl(validatedUrl);
					view.clearFocus();
				}

				return true;
			}

			return false;
		});

		urlBarInput.setOnFocusChangeListener((view, hasFocus) -> {
			if (hasFocus) {
				queryList.setVisibility(View.VISIBLE);

				if (!((TextView) view).getText().toString().trim().isEmpty()) {
					refreshLocalQueries(((TextView)view).getText().toString().trim());
					((BaseAdapter)queryList.getAdapter()).notifyDataSetChanged();
				} else {
					listQueries.clear();
					((BaseAdapter)queryList.getAdapter()).notifyDataSetChanged();
				}
			} else {
				queryList.setVisibility(View.GONE);
				InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

				if (displayedWebView != -1) {
					urlBarInput.setText(webViewList.get(displayedWebView).getUrl());
				}
			}
		});

		tabsList.setAdapter(new TabsListAdapter(webViewList));
		queryList.setAdapter(new QueryListAdapter(listQueries));
		isIncognito = false;

		try {
			isIncognito = Objects.requireNonNull(getIntent().getExtras()).getBoolean("incognito", isIncognito);
		} catch (NullPointerException exception) {
			Log.w("MainActivity", "An exception occurred while checking incognito mode: " + exception.getMessage());
		}

		if (isIncognito) {
			incognitoButton.setVisibility(View.GONE);
			sadFaceText.setVisibility(View.GONE);
		} else {
			incognitoImage.setVisibility(View.GONE);
		}

		forwardButton.setEnabled(false);
		forwardButton.setAlpha(0.5f);
		tabsList.setVisibility(View.GONE);
		queryList.setVisibility(View.GONE);
		rootWebView.setVisibility(View.GONE);
		linearFindBar.setVisibility(View.GONE);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		if (!isIncognito) {
			cachedTitle = new Gson().fromJson(sharedPreferences.getString("cachedTitle", "{}"), new TypeToken<HashMap<String, Object>>(){}.getType());
			ArrayList<String> tabUrls = new Gson().fromJson(sharedPreferences.getString("lastTabs", "[]"), new TypeToken<ArrayList<String>>(){}.getType());

			for (String url : tabUrls) {
				newTab(url);
			}
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();

		if (!isIncognito) {
			sharedPreferences.edit().putString("cachedTitle", new Gson().toJson(cachedTitle)).apply();
			ArrayList<String> tabUrls = new ArrayList<>();

			for (WebView webview : webViewList) {
				tabUrls.add(webview.getUrl());
			}

			sharedPreferences.edit().putString("lastTabs", new Gson().toJson(tabUrls)).apply();
		}
	}

	private void refreshTabsCount() {
		if (!webViewList.isEmpty()) {
			linearNoTab.setVisibility(View.GONE);
			tabsList.setVisibility(View.VISIBLE);
		}
		else {
			tabsList.setVisibility(View.GONE);
			linearNoTab.setVisibility(View.VISIBLE);
		}
		if (webViewList.size() > 99) {
			tabsCounterText.setText(getString(R.string.tab_counter_max_text));
			webViewTabsCounterText.setText(getString(R.string.tab_counter_max_text));
		}
		else {
			tabsCounterText.setText(String.valueOf(webViewList.size()));
			webViewTabsCounterText.setText(String.valueOf(webViewList.size()));
		}
	}

	private void goBackToTabs() {
		displayedWebView = -1;
		fab.show();
		rootWebView.setVisibility(View.GONE);
		rootTabs.setVisibility(View.VISIBLE);
		swipeLayout.removeAllViews();
		webViewPlaceholder.removeAllViews();
		swipeLayout = null;
	}

	private void refreshLocalQueries(String url) {
		boolean validUrl = isValidUrl(url) || (url.contains(".") && !url.contains(" "));

		if (validUrl) {
			HashMap<String, Object> tmpHashMap = new HashMap<>();
			tmpHashMap.put("query", url);
			tmpHashMap.put("isURL", true);

			if (!listQueries.isEmpty()) {
				listQueries.set(listQueries.size() - 1, tmpHashMap);
			} else {
				listQueries.add(tmpHashMap);
			}
		}

		HashMap<String, Object> tmpHashMap = new HashMap<>();
		tmpHashMap.put("query", url);
		tmpHashMap.put("isURL", false);

		if (listQueries.size() > (validUrl? 1 : 0)) {
			listQueries.set(listQueries.size() - (validUrl? 2 : 1), tmpHashMap);
		} else {
			listQueries.add(0, tmpHashMap);
		}
	}

	private void goToWebScreen(int position) {
		displayedWebView = position;
		fab.hide();

		rootTabs.setVisibility(View.GONE);
		rootWebView.setVisibility(View.VISIBLE);
		urlBarInput.setText(webViewList.get(position).getUrl());

		if (webViewList.get(position).canGoForward()) {
			forwardButton.setEnabled(true);
			forwardButton.setAlpha(1.0f);
		} else {
			forwardButton.setEnabled(false);
			forwardButton.setAlpha(0.5f);
		}

		isLoading = false;
		refreshButton.setImageResource(R.drawable.ic_refresh_24);
		webViewProgressIndicator.setVisibility(View.INVISIBLE);
		webViewProgressIndicator.setProgress(0);

		swipeLayout = new SwipeRefreshLayout(this);
		swipeLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
		swipeLayout.setOnRefreshListener(() -> {
			if (displayedWebView != -1) {
				webViewList.get(displayedWebView).reload();
			}
		});

		swipeLayout.addView(webViewList.get(position));
		webViewPlaceholder.addView(swipeLayout);
		webViewPlaceholder.requestFocus();
	}

	private String stripUrl(String url, boolean isDetailed) {
		URI uri;

		try {
			uri = new URI(url);

			if (isDetailed) {
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

				return tmpParsedUrl;
			} else {
				if (uri.getAuthority() == null) {
					if (uri.getPath() != null) {
						return uri.getPath();
					}
				} else {
					return uri.getAuthority();
				}
			}
		} catch (URISyntaxException ignored) {}

		return url;
	}

	private String validateUrl(String url) {
		if (isValidUrl(url)) {
			return url;
		} else {
			if (url.contains(".") && !url.contains(" ")) {
				return String.format(HTTP_PREFIX, url);
			} else {
                String parsedUrl = null;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    parsedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
                }
                return String.format(searchUrl, parsedUrl);
            }
		}
    }
	
	private boolean isValidUrl(String url) {
		int colon = url.indexOf(":");

		if (colon < 3) {
			return false;
		}

		String proto = url.substring(0, colon).toLowerCase();

		if (!VALID_PROTOCOLS.contains(proto)) {
			return false;
		}
		try {
			URI uri = new URI(url);

			if (VALID_HOST_PROTOCOLS.contains(proto)) {
                return uri.getHost() != null || uri.getPath() != null;
			}

			return true;
		} catch (URISyntaxException ignored) {
		}

		return false;
	}

	@SuppressLint("SetJavaScriptEnabled")
    private void newTab(String url) {
		WebView webView = new WebView(this);
		webView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
		webView.setWebViewClient(webViewClient);
		webView.setWebChromeClient(webChromeClient);
		webView.setDownloadListener(webDownloadListener);
		WebSettings webSettings = webView.getSettings();
		CookieManager cookieManager = CookieManager.getInstance();

		if (isIncognito) {
			webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
			webSettings.setDomStorageEnabled(false);
			webSettings.setDatabaseEnabled(false);
			cookieManager.setAcceptCookie(false);
			cookieManager.setAcceptThirdPartyCookies(webView, false);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				webSettings.setSafeBrowsingEnabled(false);
			}
		} else {
			webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
			webSettings.setDomStorageEnabled(true);
			webSettings.setDatabaseEnabled(true);
			cookieManager.setAcceptCookie(true);
			cookieManager.setAcceptThirdPartyCookies(webView, true);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				webSettings.setSafeBrowsingEnabled(true);
			}
		}

		webSettings.setBuiltInZoomControls(true);
		webSettings.setDisplayZoomControls(false);
		webSettings.setMediaPlaybackRequiresUserGesture(true);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportMultipleWindows(true);

		if (url != null) {
			webView.loadUrl(url);
		}


		webViewList.add(webView);
		((BaseAdapter)tabsList.getAdapter()).notifyDataSetChanged();
		refreshTabsCount();
	}

	private class TabsListAdapter extends BaseAdapter {
		ArrayList<WebView> data;

		public TabsListAdapter(ArrayList<WebView> array) {
			data = array;
		}

		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public WebView getItem(int index) {
			return data.get(index);
		}

		@Override
		public long getItemId(int index) {
			return index;
		}

		@Override
		public View getView(int position, View view, ViewGroup container) {
			LayoutInflater inflater = getLayoutInflater();

			if (view == null) {
				view = inflater.inflate(R.layout.adapter_list_tab, container, false);
			}

			final ImageView thumbImage = view.findViewById(R.id.thumb_image);
			final TextView titleText = view.findViewById(R.id.title_text);
			final ImageView closeButton = view.findViewById(R.id.close_button);
			titleText.setText(webViewList.get(position).getTitle());

			if (webViewList.get(position).getFavicon() == null) {
				thumbImage.setImageResource(R.drawable.ic_language_24);
			} else {
				thumbImage.setImageBitmap(webViewList.get(position).getFavicon());
			}
			closeButton.setOnClickListener(v1 -> {
				webViewList.get(position).destroy();
				webViewList.remove(position);
				((BaseAdapter)tabsList.getAdapter()).notifyDataSetChanged();
				refreshTabsCount();
			});

			return view;
		}
	}
	private class QueryListAdapter extends BaseAdapter {
		ArrayList<HashMap<String, Object>> data;
		
		public QueryListAdapter(ArrayList<HashMap<String, Object>> array) {
			data = array;
		}
		
		@Override
		public int getCount() {
			return data.size();
		}
		
		@Override
		public HashMap<String, Object> getItem(int index) {
			return data.get(index);
		}
		
		@Override
		public long getItemId(int index) {
			return index;
		}

        @Override
		public View getView(int position, View view, ViewGroup container) {
			LayoutInflater inflater = getLayoutInflater();

			if (view == null) {
				view = inflater.inflate(R.layout.adapter_list_query, container, false);
			}

			final ImageView thumbImage = view.findViewById(R.id.thumb_image);
            final TextView titleText = view.findViewById(R.id.title_text);
			final TextView subtitleText = view.findViewById(R.id.subtitle_text);
			final Object isUrl = Objects.requireNonNull(data.get(position).get("isURL"));

			titleText.setText(Objects.requireNonNull(data.get(position).get("query")).toString());

			if (data.get(position).containsKey("isURL") && (boolean)isUrl) {
				thumbImage.setImageResource(R.drawable.ic_language_24);
				String tmpParsedUrl = validateUrl((String)data.get(position).get("query"));
				subtitleText.setText(tmpParsedUrl);

				if (cachedTitle.containsKey(stripUrl(tmpParsedUrl, true))) {
					titleText.setText(Objects.requireNonNull(cachedTitle.get(stripUrl(tmpParsedUrl, true))).toString());
				}
				if (cachedThumb.containsKey(stripUrl(tmpParsedUrl, false))) {
					thumbImage.setImageBitmap((Bitmap)cachedThumb.get(stripUrl(tmpParsedUrl, false)));
				}

				subtitleText.setVisibility(View.VISIBLE);
			} else {
				thumbImage.setImageResource(R.drawable.ic_search_24);
				subtitleText.setVisibility(View.GONE);
			}
			
			return view;
		}
	}
}
