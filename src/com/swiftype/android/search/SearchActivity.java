package com.swiftype.android.search;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;

import com.swiftype.android.search.backend.SearchWorkerFragment;
import com.swiftype.android.search.ui.ResultListsPagerFragment;
import com.swiftype.android.search.ui.SearchInputFragment;

public abstract class SearchActivity extends FragmentActivity implements IWatchSearchRequests, IShowDetails {
	public static final String TAG_SEARCHER = "searcher";
	public static final String TAG_RESULTS = "results";
	public static final String TAG_DETAILS_LARGE_LANDSCAPE = "details_landscape";
	public static final String TAG_DETAILS_SMALL = "details_small";
	
	private static final String FIELD_CURRENT_QUERY = "current_query";
	private static final String FIELD_VISIBLE_DETAILS = "visible_details";
	private static final String FIELD_DETAILS_DOCUMENT_TYPE = "details_document_type";
	private static final String FIELD_DETAILS_IDENTIFIER = "details_identifier";
	
	private View detailsView;
	private ResultListsPagerFragment resultsPresenterFragment;
	private ShowDetailsFragment detailsFragment;
	private String currentQuery = "";
	private boolean hasVisibleDetails = false;
	private String detailsDocumentType;
	private String detailsIdentifier;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!showActivityTitle()) {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		setContentView(R.layout.activity_search);
		
		detailsView = (View) findViewById(R.id.details_container);
		
		final FragmentManager fragmentManager = getSupportFragmentManager();
		if (savedInstanceState == null) {
			final SearchWorkerFragment searchFragment = new SearchWorkerFragment();

			resultsPresenterFragment = new ResultListsPagerFragment();
			
			FragmentTransaction transaction = fragmentManager.beginTransaction();
			transaction.add(searchFragment, TAG_SEARCHER);
			transaction.add(R.id.search_input, new SearchInputFragment());
			transaction.add(R.id.result_list_container, resultsPresenterFragment, TAG_RESULTS);
			if (isLargeLandscapeScreen()) {
				transaction.add(R.id.details_container, newDetailsFragment(), TAG_DETAILS_LARGE_LANDSCAPE);
				hasVisibleDetails = true;
			}
			transaction.commit();
		} else {
			currentQuery = savedInstanceState.getString(FIELD_CURRENT_QUERY);
			hasVisibleDetails = savedInstanceState.getBoolean(FIELD_VISIBLE_DETAILS);
			detailsDocumentType = savedInstanceState.getString(FIELD_DETAILS_DOCUMENT_TYPE);
			detailsIdentifier = savedInstanceState.getString(FIELD_DETAILS_IDENTIFIER);
			
			resultsPresenterFragment = (ResultListsPagerFragment) fragmentManager.findFragmentByTag(TAG_RESULTS);

			if (isLargeLandscapeScreen()) {
				final Fragment fragment = fragmentManager.findFragmentById(R.id.details_container);
				findOrInitializeDetails(fragmentManager);
				if (fragment == null) {
					final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
					fragmentTransaction.add(R.id.details_container, detailsFragment, TAG_DETAILS_LARGE_LANDSCAPE);
					fragmentTransaction.commit();
				}
				hasVisibleDetails = true;
				
				if (detailsIdentifier != null) {
					detailsFragment.showDetails(detailsDocumentType, detailsIdentifier);
					detailsView.setVisibility(View.VISIBLE);
				}
			} else if (isLargeScreen()) {
				hasVisibleDetails = false;
			}
		}
	}
		
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (isLargeScreen()) {
			final FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}

		super.onSaveInstanceState(outState);
		
		outState.putString(FIELD_CURRENT_QUERY, currentQuery);
		outState.putBoolean(FIELD_VISIBLE_DETAILS, hasVisibleDetails);
		outState.putString(FIELD_DETAILS_DOCUMENT_TYPE, detailsDocumentType);
		outState.putString(FIELD_DETAILS_IDENTIFIER, detailsIdentifier);
	}
	
	protected boolean showActivityTitle() {
		return false;
	}
	
	@Override
	protected void onNewIntent(final Intent intent) {
		setIntent(intent);
		final SearchWorkerFragment searcher = (SearchWorkerFragment) getSupportFragmentManager().findFragmentByTag(TAG_SEARCHER);
		searcher.handleIntent(intent);
	}
	
	@Override
	public void newSearchRequest(String query) {
		showResults();
		currentQuery = query;
		resultsPresenterFragment.newSearchRequest(query);
	}
	
	private void showResults() {
		if (!isLargeLandscapeScreen()) {
			final FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			hasVisibleDetails = false;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!isLargeLandscapeScreen() && keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			hasVisibleDetails = false;
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public void showDetails(final String documentTypeName, String identifier) {
		// TODO: make show internal and external (via intent) configurable
		final FragmentManager fragmentManager = getSupportFragmentManager();
		findOrInitializeDetails(fragmentManager);
		
		Log.i(this.getClass().getSimpleName(), "Show details for " + documentTypeName + " with " + identifier);
		
		if (!hasVisibleDetails) {
			final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
			if (!isLargeLandscapeScreen()) {
				transaction.add(R.id.result_list_container, detailsFragment, TAG_DETAILS_SMALL);
				transaction.addToBackStack(null);
			}
			hasVisibleDetails = true;
			transaction.commit();
		}
		
		if (detailsView != null) {
			detailsView.setVisibility(View.VISIBLE);
		}
		
		// TODO: save last documentTypeName and identifier to show it on create
		detailsDocumentType = documentTypeName;
		detailsIdentifier = identifier;
		detailsFragment.showDetails(documentTypeName, identifier);
	}
	
	private void findOrInitializeDetails(final FragmentManager fragmentManager) {
		if (isLargeLandscapeScreen()) {
			detailsFragment = (ShowDetailsFragment) fragmentManager.findFragmentByTag(TAG_DETAILS_LARGE_LANDSCAPE);
		} else {
			detailsFragment = (ShowDetailsFragment) fragmentManager.findFragmentByTag(TAG_DETAILS_SMALL);
		}
		
		if (detailsFragment == null) {
			detailsFragment = newDetailsFragment();
		}
	}
	
	protected abstract ShowDetailsFragment newDetailsFragment();
	
	private boolean isLargeScreen() {
		final Configuration configuration = getResources().getConfiguration();
		return (configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE; 
	}
	
	private boolean isLargeLandscapeScreen() {
		final Configuration configuration = getResources().getConfiguration();
		return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && isLargeScreen();
	}
}
