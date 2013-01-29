package com.swiftype.api.search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SwiftypeQueryOptions {
	public static final SwiftypeQueryOptions DEFAULT = new Builder().build();
	
	private final JSONObject options;

	private SwiftypeQueryOptions(final Builder builder) {
		options = new JSONObject();
		mapToJson(options, "fetch_fields", builder.fetchFields);
		mapToJson(options, "search_fields", builder.searchFields);
		mapToJson(options, "filters", builder.filters);
		mapToJson(options, "functional_boosts", builder.functionalBoosts);
		mapToJson(options, "sort_field", builder.sortField);
		mapToJson(options, "sort_direction", builder.sortDirection);
		mapToJson(options, "facets", builder.facets);
		
		arrayToJson(options, "document_types", builder.documentTypes);

		intToJson(options, "page", builder.page);
		intToJson(options, "per_page", builder.per_page);
	}
	
	private SwiftypeQueryOptions(final JSONObject options) {
		this.options = options;
	}

	public static SwiftypeQueryOptions from(final String optionString) {
		JSONObject options;
		try {
			options = new JSONObject(optionString);
		} catch (JSONException e) {
			return DEFAULT;
		}
		return new SwiftypeQueryOptions(options); 
	}
	
	public JSONObject toJson() {
		return options;
	}
	
	@Override
	public String toString() {
		return options.toString();
	}
	
	public static class Builder {
		private static final List<String> VALID_DIRECTIONS = Arrays.asList(new String[] {"asc", "desc"});
		
		private final Map<String, String[]> fetchFields = new HashMap<String, String[]>(); 
		private final Map<String, String[]> searchFields = new HashMap<String, String[]>();
		private final Map<String, Map<String, String>> filters = new HashMap<String, Map<String, String>>();
		private final Map<String, String[]> functionalBoosts = new HashMap<String, String[]>();
		private final Map<String, String> sortField = new HashMap<String, String>();
		private final Map<String, String> sortDirection = new HashMap<String, String>();
		private final Map<String, String[]> facets = new HashMap<String, String[]>();
		private String[] documentTypes;
		private int page = -1;
		private int per_page = -1;
		
		public Builder fetchFields(final String documentType, final String ... fields) {
			fetchFields.put(documentType, fields);
			return this;
		}
		
		public Builder searchFields(final String documentType, final String ... fields) {
			searchFields.put(documentType, fields);
			return this;
		}
		
		public Builder filters(final String documentType, final Map<String, String> conditions) {
			filters.put(documentType, conditions);
			return this;
		}
		
		public Builder documentTypes(final String ... documentTypes) {
			this.documentTypes = documentTypes;
			return this;
		}
		
		public Builder functionalBoosts(final String documentType, final String ... boosts) {
			functionalBoosts.put(documentType, boosts);
			return this;
		}
		
		public Builder page(final int page) {
			this.page = page;
			return this;
		}
		
		public Builder perPage(final int per_page) {
			this.per_page = per_page;
			return this;
		}
		
		public Builder sortField(final String documentType, final String field) {
			sortField.put(documentType, field);
			return this;
		}
		
		public Builder sortDirection(final String documentType, final String direction) {
			if (!VALID_DIRECTIONS.contains(direction)) {
				throw new IllegalArgumentException();
			}
			sortDirection.put(documentType, direction);
			return this;
		}
		
		public Builder facets(final String documentType, final String ... fields) {
			facets.put(documentType, fields);
			return this;
		}
		
		public SwiftypeQueryOptions build() {
			return new SwiftypeQueryOptions(this);
		}
	}
	
	private <K, V> void mapToJson(final JSONObject root, final String optionName, final Map<K, V> map) {
		if (!map.isEmpty()) {
			try {
				root.put(optionName, new JSONObject(map));
			} catch (JSONException e) {
				throw new IllegalArgumentException("Illegal arguments for " + optionName + " option!");
			}
		}
	}
	
	private <T> void arrayToJson(final JSONObject root, final String optionName, final T[] values) {
		if (values != null) {
			try {
				root.put(optionName, new JSONArray(Arrays.asList(values)));
			} catch (JSONException e) {
				throw new IllegalArgumentException("Illegal arguments for " + optionName + " option!");
			}
		}
	}
	
	private void intToJson(final JSONObject root, final String optionName, final int value) {
		if (value > 0) {
			try {
				root.put(optionName, value);
			} catch (JSONException e) {
				throw new IllegalArgumentException("Illegal arguments for " + optionName + "option!");
			}
		}
	}
}
