package com.swiftype.android.search.backend;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.swiftype.android.search.IHandleAutoselects;
import com.swiftype.android.search.ISearch;
import com.swiftype.android.search.IShowDetails;
import com.swiftype.android.search.IWatchSearchRequests;

/**
 * This fragment handles all intents related to searches and suggestions, e.g. search
 * request or autoselects. The activity using this fragment has to implement
 * {@link IWatchSearchRequests}. Implementing {@link IShowDetails} is optional.
 *
 */
public class SearchWorkerFragment extends Fragment implements ISearch, IHandleAutoselects {
	private SearchServiceHelper searchService;
	private IWatchSearchRequests newSearchListener;
	private IShowDetails showDetailsListener;
	private String query = "";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		final Activity activity = getActivity();
		try {
			newSearchListener = (IWatchSearchRequests) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " should implement OnNewSearchListener.");
		}
		
		try {
			showDetailsListener = (IShowDetails) activity;
		} catch (ClassCastException e) {
			// optional
		}
		
		searchService = new SearchServiceHelper(activity);
		
		handleIntent(activity.getIntent());
	}
	
	public String getQuery() {
		return query;
	}
	
	public void search(final String query) {
		this.query = query; 
		searchService.search(query);
		newSearchListener.newSearchRequest(query);
	}
	
	public void autoselect(final String documentTypeName, final String query, final String identifier, final String id) {
		if (id != null && query != null) {
			searchService.autoselect(id, query);
			
			if (showDetailsListener != null && identifier != null) {
				showDetailsListener.showDetails(documentTypeName, identifier);
			}
		}
	}
	
	public void handleIntent(final Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			/**
			 * Handling search intents
			 */
			search(intent.getStringExtra(SearchManager.QUERY));
		} else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			/**
			 * Handling autoselects
			 */
			final String id = intent.getDataString();
			final String query = intent.getExtras().get(SearchManager.USER_QUERY).toString();
			final String extraData = intent.getExtras().getString(SearchManager.EXTRA_DATA_KEY);
			final int splitPoint = extraData.indexOf(' '); 
			final String documentTypeName = extraData.substring(0, splitPoint);
			final String identifier = extraData.substring(splitPoint + 1);
			
			autoselect(documentTypeName, identifier, id, query);
		}
	}
}
