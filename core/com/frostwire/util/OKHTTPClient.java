package com.frostwire.util;

import com.frostwire.logging.Logger;
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by gubatron on 9/7/15.
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
        Request req = new Request.Builder().
                url(url).
                header("User-Agent", DEFAULT_USER_AGENT).
                head().
                build();
        okHttpClient.setConnectTimeout(connectTimeoutInMillis,TimeUnit.MILLISECONDS);
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

        ByteArrayOutputStream baos = null;

        try {
            baos = new ByteArrayOutputStream();
            get(url, baos, timeout, userAgent, referrer, cookie, -1, -1, customHeaders);
            result = new String(baos.toByteArray(), "UTF-8");
        } catch (java.net.SocketTimeoutException timeoutException) {
            throw timeoutException;
        } catch (IOException e) {
            throw e;
        } finally {
            closeQuietly(baos);
        }

        return result;
    }

    @Override
    public void get(String url, OutputStream out, int timeout, String userAgent, String referrer, String cookie, int rangeStart, int rangeLength, Map<String, String> customHeaders) throws IOException {

    }

    @Override
    public byte[] getBytes(String url, int timeout, String userAgent, String referrer) {
        return new byte[0];
    }

    @Override
    public byte[] getBytes(String url, int timeout, String userAgent, String referrer, String cookies) {
        return new byte[0];
    }

    @Override
    public byte[] getBytes(String url, int timeout, String referrer) {
        return new byte[0];
    }

    @Override
    public byte[] getBytes(String url, int timeout) {
        return new byte[0];
    }

    @Override
    public byte[] getBytes(String url) {
        return new byte[0];
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

    private static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }
}
