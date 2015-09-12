package com.swiftype.api.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import android.util.Log;

public class RestConnection {
	private static final String LOG_ID = RestConnection.class.getSimpleName();
	
	private static final String API_HOST = "http://api.swiftype.com";
	private static final String BASE_URL = API_HOST + "/api/v1/public/";
	private static final DefaultHttpClient CLIENT;
	
	static {
		final SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
		final HttpParams params = new BasicHttpParams();
		final ClientConnectionManager connectionManager = new ThreadSafeClientConnManager(params, schemeRegistry);

		CLIENT = new DefaultHttpClient(connectionManager, params);
		CLIENT.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {
			
			@Override
			public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
				if (executionCount >= 3) {
					return false;
				}
				
				HttpUriRequest request = (HttpUriRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
				if (exception instanceof InterruptedIOException && !request.isAborted()) {
					return true;
				}
				
				return false;
			}
		});
		HttpProtocolParams.setUserAgent(CLIENT.getParams(), "Swiftype Android");
	}
	
	public HttpUriRequest get(final String requestUri) {
		final HttpUriRequest request = new HttpGet(BASE_URL + requestUri);
		request.setHeader("Accept-Encoding", "gzip,deflate");
		return request;
	}
	
	public HttpPost post(final String requestUri, final String body) {
		HttpPost request = new HttpPost(BASE_URL + requestUri);
		try {
			StringEntity entity = new StringEntity(body, "UTF-8");
			request.setEntity(entity);
			request.setHeader("Content-type", "application/json");
			request.setHeader("Accept-Encoding", "gzip,deflate");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("Invalid encoding for options.");
		}
		return request;
	}
	
	public String execute(final HttpUriRequest request) {
		ResponseHandler<String> responseHandler = new GzipStringResponseHandler();

		String response = null;
		try {
			response = CLIENT.execute(request, responseHandler);
		} catch (HttpResponseException e) {
			Log.i(LOG_ID, e.getMessage());
		} catch (IOException e) {
			Log.i(LOG_ID, "Aborted: " + e.getMessage());
		}
		return response;
	}
	
	public class GzipStringResponseHandler implements ResponseHandler<String> {
		private static final String GZIP = "gzip";

		public String handleResponse(HttpResponse response) throws IOException {
			String answer = null;
			
			if (response.getStatusLine().getStatusCode() >= 300) {
				return answer;
			}
			
			final HttpEntity httpEntity = response.getEntity();
			final Header contentEncoding = response.getFirstHeader("Content-Encoding");
			
			InputStream in = httpEntity.getContent();
			if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase(GZIP)) {
				try {
					in = new GZIPInputStream(in);
				} catch (IllegalStateException e) {
					// ignore
				}
			}
			
			final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			
			final StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			answer = sb.toString();
			
			return answer;
		}
	}
}
