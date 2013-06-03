package com.swiftype.api.search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Engine {
	private static final String LOG_ID = Engine.class.getSimpleName();
	
	private static final String SUGGEST_PATH = "engines/suggest.json";
	private static final String SEARCH_PATH = "engines/search.json";
	private static final String AUTOSELECT_PATH = "analytics/pas.json";
	private static final String CLICKTHROUGH_PATH = "analytics/pc.json";
	private static final String EMPTY_BODY = "";
	
	private final String engineKey;
	private final RestConnection apiConnection;
	private final SwiftypeQueryOptions defaultOptions;
	
	private volatile HttpUriRequest currentSearchRequest;
	private volatile HttpUriRequest currentSuggestRequest;
	
	public Engine(final String engineKey) {
		this(engineKey, SwiftypeQueryOptions.DEFAULT);
	}
	
	public Engine(final String engineKey, final SwiftypeQueryOptions defaultOptions) {
		this.engineKey = engineKey;
		this.apiConnection = new RestConnection();
		this.defaultOptions = defaultOptions;
	}
	
	public void search(final OnApiAnswerHandler handler, final String query) {
		search(handler, query, defaultOptions);
	}
	
	public void search(final OnApiAnswerHandler handler, final String query, final SwiftypeQueryOptions options) {
		currentSearchRequest = makeSearch(handler, SEARCH_PATH, query, options, currentSearchRequest);
	}
	
	public void suggest(final OnApiAnswerHandler handler, final String query) {
		suggest(handler, query, defaultOptions);
	}
	
	public void suggest(final OnApiAnswerHandler handler, final String query, final SwiftypeQueryOptions options) {
		currentSuggestRequest = makeSearch(handler, SUGGEST_PATH, query, options, currentSuggestRequest);
	}
	
	public void logAutoselect(final String externalId, final String prefix) {
		updateAnalytics(AUTOSELECT_PATH, engineKey, externalId, prefix, "prefix");
	}
	
	public void logClickthrough(final String externalId, final String query) {
		updateAnalytics(CLICKTHROUGH_PATH, engineKey, externalId, query, "q");
	}
	
	private void updateAnalytics(final String path, final String engineKey, final String externalId, final String query, final String queryParameterName) {
		try {
			final String requestPath = path + "?engine_key=" + engineKey + "&doc_id=" + externalId + "&" + queryParameterName + "=" + URLEncoder.encode(query, "UTF-8");
			Log.i(LOG_ID, "Update Analytics: " + requestPath);
			Thread analyticsUpdater = new Thread(new AnalyticsUpdater(requestPath));
			analyticsUpdater.start();
		} catch (UnsupportedEncodingException e) {
			Log.i(LOG_ID, "Unsupported Encoding: " + e.getMessage());
		} 
	}
	
	private HttpUriRequest makeSearch(final OnApiAnswerHandler handler, final String path, final String query, final SwiftypeQueryOptions options, HttpUriRequest previousRequest) {
		final String body = buildBody(query, options);
		if (body != EMPTY_BODY) {
			if (previousRequest != null) {
				Log.i(LOG_ID, "Abort request: " + previousRequest.getURI());
				previousRequest.abort();
			}

			previousRequest = apiConnection.post(path, body);
			Thread apiFetcher = new Thread(new SearchRequest(previousRequest, handler));
			apiFetcher.start();
		} 
		return previousRequest;
	}
	
	private String buildBody(final String query, final SwiftypeQueryOptions options) {
		JSONObject data = options.toJson();
		try {
			data.put("engine_key", engineKey);			
		} catch (JSONException e) {
			throw new IllegalArgumentException("Invalid engine key: " + e.getMessage());
		}
		try {
			data.put("q", query);
		} catch (JSONException e) {
			return EMPTY_BODY;
		}
		return data.toString();
	}
	
	private class AnalyticsUpdater implements Runnable {
		private final HttpUriRequest request;
		
		public AnalyticsUpdater(final String path) {
			request = apiConnection.get(path);
		}
		
		public void run() {
			apiConnection.execute(request);
		}
	}
	
	private class SearchRequest implements Runnable {
		private final String LOG_ID = SearchRequest.class.getSimpleName();
		
		private volatile HttpUriRequest request;
		private OnApiAnswerHandler handler;
		
		public SearchRequest(final HttpUriRequest request, final OnApiAnswerHandler handler) {
			this.request = request;
			this.handler = handler;
		}

		@Override
		public void run() {			
			String answer = apiConnection.execute(request);
			if (answer == null) {
				Log.i(LOG_ID, "No answer for request " + request.getURI() + " (aborted: " + request.isAborted() + " )");
				return;
			} else {
				Log.i(LOG_ID, "Answer: " + answer);
			}
			
			JSONObject results;
			try {
				results = new JSONObject(answer);
			} catch (JSONException e) {
				Log.i(LOG_ID, "Couldn't parse response: " + answer);
				results = new JSONObject();
			}
			handler.onApiAnswer(results);
		}
	}
	
	public interface OnApiAnswerHandler {
		public void onApiAnswer(final JSONObject response);
	}
}
