package com.swiftype.android.search.webbased;

import com.swiftype.android.search.SearchActivity;
import com.swiftype.android.search.ShowDetailsFragment;
import com.swiftype.android.search.ui.ResultListFragment.OnNewResultsListener;
import com.swiftype.android.search.ui.ResultListFragment.OnResultSelectListener;

public class WebSearchActivity extends SearchActivity implements OnResultSelectListener, OnNewResultsListener {
	@Override
	protected ShowDetailsFragment newDetailsFragment() {
		return new WebDetailsFragment();
	}
	
	@Override
	public void onResultSelected(final String documentTypeName, final String url) {
		showDetails(documentTypeName, url);
	}
	
	@Override
	public void onNewResults(final String documentTypeName, final String[] rankedUrls) {
		if (rankedUrls.length != 0) {
			// onShowDetails(rankedUrls[0]);
		}
	}
}
