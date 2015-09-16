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

package com.frostwire.localpeer;

/**
 * Immutable class, share and thread safe.
 *
 * @author gubatron
 * @author aldenml
 */
public final class LocalPeer implements Cloneable {

    public LocalPeer(String address, int port, boolean local, String nickname, String clientVersion) {
        this.address = address;
        this.port = port;
        this.local = local;

        this.nickname = nickname;
        this.clientVersion = clientVersion;
    }

    public final String address;
    public final int port;
    public final boolean local;

    public final String nickname;
    public final String clientVersion;

    @Override
    public Object clone() {
        return new LocalPeer(address, port, local, nickname, clientVersion);
    }
}
