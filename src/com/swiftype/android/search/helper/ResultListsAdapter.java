package com.swiftype.android.search.helper;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.swiftype.android.search.IWatchSearchRequests;
import com.swiftype.android.search.ui.ResultListFragment;

public class ResultListsAdapter extends FragmentPagerAdapter implements IWatchSearchRequests {
	private FragmentManager fragmentManager;
	private int viewId;
	private String[] documentTypeNames;
	private int maxResultFieldLength;
	
	public ResultListsAdapter(final FragmentManager fragmentManager, final int viewId, final String[] documentTypeNames, final int maxResultFieldLength) {
		super(fragmentManager);
		
		this.fragmentManager = fragmentManager;
		this.viewId = viewId;
		this.documentTypeNames = documentTypeNames;
		this.maxResultFieldLength = maxResultFieldLength;
	}
	
	@Override
	public Fragment getItem(int position) {
		return ResultListFragment.newInstance(documentTypeNames[position], maxResultFieldLength);
	}

	@Override
	public int getCount() {
		return documentTypeNames.length;
	}
	
	@Override
	public CharSequence getPageTitle(int position) {
		final ResultListFragment fragment = getFragment(position);
		return fragment == null ? "" : fragment.getTitle();
	}

	@Override
	public void newSearchRequest(String query) {
		for (int i = 0; i < getCount(); ++i) {
			
			final ResultListFragment fragment = getFragment(i);
			if (fragment != null && fragment.isAdded()) {
				fragment.refresh();
			}
		}
	}
	
	private ResultListFragment getFragment(final int position) {
		return (ResultListFragment) fragmentManager.findFragmentByTag(makeFragmentName(position));
	}
	
	private String makeFragmentName(int index) {
	     return "android:switcher:" + viewId + ":" + index;
	}
}