package com.swiftype.android.search.helper;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.TextView;

import com.swiftype.android.search.R;

public class HighlightCursorAdapter extends SimpleCursorAdapter {
	private static final String EM_START = "<em>";
	private static final String EM_STOP = "</em>";
	private static final String DOTS = "...";
	private final StyleSpan highlightStyle;
	private ForegroundColorSpan highlightColor;
	
	private final int maxFieldLength; 
	
	public HighlightCursorAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to, int flags, int maxFieldLength) {
		super(context, layout, cursor, from, to, flags);
		
		highlightStyle = new StyleSpan(android.graphics.Typeface.BOLD);
		highlightColor = new ForegroundColorSpan(context.getResources().getColor(R.color.result_highlight));
		this.maxFieldLength = maxFieldLength;
	}
	
	@Override
	public void setViewText(TextView view, String text) {
		SpannableStringBuilder sb = new SpannableStringBuilder();
		int pos = 0;
		for (int hit = text.indexOf(EM_START, pos); hit != -1; hit = text.indexOf(EM_START, pos)) {
			sb.append(decodeText(text, pos, hit));
			int spanStart = sb.length();
			pos = hit + EM_START.length();
			hit = text.indexOf(EM_STOP, pos);
			if (hit == -1) {
				hit = text.length();
			}
			sb.append(decodeText(text, pos, hit));
			pos = hit + EM_STOP.length();
			sb.setSpan(highlightStyle, spanStart, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			sb.setSpan(highlightColor, spanStart, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		int length = text.length();
		if (pos < length) {
			sb.append(decodeText(text, pos, length));
		}
		
		if (sb.length() > maxFieldLength) {
			sb = sb.delete(maxFieldLength, sb.length());
			sb.append(DOTS);
		}

		view.setText(sb);
	}
	
	private String decodeText(final String text, int start, int end) {
		final String substring = text.substring(start, end);
		final StringBuilder sb = new StringBuilder();
		if (substring.startsWith(" ")) {
			sb.append(" ");
		}
		sb.append(Html.fromHtml(substring));
		return sb.toString();
	}
}