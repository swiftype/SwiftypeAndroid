package com.swiftype.android.search.ui;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.TextView;

import com.swiftype.android.search.R;
import com.swiftype.android.search.SearchActivity;
import com.swiftype.android.search.SwiftypeConfig;
import com.swiftype.android.search.backend.SearchWorkerFragment;
import com.swiftype.android.search.helper.HighlightCursorAdapter;
import com.swiftype.android.search.helper.SearchContentProviderHelper;
import com.swiftype.android.search.helper.SearchInputTextView;

public class SearchInputFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String LOG_ID = SearchInputFragment.class.getSimpleName();
	private static final String FIELD_SUGGESTS_ACTIVE = "suggests_active";
	
	private SwiftypeConfig config;
	private SearchContentProviderHelper searchContentProviderHelper;
	private SimpleCursorAdapter adapter;
	private CursorLoader cursorLoader;
	private SearchInputTextView inputSearch;
	private Uri uri;
	private SearchWorkerFragment searcher;
	private boolean suggestsActive;
	private boolean firstTextChangeAfterResume;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		
		config = new SwiftypeConfig(getResources());
		searchContentProviderHelper = new SearchContentProviderHelper(getResources());
		suggestsActive = false;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.search_input, null);
		inputSearch = (SearchInputTextView) rootView.findViewById(R.id.search_input_box);
		
		if (savedInstanceState != null) {
			firstTextChangeAfterResume = true;
			suggestsActive = savedInstanceState.getBoolean(FIELD_SUGGESTS_ACTIVE);
		}
		
		return rootView;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putBoolean(FIELD_SUGGESTS_ACTIVE, suggestsActive);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		final Activity activity = getActivity();
		inputSearch.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
		inputSearch.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
		inputSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH || (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
					dismissSuggests();

					adapter.swapCursor(null);
					
					hideKeyboard();
					
					final String query = getQuery();
					Log.i(LOG_ID, "Query: '" + query + "'");
					searcher.search(query);
				}
				return true;
			}
		});
		
		final TextWatcher textWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (firstTextChangeAfterResume && !suggestsActive) {
					firstTextChangeAfterResume = false;
					return;
				}
				
				suggestsActive = true;
				adapter.swapCursor(null);
				update();
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {
				// nothing
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// nothing 
			}
		};
 
		inputSearch.addTextChangedListener(textWatcher);
		
		inputSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				final String query = getQuery();

				dismissSuggests();
				hideKeyboard();
				
				Cursor cursor = (Cursor) adapter.getItem(position);
				final String extraData = getField(cursor, SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA);
				final int splitPoint = extraData.indexOf(' '); 
				final String documentTypeName = extraData.substring(0, splitPoint);
				final String identifier = extraData.substring(splitPoint + 1);
				final String documentId = getField(cursor, SearchManager.SUGGEST_COLUMN_INTENT_DATA);
				
				adapter.swapCursor(null);
				
				searcher.autoselect(documentTypeName, query, identifier, documentId);
			}
		});
		
		// TODO: make layout configurable
		adapter = new HighlightCursorAdapter(activity,
											 config.getSuggestLayout(),
											 null,
											 config.getSuggestColumns(),
											 config.getSuggestResources(),
											 0,
											 config.getMaxResultFieldLength());
		inputSearch.setAdapter(adapter);
	}
	
	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(inputSearch.getWindowToken(), 0);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		searcher = (SearchWorkerFragment) getFragmentManager().findFragmentByTag(SearchActivity.TAG_SEARCHER);
	}
	
	private void dismissSuggests() {
		suggestsActive = false;
		inputSearch.dismissDropDown();				
	}
	
	private void update() {
		uri = searchContentProviderHelper.suggestUri(getQuery());
		getLoaderManager().restartLoader(0, null, this);
	}
	
	private String getQuery() {
		return inputSearch.getText().toString();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		cursorLoader = new CursorLoader(getActivity(), uri, null, null, null, null);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (cursor != null && !cursor.isClosed()) {
			adapter.swapCursor(cursor);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		adapter.swapCursor(null);
	}
	
	private String getField(final Cursor cursor, final String fieldName) {
		int idColumn = cursor.getColumnIndex(fieldName);
		return (idColumn != -1) ? cursor.getString(idColumn) : "";
	}
}
