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

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;

/**
 * @author gubatron
 * @author aldenml
 */
final class OkClient implements HttpClient {

    private final OkHttpClient c;

    public OkClient(OkHttpClient c) {
        this.c = c;
    }

    @Override
    public void send(final Request request, final RequestListener listener) {
        com.squareup.okhttp.Request r = new com.squareup.okhttp.Request.Builder()
                .url(request.url())
                .build();

        c.newCall(r).enqueue(new Callback() {
            @Override
            public void onFailure(com.squareup.okhttp.Request r, IOException e) {
                listener.onFailure(request, e);
            }

            @Override
            public void onResponse(com.squareup.okhttp.Response r) throws IOException {
                listener.onResponse(new OkResponse(r));
            }
        });
    }
}
