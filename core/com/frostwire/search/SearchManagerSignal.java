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

package com.frostwire.search;

/**
 * @author gubatron
 * @author aldenml
 */
public abstract class SearchManagerSignal {

    public SearchManagerSignal(long token) {
        this.token = token;
    }

    /**
     *
     */
    public final long token;

    public static final class Result extends SearchManagerSignal {

        Result(long token, SearchResult sr) {
            super(token);
            this.sr = sr;
        }

        /**
         *
         */
        public final SearchResult sr;
    }

    public static final class End extends SearchManagerSignal {

        End(long token) {
            super(token);
        }
    }
}
