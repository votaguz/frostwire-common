/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2015, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.http;

import com.frostwire.logging.Logger;
import com.frostwire.util.ThreadPool;
import com.frostwire.util.http.AllX509TrustManager;
import com.frostwire.util.http.WrapSSLSocketFactory;
import com.squareup.okhttp.*;
import org.apache.commons.io.IOUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author gubatron
 * @author aldenml
 */
public final class HttpClient {

    private static final Logger LOG = Logger.getLogger(HttpClient.class);

    protected static final int DEFAULT_TIMEOUT = 10000;

    private static class Loader {
        static final OkHttpClient DEFAULT_CLIENT = buildDefaultClient();
    }

    private final OkHttpClient c;

    HttpClient(OkHttpClient c) {
        this.c = c;
    }

    public void send(final Request request, final RequestListener listener) {
        c.newCall(buildReq(request)).enqueue(new Callback() {
            @Override
            public void onFailure(com.squareup.okhttp.Request r, IOException e) {
                try {
                    listener.onFailure(request, e);
                } catch (Throwable t) {
                    LOG.warn("Error invoking listener", t);
                }
            }

            @Override
            public void onResponse(com.squareup.okhttp.Response r) throws IOException {
                try {
                    listener.onResponse(new Response(r));
                } catch (Throwable t) {
                    LOG.warn("Error invoking listener", t);
                } finally {
                    IOUtils.closeQuietly(r.body());
                }
            }
        });
    }

    public static HttpClient get() {
        return with(null);
    }

    public static HttpClient with(Params params) {
        return new HttpClient(params != null ? buildClient(params) : Loader.DEFAULT_CLIENT);
    }

    private static OkHttpClient buildDefaultClient() {
        OkHttpClient c = new OkHttpClient();

        c.setFollowRedirects(true);
        c.setFollowSslRedirects(true);
        c.setConnectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        c.setReadTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        c.setWriteTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        c.setHostnameVerifier(buildHostnameVerifier());
        c.setSslSocketFactory(buildSSLSocketFactory());
        c.interceptors().add(new GzipInterceptor());

        return c;
    }

    private static OkHttpClient buildClient(Params params) {
        OkHttpClient c = Loader.DEFAULT_CLIENT.clone();

        if (params.pool != null) {
            c.setDispatcher(new Dispatcher(params.pool));
        }

        return c;
    }

    private com.squareup.okhttp.Request buildReq(Request request) {
        return new com.squareup.okhttp.Request.Builder()
                .method(request.method().toString(), buildReqBody(request))
                .headers(Headers.of(request.headers()))
                .url(request.url()).build();
    }

    private RequestBody buildReqBody(Request request) {
        if (request.mime() != null && request.body() != null) {
            return RequestBody.create(MediaType.parse(request.mime()), request.body());
        } else {
            return null;
        }
    }

    private static HostnameVerifier buildHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
    }

    private static SSLSocketFactory buildSSLSocketFactory() {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new AllX509TrustManager()}, new SecureRandom());
            SSLSocketFactory d = sc.getSocketFactory();
            return new WrapSSLSocketFactory(d);
        } catch (Throwable e) {
            LOG.error("Unable to create custom SSL socket factory", e);
        }

        return null;
    }

    public static final class Params {

        public Params(ThreadPool pool) {
            this.pool = pool;
        }

        public final ThreadPool pool;
    }
}
