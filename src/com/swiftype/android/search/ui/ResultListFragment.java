package com.swiftype.android.search.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;

import com.swiftype.android.search.R;
import com.swiftype.android.search.SearchActivity;
import com.swiftype.android.search.SwiftypeConfig;
import com.swiftype.android.search.SwiftypeConfig.DocumentTypeConfig;
import com.swiftype.android.search.backend.SearchServiceHelper;
import com.swiftype.android.search.backend.SearchWorkerFragment;
import com.swiftype.android.search.helper.HighlightCursorAdapter;
import com.swiftype.android.search.helper.SearchContentProviderHelper;
import com.swiftype.android.search.helper.SwiftypeDbHelper;

public class ResultListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String PARAM_DOCUMENT_TYPE_NAME = "document_type";
	private static final String PARAM_MAX_RESULT_FIELD_LENGTH = "max_result_field_length";
	private static final String FIELD_QUERY = "query"; 
	
	private Uri uri;
	private HighlightCursorAdapter adapter;
	private CursorLoader cursorLoader;
	private SearchWorkerFragment searcher;
	private SearchServiceHelper searchService;
	private OnResultSelectListener resultSelectedListener;
	private OnNewResultsListener newResultsListener;
	private String documentTypeName;
	private int maxResultFieldLength;
	private String query;
	private SearchContentProviderHelper helper;
	private DocumentTypeConfig config;
	private View progressContainer;
	private View listContainer;
	private boolean listVisible;
	
	public static ResultListFragment newInstance(final String documentTypeName, final int maxResultFieldLength) {
		ResultListFragment fragment = new ResultListFragment();
		Bundle arguments = new Bundle();
		arguments.putString(PARAM_DOCUMENT_TYPE_NAME, documentTypeName);
		arguments.putInt(PARAM_MAX_RESULT_FIELD_LENGTH, maxResultFieldLength);
		fragment.setArguments(arguments);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle arguments = getArguments();
		this.documentTypeName = arguments.getString(PARAM_DOCUMENT_TYPE_NAME);
		this.maxResultFieldLength = arguments.getInt(PARAM_MAX_RESULT_FIELD_LENGTH);
		final Resources resources = getResources();
		config = new SwiftypeConfig(resources).getDocumentTypeConfig(documentTypeName);
		helper = new SearchContentProviderHelper(resources);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View rootView = inflater.inflate(R.layout.result_list, null);
		listContainer = rootView.findViewById(R.id.result_list_container);
		progressContainer = rootView.findViewById(R.id.progress_container);
		
		if (savedInstanceState != null) {
			query = savedInstanceState.getString(FIELD_QUERY);
		} else {
			listVisible = true;
		}
		
		return rootView;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		refresh();
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		searcher = (SearchWorkerFragment) getFragmentManager().findFragmentByTag(SearchActivity.TAG_SEARCHER);
	
		final Activity activity = getActivity();
		try {
			resultSelectedListener = (OnResultSelectListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnResultSelectListener.");
		}
		
		try {
			newResultsListener = (OnNewResultsListener) activity;
		} catch (ClassCastException e) {
			// optional
		}

		adapter = new HighlightCursorAdapter(activity,
											 config.getResultItemLayout(),
											 null,
											 config.getDisplayFields(),
											 config.getDisplayResources(),
											 0,
											 maxResultFieldLength);
		
		setListAdapter(adapter);
		
		showNothing();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		searchService = new SearchServiceHelper(activity);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString(FIELD_QUERY, query);
	}
	
	public void refresh() {
		setListNoAnimation(false);
		updateSearchUri();
		getLoaderManager().restartLoader(0, null, this);
	}
	
	private void updateSearchUri() {
		final String documentTypeName = getArguments().getString(PARAM_DOCUMENT_TYPE_NAME);
		query = searcher.getQuery();
		final String queryHash = helper.queryHash(query, searchService.getConfig().getQueryOptions().toString());
		uri = helper.documentTypeUri(documentTypeName, queryHash);
	}
	
	public String getTitle() {
		return getArguments().getString(PARAM_DOCUMENT_TYPE_NAME);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		final String externalId = getResultColumn(position, SwiftypeDbHelper.COLUMN_DOCUMENT_ID);
		
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		
		if (externalId != null && query != null) {
			searchService.clickthrough(externalId, query);
		}
		
		resultSelectedListener.onResultSelected(config.getName(), getResultColumn(position, config.getIdentifierField()));
	}

	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		setListNoAnimation(false);
		cursorLoader = new CursorLoader(getActivity(), uri, null, null, null, null); 
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		final boolean searching = SearchServiceHelper.isSearchInProgress();
		
		if (!searching) {
			if (query.length() == 0) {
				showNothing();
			} else if (cursor != null) {
				setListShown(true);
				newResults(cursor);
			}
		}

		adapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}
	
	private void newResults(final Cursor cursor) {
		if (newResultsListener != null && cursor != null) {
			int idColumn = cursor.getColumnIndex(config.getIdentifierField());
			if (idColumn != -1) { 
				String[] result_ids = new String[cursor.getCount()];
				for (int i = 0; i < result_ids.length; ++i) {
					cursor.moveToPosition(i);
					result_ids[i] = cursor.getString(idColumn);
				}
				newResultsListener.onNewResults(config.getName(), result_ids);
			}
		}
	}
	
	private String getResultColumn(final int position, final String columnName) {
		Cursor cursor = adapter.getCursor();
		cursor.moveToPosition(position);
		int wantedColumn = cursor.getColumnIndex(columnName);
		return (wantedColumn != -1) ? cursor.getString(wantedColumn) : "";
	}
	
	private void showNothing() {
		progressContainer.setVisibility(View.GONE);
		listContainer.setVisibility(View.GONE);
	}
	
	public void setListShown(final boolean shown, final boolean animate) {
		if (listVisible == shown) {
			return;
		}
		listVisible = shown;
		
		final Activity activity = getActivity();
		if (shown) {
			if (animate) {
				progressContainer.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.fade_out));
				listContainer.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.fade_in));
			}
			progressContainer.setVisibility(View.GONE);
			listContainer.setVisibility(View.VISIBLE);
		} else {
			if (animate) {
				progressContainer.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.fade_in));
				listContainer.startAnimation(AnimationUtils.loadAnimation(activity, android.R.anim.fade_out));
			}
			progressContainer.setVisibility(View.VISIBLE);
			listContainer.setVisibility(View.INVISIBLE);
		}
	}
	
	public void setListShown(final boolean shown) {
		setListShown(shown, true);
	}
	
	public void setListNoAnimation(final boolean shown) {
		setListShown(shown, false);
	}
	
	public interface OnResultSelectListener {
		public void onResultSelected(final String documentTypeName, final String identifier);
	}
	
	public interface OnNewResultsListener {
		public void onNewResults(final String documentTypeName, final String[] result_ids);
	}
}
