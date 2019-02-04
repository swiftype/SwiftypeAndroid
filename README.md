<p align="center"><img src="https://github.com/swiftype/SwiftypeAndroid/blob/master/logo-site-search.png?raw=true" alt="Elastic Site Search Logo"></p>

> A first-party Android library to add [Elastic Site Search](https://swiftype.com/documentation/site-search/overview) to Android applications.

## Contents

+ [Getting started](#getting-started-)
+ [FAQ](#faq-)
+ [Contribute](#contribute-)
+ [License](#license-)

***

## Getting started 🐣

> **Note:** This client has been developed for the [Swiftype Site Search](https://www.swiftype.com/site-search) API endpoints only. You may refer to the [Swiftype Site Search API Documentation](https://swiftype.com/documentation/site-search/overview) for additional context.

1. Import SwiftypeAndroid into your workspace.
2. In the properties for your application select 'Android' and add the imported project as a library
3. Choose a search activity:
    - If your Swiftype search engine is crawler-based or WordPress-based and you would like to display results in a web view, use WebSearchActivity.
	- If you have an API-based engine, or would like different result display behavior, extend SearchActivity and create a custom ShowDetailsFragment to present the results.
4. Add the search activity to your AndroidManifest.xml (example for WebSearchActivity):

```c
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
```
5. Make it the default search activity for your application by adding the following lines to your main activity:

```c
        <meta-data
		  android:name="android.app.default_searchable"
		  android:value="com.swiftype.android.search.webbased.WebSearchActivity" />
```
6. Copy `/res/values/swiftype_config.xml` and `/res/values/colors.xml` (if you use a light theme) from SwiftypeAndroid. You must set the values for `search_content_provider_authority` and `engine_key`.
7. Copy `/res/xml/searchable.xml` from SwiftypeAndroid to your project.
8. Add `android:configChanges="orientation|screenSize"` to the application attributes in `AndroidManifest.xml`.
9. Add the search provider and service to your `AndroidManifest.xml`:

```c
        <provider
          android:exported="false"
          android:authorities="@string/search_content_provider_authority"
          android:name="com.swiftype.android.search.backend.SearchContentProvider" />

        <service
          android:name="com.swiftype.android.search.backend.SearchService" />
```
10. Add Internet permissions to your `AndroidManifest.xml`:

```c
		<uses-permission android:name="android.permission.INTERNET" />
```
11. Use a SearchView or the SearchDialog to start a search. For an example, look at our [SwiftypeAndroidExample](https://github.com/swiftype/SwiftypeAndroidExample) repository. More information can be found in the [Android Documentation](http://developer.android.com/training/search/setup.html).

## FAQ 🔮

### Where do I report issues with the client?

If something is not working as expected, please open an [issue](https://github.com/swiftype/SwiftypeAndroid/issues/new).

### Where can I learn more about Site Search?

Your best bet is to read the [documentation](https://swiftype.com/documentation/site-search).

### Where else can I go to get help?

You can checkout the [Elastic Site Search community discuss forums](https://discuss.elastic.co/c/site-search).

## Contribute 🚀

We welcome contributors to the project. Before you begin, a couple notes...

+ Before opening a pull request, please create an issue to [discuss the scope of your proposal](https://github.com/swiftype/SwiftypeAndroid/issues).
+ Please write simple code and concise documentation, when appropriate.

## License 📗

[MIT](https://github.com/swiftype/SwiftypeAndroid/blob/master/LICENSE) © [Elastic](https://github.com/elastic)

Thank you to all the [contributors](https://github.com/swiftype/SwiftypeAndroid/graphs/contributors)!
