package com.swiftype.android.search.helper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;


import android.app.SearchManager;
import android.content.ContentValues;

public class ResultParser {
	private static final String FIELD_HIGHLIGHT = "highlight";
	private static final String FIELD_RECORDS = "records";
	private static final String FIELD_INFO = "info";
	private static final String FIELD_TOTAL_COUNT = "total_result_count";
	private static final ContentValues[] EMTPY_RESULTS = new ContentValues[0];
	
	private final JSONObject records;
	private final JSONObject info;
	private final String[] documentTypeNames;
	
	public ResultParser(final JSONObject response) {
		records = response.optJSONObject(FIELD_RECORDS);
		info = response.optJSONObject(FIELD_INFO);
		final Set<String> uniqueNames = new HashSet<String>();
		if (records != null) {
			for (@SuppressWarnings("unchecked") final Iterator<String> names = records.keys(); names.hasNext();) {
				uniqueNames.add(names.next());
			}
		}
		documentTypeNames = uniqueNames.toArray(new String[uniqueNames.size()]);
	}
	
	public String[] getDocumentTypeNames() {
		return documentTypeNames;
	}
	
	public ContentValues[] getResults(final String documentTypeName, final String[] fields) {
		return getResults(documentTypeName, fields, null);
	}
	
	
	public ContentValues[] getResults(final String documentTypeName, final String[] fields, final String suggestIdentifierField) {
		final JSONArray documentTypeResults = records.optJSONArray(documentTypeName);
		if (documentTypeResults == null) {
			return EMTPY_RESULTS;
		}
		final boolean isSuggest = (suggestIdentifierField != null);
		
		final int resultCount = documentTypeResults.length();
		final ContentValues[] results = new ContentValues[resultCount];
		for (int i = 0; i < resultCount; ++i) {
			final JSONObject result = documentTypeResults.optJSONObject(i);
			final ContentValues resultValues = extractResultFields(result, fields, isSuggest);
			resultValues.put(SwiftypeDbHelper.COLUMN_DOCUMENT_TYPE, documentTypeName);
			
			if (isSuggest) {
				resultValues.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA, result.optString(SwiftypeDbHelper.COLUMN_DOCUMENT_ID));
				resultValues.put(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA, documentTypeName + " " + result.optString(suggestIdentifierField));
			}
			
			results[i] = resultValues;
		}
		return results;
	}
	
	public ContentValues getInfo(final String documentTypeName) {
		final JSONObject documentTypeInfo = info.optJSONObject(documentTypeName);

		final ContentValues resultInfo = new ContentValues();
		resultInfo.put(SwiftypeDbHelper.COLUMN_DOCUMENT_TYPE, documentTypeName);
		final int totalCount = (documentTypeInfo == null) ? 0 : documentTypeInfo.optInt(FIELD_TOTAL_COUNT);
		resultInfo.put(SwiftypeDbHelper.COLUMN_TOTAL_COUNT, totalCount);
		
		return resultInfo;
	}
	
	private ContentValues extractResultFields(final JSONObject result, final String[] fieldNames, final boolean isSuggest) {
		final ContentValues fields = new ContentValues();
		
		JSONObject highlightedFields = result.optJSONObject(FIELD_HIGHLIGHT);
		if (highlightedFields == null){
			highlightedFields = new JSONObject();
		}
		
		for (final String fieldName : fieldNames) {
			if (!isSuggest && highlightedFields.has(fieldName)) {
				// prefer highlighted fields
				final String value = highlightedFields.optString(fieldName);
				fields.put(fieldName, value);
			} else {
				JSONArray arrayField = result.optJSONArray(fieldName);
				
				final String value;
				if (arrayField != null && isSuggest) {
					// only store the first value of arrays for suggests
					value = arrayField.optString(0);
				} else {
					value = result.optString(fieldName);
				}
				
				fields.put(fieldName, value);
			}
		}
		
		return fields;
	}
}
