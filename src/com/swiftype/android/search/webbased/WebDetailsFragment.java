package com.swiftype.android.search.webbased;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.swiftype.android.search.R;
import com.swiftype.android.search.ShowDetailsFragment;

public class WebDetailsFragment extends ShowDetailsFragment {
	private static final String FIELD_URL = "url";	
	private static final String PAGE_DOCUMENT_TYPE = "page";
	private static final String POSTS_DOCUMENT_TYPE = "posts";
	
	private WebView webView;
	private String url;
	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.web_result, null);
		
		if (webView != null) {
			webView.destroy();
		}

		webView = (WebView) rootView.findViewById(R.id.web_result);
		if (savedInstanceState != null) {
			
		} 
		
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		webView.setLayoutParams(params);
		
		webView.setWebChromeClient(new WebChromeClient()); 
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				return true;
			}
			
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				
				Log.i(this.getClass().getSimpleName(), "START LOADING: " + url);
			}
			
			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				
				Log.i(this.getClass().getSimpleName(), "FINISHED LOADING: " + url);
			}
			
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				super.onReceivedError(view, errorCode, description, failingUrl);
				
				Log.i(this.getClass().getSimpleName(), "ERROR CODE: " + errorCode);
				Log.i(this.getClass().getSimpleName(), "ERROR DESCRIPTION: " + description);
				Log.i(this.getClass().getSimpleName(), "ERROR URL: " + failingUrl);
			}
		});
		
		WebSettings webSettings = webView.getSettings();
		webSettings.setSavePassword(false);
		webSettings.setSaveFormData(false);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setLoadWithOverviewMode(true);
		webSettings.setUseWideViewPort(true);
		
		if (savedInstanceState != null) {
			url = savedInstanceState.getString(FIELD_URL);
		}
		
		update();

		return rootView;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString(FIELD_URL, url);
	}

	@Override
	public void showDetails(final String documentTypeName, final String identifier) {		
		if (!(documentTypeName.equals(PAGE_DOCUMENT_TYPE) || documentTypeName.equals(POSTS_DOCUMENT_TYPE))) {
			return;
		}
		
		url = identifier;
		
		update();
	}
	
	private void update() {
		if (webView != null && url != null) {
			webView.loadUrl(url);	
		}
	}
}
