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


package com.frostwire.util.http;

import com.frostwire.logging.Logger;
import com.frostwire.util.HttpClientFactory;
import static com.frostwire.util.HttpClientFactory.HttpContext;
import com.frostwire.util.StringUtils;
import com.frostwire.util.ThreadPool;
import com.squareup.okhttp.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** An OkHttpClient based HTTP Client.
  *
  * @author gubatron
  * @author aldenml
*/
public class OKHTTPClient extends AbstractHttpClient {
    private static final Logger LOG = Logger.getLogger(OKHTTPClient.class);
    private OkHttpClient okHttpClient;


    private static final Map<HttpContext, OKHTTPClient> okHttpClients;

    static {
        okHttpClients = buildOkHttpClients();
    }

    private OKHTTPClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    @Override
    public int head(String url, int connectTimeoutInMillis) throws IOException {
        okHttpClient.setConnectTimeout(connectTimeoutInMillis, TimeUnit.MILLISECONDS);
        Request req = new Request.Builder().
                url(url).
                header("User-Agent", DEFAULT_USER_AGENT).
                head().
                build();
        Response resp = okHttpClient.newCall(req).execute();
        return resp.code();
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
    public void save(String url, File file, boolean resume, int timeout, String userAgent, String referrer) throws IOException {
        FileOutputStream fos = null;
        int rangeStart = 0;

        try {
            if (resume && file.exists()) {
                fos = new FileOutputStream(file, true);
                rangeStart = (int) file.length();
            } else {
                fos = new FileOutputStream(file, false);
                rangeStart = -1;
            }

            final Request.Builder builder = prepareRequestBuilder(url, timeout, userAgent, referrer, null);
            addRangeHeader(rangeStart, -1, builder);
            final Response response = getSyncResponse(builder);
            final InputStream in = response.body().byteStream();

            byte[] b = new byte[4096];
            int n;
            while (!canceled && (n = in.read(b, 0, b.length)) != -1) {
                if (!canceled) {
                    fos.write(b, 0, n);
                    onData(b, 0, n);
                }
            }
            closeQuietly(fos);
            if (canceled) {
                onCancel();
            } else {
                onComplete();
            }
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            closeQuietly(fos);
        }
    }

    @Override
    public String post(String url, int timeout, String userAgent, Map<String, String> formData) {
        /* TODO */
        String result = null;

        ByteArrayOutputStream baos = null;

        try {
            baos = new ByteArrayOutputStream();
            //post(url, baos, timeout, userAgent, formData);
            //result = new String(baos.toByteArray(), "UTF-8");
        } catch (Throwable e) {
            LOG.error("Error posting data via http: " + e.getMessage(), e);
        } finally {
            closeQuietly(baos);
        }

        return result;
    }

    @Override
    public String post(String url, int timeout, String userAgent, String content, String postContentType, boolean gzip) throws IOException {
        /* WIP */
        String result = null;
        canceled = false;
        final Request.Builder builder = prepareRequestBuilder(url, timeout, userAgent, null, null);
        if (!StringUtils.isNullOrEmpty(postContentType)) {
            builder.addHeader("Content-type", postContentType);
        }
        final RequestBody requestBody = RequestBody.create(MediaType.parse(postContentType), content);
        builder.post(requestBody);
        okHttpClient.setFollowRedirects(false);
        okHttpClient.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        //okHttpClient.setSslSocketFactory()

//        final URL u = new URL(url);
//        final HttpURLConnection conn = (HttpURLConnection) u.openConnection();
//        conn.setDoOutput(true);
//        conn.setConnectTimeout(timeout);
//        conn.setReadTimeout(timeout);
//        conn.setRequestProperty("User-Agent", userAgent);
//        conn.setInstanceFollowRedirects(false);

        /**
        if (conn instanceof HttpsURLConnection) {
            setHostnameVerifier((HttpsURLConnection) conn);
        }

        byte[] data = content.getBytes("UTF-8");

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", postContentType);
        conn.setRequestProperty("charset", "utf-8");
        conn.setUseCaches(false);

        ByteArrayInputStream in = new ByteArrayInputStream(data);

        try {
            OutputStream out = null;
            if (gzip) {
                out = new GZIPOutputStream(conn.getOutputStream());
            } else {
                out = conn.getOutputStream();
            }

            byte[] b = new byte[4096];
            int n = 0;
            while (!canceled && (n = in.read(b, 0, b.length)) != -1) {
                if (!canceled) {
                    out.write(b, 0, n);
                    out.flush();
                    onData(b, 0, n);
                }
            }

            closeQuietly(out);

            conn.connect();
            int httpResponseCode = getResponseCode(conn);

            if (httpResponseCode != HttpURLConnection.HTTP_OK && httpResponseCode != HttpURLConnection.HTTP_PARTIAL) {
                throw new ResponseCodeNotSupportedException(httpResponseCode);
            }

            if (canceled) {
                onCancel();
            } else {
                BufferedInputStream bis = new BufferedInputStream(conn.getInputStream(), 4096);
                ByteArrayBuffer baf = new ByteArrayBuffer(1024);
                byte[] buffer = new byte[64];
                int read = 0;
                while (true) {
                    read = bis.read(buffer);
                    if (read == -1) {
                        break;
                    }
                    baf.append(buffer, 0, read);
                }
                result = new String(baf.toByteArray());
                onComplete();
            }
        } catch (Exception e) {
            onError(e);
        } finally {
            closeQuietly(in);
            closeQuietly(conn);
        }
         */
        return result;
    }

    private void addRangeHeader(int rangeStart, int rangeEnd, Request.Builder builderRef) {
        if (rangeStart < 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("bytes=");
        sb.append(String.valueOf(rangeStart));
        sb.append('-');
        if (rangeEnd > 0 && rangeEnd > rangeStart) {
            sb.append(String.valueOf(rangeEnd));
        }
        builderRef.addHeader("Range", sb.toString());
    }

    private static Map<HttpContext, OKHTTPClient> buildOkHttpClients() {
        final HashMap<HttpContext, OKHTTPClient> map = new HashMap<HttpContext, OKHTTPClient>();
        map.put(HttpContext.SEARCH, new OKHTTPClient(newOkHttpClient(new ThreadPool("OkHttpClient-searches", 1, 4, 60, new LinkedBlockingQueue<Runnable>(), true))));
        map.put(HttpContext.DOWNLOAD, new OKHTTPClient(newOkHttpClient(new ThreadPool("OkHttpClient-downloads", 1, 10, 5, new LinkedBlockingQueue<Runnable>(), true))));
        return map;
    }

    private Request.Builder prepareRequestBuilder(String url, int timeout, String userAgent, String referrer, String cookie) {
        okHttpClient.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
        okHttpClient.setReadTimeout(timeout, TimeUnit.MILLISECONDS);
        okHttpClient.setWriteTimeout(timeout, TimeUnit.MILLISECONDS);
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
}