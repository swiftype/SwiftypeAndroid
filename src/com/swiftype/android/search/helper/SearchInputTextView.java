package com.swiftype.android.search.helper;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class SearchInputTextView extends AutoCompleteTextView {

	public SearchInputTextView(Context context) {
		super(context);
	}
	
	public SearchInputTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public SearchInputTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	protected void replaceText(CharSequence text) {
		// don't change search text
	}
}
