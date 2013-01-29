package com.swiftype.android.search.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.swiftype.android.search.IWatchSearchRequests;
import com.swiftype.android.search.R;
import com.swiftype.android.search.SwiftypeConfig;
import com.swiftype.android.search.helper.ResultListsAdapter;

public class ResultListsPagerFragment extends Fragment implements IWatchSearchRequests {
	private ResultListsAdapter adapter;
	private ViewPager pager;
	private PagerTabStrip tabs;
	private int visibility;
	private String query;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.result_lists_pager, null);
		
		final SwiftypeConfig config = new SwiftypeConfig(getResources());
		final String[] documentTypeNames = config.getDocumentTypeNames();
		adapter = new ResultListsAdapter(getFragmentManager(), R.id.results_pager, documentTypeNames, config.getMaxResultFieldLength());
		
		pager = (ViewPager) rootView.findViewById(R.id.results_pager);
		pager.setAdapter(adapter);
		tabs = (PagerTabStrip) rootView.findViewById(R.id.pager_title_strip);
		visibility = (documentTypeNames.length < 2) ? View.GONE : View.VISIBLE;
		
		if (query != null) {
			newSearchRequest(query);
		}

		return rootView;
	}

	@Override
	public void newSearchRequest(String query) {
		this.query = query;
		if (adapter != null) {
			adapter.newSearchRequest(query);
			adapter.notifyDataSetChanged();
			tabs.setVisibility(visibility);
		}
	}
}
