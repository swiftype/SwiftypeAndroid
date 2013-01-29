package com.swiftype.android.search.helper;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.net.Uri;

import com.swiftype.android.search.R;

public class SearchContentProviderHelper {
	public static final int SEARCH_URI_ID = 1;
	public static final int SUGGEST_URI_ID = 2;
	public static final int SEARCH_STATUS_URI_ID = 3;
	public static final int DOCUMENT_TYPE_RESULT_URI_ID = 4;
	public static final int RESULT_STATUS_URI_ID = 5;
	public static final int DO_NOTHING_URI_ID = 99;
	
	private final String authority;
	
	private final String searchPath = "search";
	private final String suggestPath = SearchManager.SUGGEST_URI_PATH_QUERY;
	private final String searchStatusPath = "status";
	private final String documentTypeResultPath = "documentType";
	private final String resultStatusPath = "results_status";
	
	public final Uri searchUri;
	public final Uri suggestUri;
	public final Uri searchStatusUri;
	public final Uri documentTypeResultUri;
	public final Uri resultStatusUri;
	public final Uri searchUpdateUri;
	
	private final UriMatcher uriMatcher;
	
	public SearchContentProviderHelper(final Resources resources) {
		authority = resources.getString(R.string.search_content_provider_authority);
		
		searchUri = Uri.parse("content://" + authority + "/" + searchPath);
		suggestUri = Uri.parse("content://" + authority + "/" + suggestPath);
		searchStatusUri = Uri.parse("content://" + authority + "/" + searchStatusPath);
		documentTypeResultUri = Uri.parse("content://" + authority + "/" + documentTypeResultPath);
		resultStatusUri = Uri.parse("content://" + authority + "/" + resultStatusPath);
		
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(authority, documentTypeResultPath + "/*/*", DOCUMENT_TYPE_RESULT_URI_ID);
		uriMatcher.addURI(authority, documentTypeResultPath + "/*", DO_NOTHING_URI_ID);
		uriMatcher.addURI(authority, documentTypeResultPath, DO_NOTHING_URI_ID);
		uriMatcher.addURI(authority, searchPath, DO_NOTHING_URI_ID);
		uriMatcher.addURI(authority, searchPath + "/*", SEARCH_URI_ID);
		uriMatcher.addURI(authority, suggestPath, DO_NOTHING_URI_ID);
		uriMatcher.addURI(authority, suggestPath + "/*", SUGGEST_URI_ID);
		uriMatcher.addURI(authority, searchStatusPath + "/*", SEARCH_STATUS_URI_ID);
		uriMatcher.addURI(authority, resultStatusPath + "/*", RESULT_STATUS_URI_ID);
		
		searchUpdateUri = searchUri("");
	}	
	
	/**
	 * Generate query hash
	 * @param query		Search or suggest query
	 * @param options	SwiftypeQueryOptions as string
	 * @return
	 */
	@SuppressLint("DefaultLocale")
	public String queryHash(final String query, final String options) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(query.toLowerCase().getBytes());
			md.update(options.getBytes());
			return new BigInteger(1, md.digest()).toString(16);
		} catch (NoSuchAlgorithmException e) {
			return "SHA1_NOT_AVAILABLE";
		}
	}
	
	/**
	 * Generate Uri to watch for results of a specific document type, search query and options
	 * @param documentTypeName	Document type name
	 * @param query				Search query
	 * @param options			SwiftypeQueryOptions as string
	 * @return
	 */
	@SuppressLint("DefaultLocale")
	public Uri documentTypeUri(final String documentTypeName, final String queryHash) {
		return documentTypeResultUri.buildUpon().appendPath(documentTypeName).appendPath(queryHash).build();
	}
	
	/**
	 * Generate Uri to get suggestions for a specific query hash
	 * @param queryHash
	 * @return
	 */
	public Uri suggestUri(final String queryHash) {
		return buildUri(suggestUri, queryHash);
	}
	
	public UriMatcher getUriMatcher() {
		return uriMatcher;
	}
	
	public Uri searchUri(final String queryHash) {
		return buildUri(searchUri, queryHash);
	}
	
	public Uri searchStatusUri(final String query, final String options) {
		return searchStatusUri(queryHash(query, options));
	}
	
	public Uri searchStatusUri(final String queryHash) {
		return buildUri(searchStatusUri, queryHash);
	}
	
	public Uri resultStatusUri(final String query) {
		return buildUri(resultStatusUri, query);
	}
	
	private Uri buildUri(final Uri uri, final String path) {
		return uri.buildUpon().appendPath(path).build();
	}

	public Uri getSearchUpdateUri() {
		return searchUpdateUri;
	}
}
