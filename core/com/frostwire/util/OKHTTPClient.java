/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2015, FrostWire(R). All rights reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.frostwire.util;

import com.frostwire.logging.Logger;
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** An OkHttpClient based HTTP Client.
  *
  * @author gubatron
  * @author aldenml
*/
public class OKHTTPClient implements HttpClient {
    private static final Logger LOG = Logger.getLogger(JdkHttpClient.class);
    private static final int DEFAULT_TIMEOUT = 10000;
    private static final String DEFAULT_USER_AGENT = UserAgentGenerator.getUserAgent();
    private HttpClientListener listener;
    private OkHttpClient okHttpClient;

    enum HttpContext {
        SEARCH,
        DOWNLOAD
    }

    private static final Map<HttpContext, OKHTTPClient> okHttpClients;

    static {
        okHttpClients = buildOkHttpClients();
    }

    private OKHTTPClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    private static Map<HttpContext, OKHTTPClient> buildOkHttpClients() {
        final HashMap<HttpContext, OKHTTPClient> map = new HashMap<HttpContext, OKHTTPClient>();
        map.put(HttpContext.SEARCH, new OKHTTPClient(newOkHttpClient(new ThreadPool("OkHttpClient-searches", 1, 4, 60, new LinkedBlockingQueue<Runnable>(), true))));
        map.put(HttpContext.DOWNLOAD, new OKHTTPClient(newOkHttpClient(new ThreadPool("OkHttpClient-downloads", 1, 10, 5, new LinkedBlockingQueue<Runnable>(), true))));
        return map;
    }

    private static OkHttpClient newOkHttpClient(ThreadPool pool) {
        OkHttpClient searchClient = new OkHttpClient();
        searchClient.setDispatcher(new Dispatcher(pool));
        searchClient.setFollowRedirects(true);
        searchClient.setFollowSslRedirects(true);
        searchClient.setConnectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        // Maybe we should use a custom connection pool here. Using default.
        //searchClient.setConnectionPool(?);
       return searchClient;
    }

    @Override
    public void setListener(HttpClientListener listener) {
        this.listener = listener;
    }

    @Override
    public HttpClientListener getListener() {
        return listener;
    }

    @Override
    public int head(String url, int connectTimeoutInMillis) throws IOException {
        okHttpClient.setConnectTimeout(connectTimeoutInMillis,TimeUnit.MILLISECONDS);
        Request req = new Request.Builder().
                url(url).
                header("User-Agent", DEFAULT_USER_AGENT).
                head().
                build();
        Response resp = okHttpClient.newCall(req).execute();
        return resp.code();
    }

    @Override
    public String get(String url) throws IOException {
        return get(url, DEFAULT_TIMEOUT, DEFAULT_USER_AGENT);
    }

    @Override
    public String get(String url, int timeout) throws IOException {
        return get(url, timeout, DEFAULT_USER_AGENT);
    }

    @Override
    public String get(String url, int timeout, String userAgent) throws IOException {
        return get(url, timeout, userAgent, null, null);
    }

    @Override
    public String get(String url, int timeout, String userAgent, String referrer, String cookie) throws IOException {
        return get(url, timeout, userAgent, referrer, cookie, null);
    }

    @Override
    public String get(String url, int timeout, String userAgent, String referrer, String cookie, Map<String, String> customHeaders) throws IOException {
        String result = null;
        final Request.Builder builder = prepareRequestBuilder(url, timeout, userAgent, referrer, cookie);
        addCustomHeaders(customHeaders, builder);
        try {
            result = getSyncResponse(builder).body().string();
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
        return result;
    }

    @Override
    public byte[] getBytes(String url, int timeout, String userAgent, String referrer, String cookies) {
        byte[] result = null;
        final Request.Builder builder = prepareRequestBuilder(url, timeout, userAgent, referrer, cookies);
        try {
            result = getSyncResponse(builder).body().bytes();
        } catch (Throwable e) {
            LOG.error("Error getting bytes from http body response: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public byte[] getBytes(String url, int timeout, String userAgent, String referrer) {
        return getBytes(url, timeout, userAgent, referrer, null);
    }


    @Override
    public byte[] getBytes(String url, int timeout, String referrer) {
        return getBytes(url, timeout, DEFAULT_USER_AGENT, referrer);
    }

    @Override
    public byte[] getBytes(String url, int timeout) {
        return getBytes(url, timeout, null);
    }

    @Override
    public byte[] getBytes(String url) {
        return getBytes(url, DEFAULT_TIMEOUT);
    }

    @Override
    public void save(String url, File file) throws IOException {

    }

    @Override
    public void save(String url, File file, boolean resume) throws IOException {

    }

    @Override
    public void save(String url, File file, boolean resume, int timeout, String userAgent) throws IOException {

    }

    @Override
    public String post(String url, int timeout, String userAgent, Map<String, String> formData) {
        return null;
    }

    @Override
    public String post(String url, int timeout, String userAgent, String content, boolean gzip) throws IOException {
        return null;
    }

    @Override
    public String post(String url, int timeout, String userAgent, String content, String postContentType, boolean gzip) throws IOException {
        return null;
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    private Request.Builder prepareRequestBuilder(String url, int timeout, String userAgent, String referrer, String cookie) {
        okHttpClient.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        if (!StringUtils.isNullOrEmpty(userAgent)) {
            builder.header("User-Agent", userAgent);
        }
        if (!StringUtils.isNullOrEmpty(referrer)) {
            builder.header("Referer", referrer); // [sic - typo in HTTP protocol]
        }
        if (!StringUtils.isNullOrEmpty(cookie)) {
            builder.header("Cookie", cookie);
        }
        return builder;
    }

    private void addCustomHeaders(Map<String, String> customHeaders, Request.Builder builder) {
        if (customHeaders != null && customHeaders.size() > 0) {
            try {
                final Iterator<Map.Entry<String, String>> iterator = customHeaders.entrySet().iterator();
                while (iterator.hasNext()) {
                    final Map.Entry<String, String> header = iterator.next();
                    builder.header(header.getKey(), header.getValue());
                }
            } catch (Throwable e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    private Response getSyncResponse(Request.Builder builder) throws IOException {
        final Request request = builder.build();
        return okHttpClient.newCall(request).execute();
    }

//    private static void closeQuietly(Closeable closeable) {
//        try {
//            if (closeable != null) {
//                closeable.close();
//            }
//        } catch (IOException ioe) {
//            // ignore
//        }
//    }
}
