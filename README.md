# SwiftypeAndroid

## Option 1 - You want a search frontend for your website or WordPress blog

  1. In the properties for this project select 'Android' and unselect 'is Library'
  2. Change /res/values/swiftype_config.xml according to the instructions there.

## Option 2 - Use SwiftypeAndroid to extend your application with search

  1. Import SwiftypeAndroid into your workspace
  2. In the properties for your application select 'Android' and add the imported project as a library
  3. Choose a search activity:
	 - Option 1: Use WebSearchActivity from SwiftypeAndroid, if you want to search a crawler-based or a WordPress engine.
	 - Option 2: Extend SearchActivity to get and create a custom ShowDetailsFragment to present the results on clicks.
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

  6. Make it the default search activity for your application by adding the following lines to your main activity:

		<meta-data
			android:name="android.app.default_searchable"
			android:value="com.swiftype.android.search.webbased.WebSearchActivity" />

  7. Copy /res/values/swiftype\_config.xml and /res/values/colors.xml (if you use a light theme) from SwiftypeAndroid
	 If you are OK with the default values, adding swiftype\_config.xml to your /res/values and setting the values for 'search\_content\_provider\_authority' and 'engine\_key' is enough.
  8. Copy searchable.xml from SwiftypeAndroid /res/xml/searchable.xml to your project
  9. Add 'android:configChanges="orientation|screenSize"' to the application attributes in AndroidManifest.xml
  10. Add the search provider and service to your AndroidManifest.xml

		<provider
			android:exported="false"
			android:authorities="@string/search_content_provider_authority"
			android:name="com.swiftype.android.search.backend.SearchContentProvider" />

	    <service
			android:name="com.swiftype.android.search.backend.SearchService" />

  11. Add internet permissions to your AndroidManifest.xml

		<uses-permission android:name="android.permission.INTERNET" />

  12. Use a SearchView or the SearchDialog to start a search. For an example take a look at our SwiftypeAndroidExample repository at github. More information can be found at the [Android Documentation](http://developer.android.com/training/search/setup.html).
