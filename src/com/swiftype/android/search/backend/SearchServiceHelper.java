package com.swiftype.android.search.backend;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.swiftype.android.search.SwiftypeConfig;
import com.swiftype.android.search.helper.SearchContentProviderHelper;
import com.swiftype.api.search.SwiftypeQueryOptions;

public class SearchServiceHelper {
	private static volatile boolean searchInProgress = false;
	private final Context context;
	private final SwiftypeConfig config;
	private final SearchContentProviderHelper searchContentProviderHelper;

	public SearchServiceHelper(final Context context) {
		this.context = context;
		final Resources resources = context.getResources();
		this.config = new SwiftypeConfig(resources);
		this.searchContentProviderHelper = new SearchContentProviderHelper(resources);
	}
	
	/**
	 * Check if some search is currently in the process of checking and/or updating the results
	 */
	public static boolean isSearchInProgress() {
		return searchInProgress;
	}
	
	/**
	 * Start suggest API request and update SearchContentProvider if necessary
	 * 
	 * @param query		Suggest query
	 */
	public void suggest(final String query) {
		searchIntent(query, SearchService.REQUEST_SUGGEST, config.getSuggestQueryOptions());
	}
	
	/**
	 * Start search API request and update SearchContentProvider if necessary 
	 * 
	 * @param query		Search query
	 */
	public void search(final String query) {
		search(query, config.getQueryOptions());
	}
	
	
	/**
	 * Start search API request with custom options
	 * 
	 * @param query		Search query
	 * @param options	Search options (use search(query) to use default options)
	 */
	public void search(final String query, final SwiftypeQueryOptions options) {
		searchStarted();
		searchIntent(query, SearchService.REQUEST_SEARCH, options);
	}
		
	/**
	 * Register a click on the suggestions dropdown to Swiftype Analytics
	 * 
	 * @param id		Id of the selected suggest result
	 * @param prefix	Query used to provide this suggest result
	 */
	public void autoselect(final String id, final String prefix) {
		analyticsIntent(id, prefix, SearchService.REQUEST_AUTOSELECT);
	}
	
	/**
	 * Register a click on the search results to Swiftype Analytics
	 * 
	 * @param id		Id of the selected search result
	 * @param query		Query used for the search result
	 */
	public void clickthrough(final String id, final String query) {
		analyticsIntent(id, query, SearchService.REQUEST_CLICKTHROUGH);
	}
	
	/**
	 * @return	Current default config for all searches and suggestions
	 */
	public SwiftypeConfig getConfig() {
		return config;
	}
	
	static void searchFinished(final ContentResolver resolver, final SearchContentProviderHelper helper) {
		searchInProgress = false;
		resolver.notifyChange(helper.getSearchUpdateUri(), null);
	}
	
	private void searchStarted() {
		searchInProgress = true;
		context.getContentResolver().notifyChange(searchContentProviderHelper.getSearchUpdateUri(), null);
	}
	
	private void analyticsIntent(final String externalId, final String query, final int requestType) {
		Intent analyticsIntent = new Intent(context, SearchService.class);
		analyticsIntent.putExtra(SearchService.PARAM_DOCUMENT_ID, externalId);
		analyticsIntent.putExtra(SearchService.PARAM_QUERY, query);
		analyticsIntent.putExtra(SearchService.PARAM_REQUEST_TYPE, requestType);
		context.startService(analyticsIntent);
	}
	
	private void searchIntent(final String query, final int requestType, final SwiftypeQueryOptions options) {
		Intent searchIntent = new Intent(context, SearchService.class);
		searchIntent.putExtra(SearchService.PARAM_QUERY, query);
		searchIntent.putExtra(SearchService.PARAM_REQUEST_TYPE, requestType);
		if (options != SwiftypeQueryOptions.DEFAULT) {
			searchIntent.putExtra(SearchService.PARAM_OPTIONS, options.toString());
		}
		context.startService(searchIntent);
	}
}
