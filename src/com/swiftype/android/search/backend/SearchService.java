package com.swiftype.android.search.backend;

import java.util.Date;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.swiftype.android.search.R;
import com.swiftype.android.search.SwiftypeConfig;
import com.swiftype.android.search.SwiftypeConfig.DocumentTypeConfig;
import com.swiftype.android.search.helper.ResultParser;
import com.swiftype.android.search.helper.SearchContentProviderHelper;
import com.swiftype.android.search.helper.SwiftypeDbHelper;
import com.swiftype.api.search.Engine;
import com.swiftype.api.search.Engine.OnApiAnswerHandler;
import com.swiftype.api.search.SwiftypeQueryOptions;

public class SearchService extends IntentService {
	public static final String PARAM_QUERY = "query";
	public static final String PARAM_OPTIONS = "options";
	public static final String PARAM_URI = "uri";
	public static final String PARAM_DOCUMENT_ID = "id";
	public static final String PARAM_REQUEST_TYPE = "request_type";
	
	public static final int REQUEST_SEARCH = 1;
	public static final int REQUEST_SUGGEST = 2;
	public static final int REQUEST_AUTOSELECT = 3;
	public static final int REQUEST_CLICKTHROUGH = 4;
	
	private static final String LOG_ID = SearchService.class.getSimpleName();
	private static final String NEEDS_UPDATE_QUERY_SELECTION = SwiftypeDbHelper.COLUMN_QUERY_HASH + " = ? AND " + SwiftypeDbHelper.COLUMN_SEARCH_TYPE + " = ?" + " AND " + SwiftypeDbHelper.COLUMN_TIMESTAMP + " > ?";
	
	private SwiftypeConfig config;
	private SearchContentProviderHelper helper;
	private Engine engine;
	
	public SearchService() {
		super("SwiftypeSearchServiceWorker");
		Log.i(LOG_ID, "Service Started.");
	}

	@SuppressLint("DefaultLocale") 
	@Override
	protected void onHandleIntent(final Intent searchIntent) {
		if (config == null) {
			final Resources resources = getResources();
			config = new SwiftypeConfig(resources);
			helper = new SearchContentProviderHelper(resources);
			final String engineKey = getString(R.string.engine_key);
			Log.i(LOG_ID, "Engine initialized with key: " + engineKey);
			engine = new Engine(engineKey);
		}
		
		final String query = searchIntent.getStringExtra(PARAM_QUERY).toLowerCase();
		final int requestType = searchIntent.getIntExtra(PARAM_REQUEST_TYPE, 0);
		switch (requestType) {
		case REQUEST_SUGGEST:
			executeSuggest(query, searchIntent);
			break;
		case REQUEST_SEARCH:
			executeSearch(query, searchIntent);
			break;
		case REQUEST_AUTOSELECT:
			engine.logAutoselect(searchIntent.getStringExtra(PARAM_DOCUMENT_ID), searchIntent.getStringExtra(PARAM_QUERY));
			break;
		case REQUEST_CLICKTHROUGH:
			engine.logClickthrough(searchIntent.getStringExtra(PARAM_DOCUMENT_ID), searchIntent.getStringExtra(PARAM_QUERY));
			break;
		default:
			throw new IllegalArgumentException();
		}
	}
	
	private void executeSearch(final String query, final Intent intent) {
		SwiftypeQueryOptions options;
		if (intent.hasExtra(PARAM_OPTIONS)) {
			options = SwiftypeQueryOptions.from(intent.getStringExtra(PARAM_OPTIONS));
		} else {
			options = config.getQueryOptions();
		}
		final String queryHash = helper.queryHash(query, options.toString());
		final Uri uri = helper.searchUri(queryHash);
		
		if (needsUpdate(queryHash, false)) {
			SearchApiRequestHandler handler = new SearchApiRequestHandler(uri, queryHash);
			engine.search(handler, query, options);
		} else {
			SearchServiceHelper.searchFinished(getContentResolver(), helper);
		}
	}
	
	private void executeSuggest(final String query, final Intent intent) {
		SwiftypeQueryOptions options;
		if (intent.hasExtra(PARAM_OPTIONS)) {
			options = SwiftypeQueryOptions.from(intent.getStringExtra(PARAM_OPTIONS));
		} else {
			options = config.getSuggestQueryOptions();
		}
		final String queryHash = helper.queryHash(query, options.toString());
		final Uri uri = helper.suggestUri(queryHash);
		
		if (needsUpdate(queryHash, true)) {
			SuggestApiRequestHandler handler = new SuggestApiRequestHandler(uri, queryHash);
			engine.suggest(handler, query, options);
		}
	}
	
	private boolean needsUpdate(final String queryHash, final boolean isSuggest) {
		String cutoffTimestamp = Long.valueOf(now() - SearchContentProvider.CACHE_TIME).toString();
		final Cursor cursor = getContentResolver().query(helper.searchStatusUri(queryHash),
				                                         null,
				                                         NEEDS_UPDATE_QUERY_SELECTION,
				                                         new String[] {queryHash, SearchContentProvider.searchStatusType(isSuggest), cutoffTimestamp},
				                                         null);

        Boolean hasValidResults = cursor.getCount() == 0;
        cursor.close();
        return hasValidResults;
	}
	
	private void updateSearchStatus(final String queryHash, final boolean isSuggest) {
		ContentValues values = new ContentValues();
		values.put(SwiftypeDbHelper.COLUMN_SEARCH_TYPE, SearchContentProvider.searchStatusType(isSuggest));
		values.put(SwiftypeDbHelper.COLUMN_TIMESTAMP, now());
		getContentResolver().insert(helper.searchStatusUri(queryHash), values);
	}
	
	private long now() {
		return new Date().getTime();
	}
	
	private class SearchApiRequestHandler implements OnApiAnswerHandler {
		private final Uri uri;
		private final String queryHash;
		
		public SearchApiRequestHandler(final Uri uri, final String queryHash) {
			this.uri = uri;
			this.queryHash = queryHash;
		}

		@Override
		public void onApiAnswer(final JSONObject response) {
			final ResultParser resultParser = new ResultParser(response);
			final ContentResolver resolver = getContentResolver();
			
			resolver.delete(uri, SwiftypeDbHelper.COLUMN_QUERY_HASH + " = ?", new String[] { queryHash });
			
			long timestamp = now();
			final String[] documentTypeNames = resultParser.getDocumentTypeNames();
			final int namesCount = documentTypeNames.length;
			final ContentValues[] infoRows = new ContentValues[namesCount];
			for (int i = 0; i < namesCount; ++i) {
				final String documentTypeName = documentTypeNames[i];
				final DocumentTypeConfig documentTypeConfig = config.getDocumentTypeConfig(documentTypeName);
				final ContentValues[] resultRows = resultParser.getResults(documentTypeName, documentTypeConfig.getSearchFields());
				
				for (ContentValues row : resultRows) {
					row.put(SwiftypeDbHelper.COLUMN_QUERY_HASH, queryHash);
					row.put(SwiftypeDbHelper.COLUMN_TIMESTAMP, timestamp);
				}
				
				resolver.bulkInsert(uri, resultRows);
				
				final ContentValues info = resultParser.getInfo(documentTypeName);
				info.put(SwiftypeDbHelper.COLUMN_QUERY_HASH, queryHash);
				info.put(SwiftypeDbHelper.COLUMN_TIMESTAMP, timestamp);
				infoRows[i] = info;
			}
			
			Uri resultStatusUri = helper.resultStatusUri(queryHash);
			resolver.bulkInsert(resultStatusUri, infoRows);
			
			updateSearchStatus(queryHash, false);
			
			SearchServiceHelper.searchFinished(resolver, helper);
		}
	}
	
	private class SuggestApiRequestHandler implements OnApiAnswerHandler {
		private final Uri uri;
		private final String queryHash;
		
		public SuggestApiRequestHandler(final Uri uri, final String queryHash) {
			this.uri = uri;
			this.queryHash = queryHash;
		}

		@Override
		public void onApiAnswer(final JSONObject response) {
			final ResultParser resultParser = new ResultParser(response);
			final ContentResolver resolver = getContentResolver();
			
			resolver.delete(uri, SwiftypeDbHelper.COLUMN_QUERY_HASH + " = ?", new String[] {queryHash});
			
			long timestamp = now();
			final String[] documentTypeNames = resultParser.getDocumentTypeNames();
			final int namesCount = documentTypeNames.length;
			for (int i = 0; i < namesCount; ++i) {
				final String documentTypeName = documentTypeNames[i];
				final DocumentTypeConfig documentTypeConfig = config.getDocumentTypeConfig(documentTypeName);
				final ContentValues[] resultRows = resultParser.getResults(documentTypeName,
						                                                   documentTypeConfig.getSuggestFields(),
						                                                   documentTypeConfig.getIdentifierField());
				
				for (final ContentValues row : resultRows) {
					row.put(SwiftypeDbHelper.COLUMN_QUERY_HASH, queryHash);
					row.put(SwiftypeDbHelper.COLUMN_TIMESTAMP, timestamp);
				}
				
				resolver.bulkInsert(uri, resultRows);
			}
			
			updateSearchStatus(queryHash, true);
			
			resolver.notifyChange(uri, null);
		}
	}
}
