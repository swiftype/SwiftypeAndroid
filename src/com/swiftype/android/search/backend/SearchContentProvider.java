package com.swiftype.android.search.backend;

import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import com.swiftype.android.search.SwiftypeConfig;
import com.swiftype.android.search.helper.SearchContentProviderHelper;
import com.swiftype.android.search.helper.SwiftypeDbHelper;

public class SearchContentProvider extends ContentProvider {
	/**
	 * Results are cached for 5 minutes and updated, if older than that 
	 */
	public static final long CACHE_TIME = 5 * 60 * 1000;
	
	/**
	 * Suggest are only allowed for queries with a minimum of three characters 
	 */
	public static final int MIN_SUGGEST_CHARS = 3;
	
	private static final String LOG_ID = SearchContentProvider.class.getSimpleName();
	
	private static final String[] RESULT_STATUS_QUERY_COLUMNS = { SwiftypeDbHelper.COLUMN_DOCUMENT_TYPE, SwiftypeDbHelper.COLUMN_TOTAL_COUNT };
	private static final String RESULT_STATUS_QUERY_SELECTION = SwiftypeDbHelper.COLUMN_QUERY_HASH + " = ?";
	private static final String SUGGEST_NEEDS_UPDATE_QUERY_SELECTION = SwiftypeDbHelper.COLUMN_QUERY_HASH + " = ? AND " + SwiftypeDbHelper.COLUMN_SEARCH_TYPE + " = ?" + " AND " + SwiftypeDbHelper.COLUMN_TIMESTAMP + " > ?";

	private SearchServiceHelper searchServiceHelper;
	private SQLiteOpenHelper dbHelper;
	private SwiftypeConfig config;
	private SearchContentProviderHelper helper;
	
	@Override
	public boolean onCreate() {
		final Context context = getContext();
		searchServiceHelper = new SearchServiceHelper(context);
		dbHelper = new SwiftypeDbHelper(context);
		final Resources resources = context.getResources();
		config = new SwiftypeConfig(resources);
		helper = new SearchContentProviderHelper(resources);
		return true;
	}	

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		final String argument = extractArgument(uri);
		Cursor cursor = null;
		switch (helper.getUriMatcher().match(uri)) {
		/**
		 * Query for search results for a specific document type.
		 * To update the results if necessary use search on 
		 * SearchServiceHelper. The cursor for this query will be 
		 * notified on updates.
		 *  
		 * @param	uri		Uri with two path segments. The first
		 * specifies the document type and the second the query 
		 * hash computed from the query and the options.
		 */
		case SearchContentProviderHelper.DOCUMENT_TYPE_RESULT_URI_ID:
			final String documentType = extractDocumentType(uri);
			cursor = search(documentType, argument);
			break;
			
		/**
		 * Query for suggest results. Updates the suggestions if
		 * necessary and notifies the cursor.
		 * 
		 * @param	uri		Uri with one path segment representing
		 * the search query. Options are used from SwiftypeConfig.
		 */
		case SearchContentProviderHelper.SUGGEST_URI_ID:
			cursor = suggest(uri, argument);
			break;
		case SearchContentProviderHelper.SEARCH_STATUS_URI_ID:
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			cursor = db.query(SwiftypeDbHelper.TABLE_SEARCH_STATUS,
							  projection,
							  selection,
							  selectionArgs,
							  null, null, null);
			break;
		case SearchContentProviderHelper.RESULT_STATUS_URI_ID:
			cursor = dbHelper.getReadableDatabase().query(SwiftypeDbHelper.TABLE_RESULT_STATUS,
							                              RESULT_STATUS_QUERY_COLUMNS,
							                              RESULT_STATUS_QUERY_SELECTION,
							                              new String[] {argument},
							                              null, null, null);
			break;
		case SearchContentProviderHelper.DO_NOTHING_URI_ID:
			break;
		default:
			throw new IllegalArgumentException("Illegal Uri: " + uri.toString());
		}
		return cursor;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		int deletes = 0;
		switch (helper.getUriMatcher().match(uri)) {
		case SearchContentProviderHelper.SEARCH_URI_ID:
			for (final String documentTypeName : config.getDocumentTypeNames()) {
				final String tableName = SwiftypeDbHelper.searchTable(documentTypeName);
				db.delete(tableName, selection, selectionArgs);
			}
			break;
		case SearchContentProviderHelper.SUGGEST_URI_ID:
			for (final String documentTypeName : config.getDocumentTypeNames()) {
				final String tableName = SwiftypeDbHelper.suggestTable(documentTypeName);
				db.delete(tableName, selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException();
		}
		return deletes;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();

		switch (helper.getUriMatcher().match(uri)) {
		case SearchContentProviderHelper.SEARCH_URI_ID:
			final String searchTableName = SwiftypeDbHelper.searchTable(values.getAsString(SwiftypeDbHelper.COLUMN_DOCUMENT_TYPE));
			values.remove(SwiftypeDbHelper.COLUMN_DOCUMENT_TYPE);
			db.replace(searchTableName, null, values);
			break;
		case SearchContentProviderHelper.SUGGEST_URI_ID:
			final String suggestTableName = SwiftypeDbHelper.suggestTable(values.getAsString(SwiftypeDbHelper.COLUMN_DOCUMENT_TYPE));
			values.remove(SwiftypeDbHelper.COLUMN_DOCUMENT_TYPE);
			db.replace(suggestTableName, null, values);
			break;
		case SearchContentProviderHelper.SEARCH_STATUS_URI_ID:
			values.put(SwiftypeDbHelper.COLUMN_QUERY_HASH, extractArgument(uri));
			db.replace(SwiftypeDbHelper.TABLE_SEARCH_STATUS, null, values);
			break;
		case SearchContentProviderHelper.RESULT_STATUS_URI_ID:
			db.replace(SwiftypeDbHelper.TABLE_RESULT_STATUS, null, values);
			break;
		default:
			throw new IllegalArgumentException();
		}
		return uri;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int rowsUpdated = 0;
		switch (helper.getUriMatcher().match(uri)) {
		case SearchContentProviderHelper.SEARCH_STATUS_URI_ID:
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			rowsUpdated += db.update(SwiftypeDbHelper.TABLE_SEARCH_STATUS, values, selection, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException();
		}
		return rowsUpdated;
	}
	
	private Cursor search(final String documentTypeName, final String queryHash) {
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		final Cursor cursor = db.query(SwiftypeDbHelper.searchTable(documentTypeName),
							  		   concat(config.getDocumentTypeConfig(documentTypeName).getSearchFields(), SwiftypeDbHelper.COLUMN_ID),
							  		   SwiftypeDbHelper.COLUMN_QUERY_HASH + " = ?",
							  		   new String[] { queryHash },
							  		   null, null, null);
		
		Log.i(LOG_ID, "DocumentTypeName: " + documentTypeName);
		Log.i(LOG_ID, "Results (" + queryHash + ") " + cursor.getCount());
		
		cursor.setNotificationUri(getContext().getContentResolver(), helper.getSearchUpdateUri());
		return cursor;
	}
	
	private Cursor suggest(final Uri uri, final String query) {
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		final String[] documentTypeNames = config.getDocumentTypeNames();
		final Cursor[] cursors = new Cursor[documentTypeNames.length];
		for (int i = 0; i < cursors.length; ++i) {
			final Cursor cursor = cursorFor(db, documentTypeNames[i], query);
			cursors[i] = cursor;
		}
		
		final Cursor cursor = new MergeCursor(cursors);

		if (query.length() >= MIN_SUGGEST_CHARS) {
			if (cursor.getCount() == 0) {
				if (suggestNeedsUpdate(helper.queryHash(query, config.getSuggestQueryOptions().toString()))) {
					searchServiceHelper.suggest(query);
				}
			} else if (needsUpdate(cursor)) {
				searchServiceHelper.suggest(query);
			}
		}
		return cursor;
	}
	
	public static String searchStatusType(final boolean isSuggest) {
		return isSuggest ? "2" : "1";
	}
	
	private boolean suggestNeedsUpdate(final String queryHash) {
		final String cutoffTimestamp = Long.valueOf(cutOffTime()).toString();
		final SQLiteDatabase db = dbHelper.getReadableDatabase();
		final Cursor cursor = db.query(SwiftypeDbHelper.TABLE_SEARCH_STATUS,
				                       null,
				                       SUGGEST_NEEDS_UPDATE_QUERY_SELECTION,
					                   new String[] {queryHash, searchStatusType(true), cutoffTimestamp},
					                   null, null, null);
		return cursor.getCount() == 0;
	}
	
	private boolean needsUpdate(final Cursor cursor) {
		cursor.moveToFirst();
		final int column = cursor.getColumnIndex(SwiftypeDbHelper.COLUMN_TIMESTAMP);
		if (column != -1) {
			final long timestamp = cursor.getLong(column);
			if (timestamp < cutOffTime()) {
				return true;
			}
		}
		return false;
	}
	
	private String[] concat(final String[] array, final String ... values) {
		String[] newArray = new String[array.length + values.length];
		for (int i = 0; i < array.length; ++i) {
			newArray[i] = array[i];
		}
		for (int i = 0; i < values.length; ++i) {
			newArray[array.length + i] = values[i];
		}
		return newArray;
	}
	
	private Cursor cursorFor(final SQLiteDatabase db, final String documentTypeName, final String query) {
		final String queryHash = helper.queryHash(query, config.getSuggestQueryOptions().toString());
		SQLiteQueryBuilder suggest = config.getDocumentTypeConfig(documentTypeName).getSuggestQuery();
		final Cursor cursor = suggest.query(db,
				                            null,
				                            SwiftypeDbHelper.COLUMN_QUERY_HASH + " = ?",
				                            new String[] { queryHash },
				                            null, null, null);
		cursor.setNotificationUri(getContext().getContentResolver(), helper.suggestUri(queryHash));
		return cursor;
	}
	
	@SuppressLint("DefaultLocale")
	private String extractArgument(final Uri uri) {
		return uri.getLastPathSegment().toLowerCase();
	}
	
	private String extractDocumentType(final Uri uri) {
		final List<String> pathSegments = uri.getPathSegments();
		return pathSegments.get(pathSegments.size() - 2);
	}
	
	private long cutOffTime() {
		return new Date().getTime() - CACHE_TIME;
	}
}
