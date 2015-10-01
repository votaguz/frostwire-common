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
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

/**
 * @author gubatron
 * @author aldenml
 */
public final class HttpClient {

    private static final Logger LOG = Logger.getLogger(HttpClient.class);

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

    public static HttpClient with(Params params) {
        return new HttpClient(null);
    }

    private com.squareup.okhttp.Request buildReq(Request request) {
        return new com.squareup.okhttp.Request.Builder()
                .method(request.method().toString(), buildReqBody(request))
                .url(request.url())
                .build();
    }

    private RequestBody buildReqBody(Request request) {
        if (request.mime() != null && request.body() != null) {
            return RequestBody.create(MediaType.parse(request.mime()), request.body());
        } else {
            return null;
        }
    }

    public static final class Params {

    }
}
