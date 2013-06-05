# SwiftypeAndroid

SwiftypeAndroid is a library that makes it easy to add [Swiftype-powered search](http://swiftype.com/) to your Android application. To see an example application, visit the [SwiftypeAndroidExample repository](https://github.com/swiftype/SwiftypeAndroidExample).

## Installing and configuring SwiftypeAndroid

1. Import SwiftypeAndroid into your workspace.
2. In the properties for your application select 'Android' and add the imported project as a library
3. Choose a search activity:
    - If your Swiftype search engine is crawler-based or WordPress-based and you would like to display results in a web view, use WebSearchActivity.
	- If you have an API-based engine, or would like different result display behavior, extend SearchActivity and create a custom ShowDetailsFragment to present the results.
4. Add the search activity to your AndroidManifest.xml (example for WebSearchActivity):

        <activity
	      android:name="com.swiftype.android.search.webbased.WebSearchActivity"
		  android:label="@string/app_name"
		  android:windowSoftInputMode="stateAlwaysHidden"
		  android:exported="false"
		  android:launchMode="singleTop" >
		  <intent-filter>
		    <action android:name="android.intent.action.SEARCH" />
		  </intent-filter>

          <meta-data
		    android:name="android.app.searchable"
		    android:resource="@xml/searchable" />
    	</activity>

5. Make it the default search activity for your application by adding the following lines to your main activity:

        <meta-data
		  android:name="android.app.default_searchable"
		  android:value="com.swiftype.android.search.webbased.WebSearchActivity" />

6. Copy `/res/values/swiftype_config.xml` and `/res/values/colors.xml` (if you use a light theme) from SwiftypeAndroid. You must set the values for `search_content_provider_authority` and `engine_key`. 
7. Copy `/res/xml/searchable.xml` from SwiftypeAndroid to your project.
8. Add `android:configChanges="orientation|screenSize"` to the application attributes in `AndroidManifest.xml`.
9. Add the search provider and service to your `AndroidManifest.xml`:

        <provider
          android:exported="false"
          android:authorities="@string/search_content_provider_authority"
          android:name="com.swiftype.android.search.backend.SearchContentProvider" />
        
        <service
          android:name="com.swiftype.android.search.backend.SearchService" />

10. Add Internet permissions to your `AndroidManifest.xml`:

		<uses-permission android:name="android.permission.INTERNET" />

11. Use a SearchView or the SearchDialog to start a search. For an example, look at our [SwiftypeAndroidExample](https://github.com/swiftype/SwiftypeAndroidExample) repository. More information can be found in the [Android Documentation](http://developer.android.com/training/search/setup.html).
