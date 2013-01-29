package com.swiftype.android.search.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.SearchManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.swiftype.android.search.R;
import com.swiftype.android.search.SwiftypeConfig;
import com.swiftype.android.search.SwiftypeConfig.DocumentTypeConfig;

public class SwiftypeDbHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "Swiftype.db";
	private static final String TABLE_SEARCH = "Search";
	private static final String TABLE_SUGGEST = "Suggest";
	public static final String TABLE_SEARCH_STATUS = "SearchStatus";
	public static final String TABLE_RESULT_STATUS = "ResultStatus";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_QUERY = "_query";
	public static final String COLUMN_QUERY_HASH = "_query_hash";
	public static final String COLUMN_DOCUMENT_ID = "id";
	public static final String COLUMN_TIMESTAMP = "_timestamp";
	public static final String COLUMN_DOCUMENT_TYPE = "_document_type";
	public static final String COLUMN_TOTAL_COUNT = "count";
	public static final String COLUMN_ENGINE_KEY = "engine_key";
	public static final String COLUMN_PREFIX = "prefix";
	public static final String COLUMN_SEARCH_TYPE = "search_type";
	public static final String COLUMN_EXTERNAL_ID = "external_id";
	
	private static final List<String> SHARED_COLUMNS = Arrays.asList(new String[] {COLUMN_ID, COLUMN_QUERY_HASH, COLUMN_DOCUMENT_ID});
	private static final String COMMON_COLUMNS_STATEMENT = " ( " + COLUMN_ID + " INTEGER PRIMARY KEY, " + COLUMN_DOCUMENT_ID + " TEXT, " + COLUMN_QUERY_HASH + " TEXT, " + COLUMN_TIMESTAMP + " INTEGER, ";
	
	private final List<String> createTableStatements;
	private final List<String> deleteTableStatements;
	private boolean touchedDatabase = false;
	private final SwiftypeConfig config;
	
	public SwiftypeDbHelper(final Context context) {
		super(context, DATABASE_NAME, null, getDatabaseVersion(context));
		
		final String searchStatusStatement = "CREATE TABLE IF NOT EXISTS " + TABLE_SEARCH_STATUS + " ( " + COLUMN_QUERY_HASH + " TEXT, " + COLUMN_SEARCH_TYPE + " INTEGER, " + COLUMN_TIMESTAMP + " INTEGER, PRIMARY KEY(" + COLUMN_QUERY_HASH + ") )";
		final String resultStatusStatement = "CREATE TABLE IF NOT EXISTS " + TABLE_RESULT_STATUS + " ( " + COLUMN_QUERY_HASH + " TEXT, " + COLUMN_QUERY + " TEXT, " + COLUMN_TIMESTAMP + " INTEGER, " + COLUMN_DOCUMENT_TYPE + " TEXT, " + COLUMN_TOTAL_COUNT + " INTEGER, PRIMARY KEY( " + COLUMN_QUERY + " ) )";
		
		config = new SwiftypeConfig(context.getResources());
		
		final String[] names = config.getDocumentTypeNames();
		final String[] extraStatements = {searchStatusStatement, resultStatusStatement};
		final String[] extraTables = {TABLE_SEARCH_STATUS, TABLE_RESULT_STATUS};
		int statementCount = names.length * 2 + extraStatements.length;
		createTableStatements = new ArrayList<String>(statementCount);
		deleteTableStatements = new ArrayList<String>(statementCount);
		for (final String name : names) {
			createTableStatements.addAll(createDocumentTypeTable(name));
			deleteTableStatements.add(deleteStatement(searchTable(name)));
			deleteTableStatements.add(deleteStatement(suggestTable(name)));
		}
		for (final String statement : extraStatements) {
			createTableStatements.add(statement);
		}
		for (final String table : extraTables) {
			createTableStatements.add(createIndex(table, COLUMN_QUERY_HASH));
			deleteTableStatements.add(deleteStatement(table));
		}
	}
	
	private static int getDatabaseVersion(final Context context) {
		return context.getResources().getInteger(R.integer.database_version);
	}
	
	@Override
	public SQLiteDatabase getReadableDatabase() {
		if (!touchedDatabase) {
			getWritableDatabase();
			touchedDatabase = true;
		}
		return super.getReadableDatabase();
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO: just drop if necessary? create different tables for different schemas? add columns if necessary?
		Log.i("SwiftypeDbHelper", "Drop and create search and suggest tables");
		for (String statement : deleteTableStatements) {
			db.execSQL(statement);
		}
		for (String statement : createTableStatements) {
			Log.i("SwiftypeDbHelper", "Create: " + statement);
			db.execSQL(statement);
		}
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Databases are only used for caching
		destroyDatabases(db);
		onCreate(db);
	}
	
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}
	
	public static String searchTable(final String documentTypeName) {
		return TABLE_SEARCH + "_" + documentTypeName;
	}
	final
	public static String suggestTable(final String documentTypeName) {
		return TABLE_SUGGEST + "_" + documentTypeName;
	}
	
	private void destroyDatabases(SQLiteDatabase db) {
		for (String statement : deleteTableStatements) {
			db.execSQL(statement);
		}
	}
	
	private List<String> createDocumentTypeTable(final String documentTypeName) {
		final DocumentTypeConfig documentTypeConfig = config.getDocumentTypeConfig(documentTypeName);
		final List<String> statements = new ArrayList<String>(2);
		
		final String searchTable = searchTable(documentTypeName);
		statements.add(createTable(searchTable, documentTypeConfig.getSearchFields()));
		statements.add(createIndex(searchTable, COLUMN_QUERY_HASH));
		
		final String suggestTable = suggestTable(documentTypeName);
		final String[] columns = concat(documentTypeConfig.getSuggestFields(),
										SearchManager.SUGGEST_COLUMN_INTENT_DATA,
										SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA); 
		statements.add(createTable(suggestTable, columns));
		statements.add(createIndex(suggestTable, COLUMN_QUERY_HASH));
		
		return statements;
	}
	
	private String createIndex(final String tableName, final String column) {
		return "CREATE INDEX IF NOT EXISTS " + tableName + column + "_idx ON " + tableName + " ( " + column + " )";
	}
	
	private String[] concat(final String[] array, final String ... columns) {
		final int oldLength = array.length;
		final int newColumnsLength = columns.length;
		final String[] newArray = copyOf(array, oldLength + newColumnsLength);
		for (int i = 0; i < newColumnsLength; ++i) {
			newArray[oldLength + i] = columns[i];
		}
		return newArray;
	}
	
	private String[] copyOf(final String[] array, final int newSize) {
		final String[] newArray = new String[newSize];
		for (int i = 0; i < array.length && i < newSize; ++i) {
			newArray[i] = array[i];
		}
		return newArray;
	}
	
	private String createTable(final String tableName, final String[] columns) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE IF NOT EXISTS ");
		sb.append(tableName);
		sb.append(COMMON_COLUMNS_STATEMENT);
		for (String column : columns) {
			if (SHARED_COLUMNS.contains(column)) {
				continue;
			}
			sb.append(column);
			sb.append(" TEXT, ");
		}
		sb.delete(sb.length() - 2, sb.length());
		sb.append(" )");
		return sb.toString();
	}
	
	private String deleteStatement(final String tableName) {
		return "DROP TABLE IF EXISTS " + tableName;
	}
}
