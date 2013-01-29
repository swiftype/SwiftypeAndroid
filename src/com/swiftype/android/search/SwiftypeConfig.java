package com.swiftype.android.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.app.SearchManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.sqlite.SQLiteQueryBuilder;

import com.swiftype.android.search.helper.SwiftypeDbHelper;
import com.swiftype.api.search.SwiftypeQueryOptions;

public class SwiftypeConfig {
	private static final ConcurrentHashMap<String, DocumentTypeConfig> documentTypeConfigs = new ConcurrentHashMap<String, SwiftypeConfig.DocumentTypeConfig>();
	private static SwiftypeQueryOptions options = SwiftypeQueryOptions.DEFAULT;
	private static SwiftypeQueryOptions suggestOptions = SwiftypeQueryOptions.DEFAULT;
	private static int suggestLayout;
	private static String[] suggestColumns;
	private static int[] suggestResources;
	private static int maxResultFieldLength;
	private static int maxSuggestFieldLength;
	private static String[] documentTypeNames;
	
	public SwiftypeConfig(final Resources resources) {
		if (documentTypeNames == null) {
			final String splitRegex = "\\s*,\\s*";
			
			suggestLayout = resources.obtainTypedArray(R.array.suggest_layout).getResourceId(0, 0);
			if (suggestLayout == 0) {
				throw new IllegalArgumentException();
			}
			
			suggestColumns = resources.getStringArray(R.array.suggest_columns);
			suggestResources = getResourceIds(resources, R.array.suggest_resources);
			
			if (suggestColumns.length != suggestResources.length) {
				throw new IllegalArgumentException("Suggest columns and resources must have the same length.");
			}
		
			maxResultFieldLength = resources.getInteger(R.integer.max_result_field_length);
			maxSuggestFieldLength = resources.getInteger(R.integer.max_suggest_field_length);
			
			final TypedArray documentTypes = resources.obtainTypedArray(R.array.document_types);
			documentTypeNames = new String[documentTypes.length()];
			for (int i = 0; i < documentTypes.length(); ++i) {
				int id = documentTypes.getResourceId(i, 0);
				if (id == 0) {
					throw new IllegalArgumentException();
				}
				
				final TypedArray documentType = resources.obtainTypedArray(id);
				
				int fieldIndex = 0;
				final String name = documentType.getString(fieldIndex++);
				final String identifierField = documentType.getString(fieldIndex++);
				final int resultItemLayout = documentType.getResourceId(fieldIndex++, 0);
				final String[] displayFields = documentType.getString(fieldIndex++).split(splitRegex);
					
				final int displayFieldResourceId = documentType.getResourceId(fieldIndex++, 0);
				if (displayFieldResourceId == 0) {
					throw new IllegalArgumentException();
				}
				
				final int[] displayResources = getResourceIds(resources, displayFieldResourceId);
				
				final String[] suggestDisplayFields = documentType.getString(fieldIndex++).split(splitRegex);
				
				if (suggestDisplayFields.length != suggestColumns.length) {
						throw new IllegalArgumentException("Suggest display fields and columns must be the same length!");
					}
				
				DocumentTypeConfig documentTypeConfig = new DocumentTypeConfig(name,
																			   identifierField,
																			   resultItemLayout,
																			   displayFields,
																			   displayResources,
																			   suggestDisplayFields);
				documentTypeNames[i] = name;
				documentTypeConfigs.put(name, documentTypeConfig);
			}
			
		}
	}
	
	private int[] getResourceIds(final Resources resources, final int resourceId) {
		final TypedArray resourceIdArray = resources.obtainTypedArray(resourceId);
		
		final int[] resourceIds = new int[resourceIdArray.length()];
		for (int i = 0; i < resourceIdArray.length(); ++i) {
			final int id = resourceIdArray.getResourceId(i, 0);
			if (id == 0) {
				throw new IllegalArgumentException();
			}
			resourceIds[i] = id;
		}
		return resourceIds;
	}
	
	public SwiftypeQueryOptions getQueryOptions() {
		return options;
	}
	
	public SwiftypeConfig setQueryOptions(final SwiftypeQueryOptions options) {
		SwiftypeConfig.options = options;
		return this;
	}
	
	public SwiftypeQueryOptions getSuggestQueryOptions() {
		return suggestOptions;
	}
	
	public SwiftypeConfig setSuggestQueryOptions(final SwiftypeQueryOptions options) {
		SwiftypeConfig.suggestOptions = options;
		return this;
	}

	public int getMaxResultFieldLength() {
		return maxResultFieldLength;
	}
	
	public int getMaxSuggestFieldLength() {
		return maxSuggestFieldLength;
	}
	
	public String[] getDocumentTypeNames() {
		return documentTypeNames;
	}
	
	public int getSuggestLayout() {
		return suggestLayout;
	}
	
	public String[] getSuggestColumns() {
		return suggestColumns;
	}
	
	public int[] getSuggestResources() {
		return suggestResources;
	}
	
	public DocumentTypeConfig getDocumentTypeConfig(final String name) {
		DocumentTypeConfig config = documentTypeConfigs.get(name);
		if (config == null) {
			config = new DocumentTypeConfig(name);
			documentTypeConfigs.put(name, config);
		}
		return config;
	}
	
	public static class DocumentTypeConfig {
		private static final int DEFAULT_ITEM_LAYOUT = R.layout.default_result_item;
		private static final String[] SUGGEST_EXTRA_FIELDS = {SearchManager.SUGGEST_COLUMN_INTENT_DATA,
															  SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,
															  SwiftypeDbHelper.COLUMN_TIMESTAMP};
		
		private final String name;
		private final String identifierField;
		private final int resultItemLayout;
		private final String[] displayFields;
		private final int[] displayResources;
		private final String[] suggestDisplayFields;
		private final String[] searchFields;
		private final String[] suggestFields;
		private final SQLiteQueryBuilder suggestQuery;
		
		private DocumentTypeConfig(final String name) {
			this(name,
				 SwiftypeDbHelper.COLUMN_EXTERNAL_ID,
				 DEFAULT_ITEM_LAYOUT,
				 new String[] {SwiftypeDbHelper.COLUMN_EXTERNAL_ID},
				 new int[] {R.id.default_item_external_id},
				 new String[] {SwiftypeDbHelper.COLUMN_EXTERNAL_ID});
		}
		
		private DocumentTypeConfig(final String name,
				                   final String identifierField,
				                   final int resultItemLayout,
				                   final String[] displayFields,
				                   final int[] displayResources,
				                   final String[] suggestDisplayFields) {
			
			this.name = name;
			this.identifierField = identifierField;
			this.resultItemLayout = resultItemLayout;
			this.displayFields = displayFields;
			this.displayResources = displayResources;
			this.suggestDisplayFields = suggestDisplayFields;
			
			Set<String> fieldSet = new HashSet<String>();
			for (final String field : displayFields) {
				fieldSet.add(field);
			}
			fieldSet.add(identifierField);
			fieldSet.add(SwiftypeDbHelper.COLUMN_DOCUMENT_ID);
			searchFields = fieldSet.toArray(new String[fieldSet.size()]);
			
			fieldSet.clear();
			for (final String field : suggestDisplayFields) {
				fieldSet.add(field);
			}
			fieldSet.add(identifierField);
			fieldSet.add(SwiftypeDbHelper.COLUMN_DOCUMENT_ID);
			suggestFields = fieldSet.toArray(new String[fieldSet.size()]);
			
			suggestQuery = new SQLiteQueryBuilder();
			suggestQuery.setTables(SwiftypeDbHelper.suggestTable(name));
			
			Map<String, String> projectionMap = new HashMap<String, String>();
			projectionMap.put(SwiftypeDbHelper.COLUMN_ID, SwiftypeDbHelper.COLUMN_ID);
			for (int i = 0; i < suggestDisplayFields.length; ++i) {
				final String field = suggestDisplayFields[i];
				projectionMap.put(field, field + " AS " + suggestColumns[i]);
			}
			for (final String field : SUGGEST_EXTRA_FIELDS) {
				projectionMap.put(field, field + " AS " + field);
			}
			
			suggestQuery.setProjectionMap(projectionMap);
		}
		
		public String getName() {
			return name;
		}
		
		public int getResultItemLayout() {
			return resultItemLayout;
		}

		public String[] getDisplayFields() {
			return displayFields;
		}

		public int[] getDisplayResources() {
			return displayResources;
		}

		public String getIdentifierField() {
			return identifierField;
		}
		
		public String[] getSuggestDisplayFields() {
			return suggestDisplayFields;
		}

		public String[] getSearchFields() {
			return searchFields;
		}
		
		public String[] getSuggestFields() {
			return suggestFields;
		}
		
		public SQLiteQueryBuilder getSuggestQuery() {
			return suggestQuery;
		}
	}
}