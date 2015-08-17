/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
 
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

package com.frostwire.search.torrent;

import com.frostwire.search.AbstractFileSearchResult;
import com.frostwire.util.Digests;

import java.nio.ByteBuffer;

/**
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public abstract class AbstractTorrentSearchResult extends AbstractFileSearchResult implements TorrentCrawlableSearchResult {

    private int uid = -1;

    @Override
    public boolean isComplete() {
        return true;
    }

    public String getCookies() { return null; }

    @Override
    public int uid() {
        if (uid == -1) {
            StringBuilder key = new StringBuilder();
            key.append(getDisplayName());
            key.append(getDetailsUrl());
            key.append(getSource());
            key.append(getHash());
            uid = Digests.fnvhash32(key.toString().getBytes());
        }
        return uid;
    }
}