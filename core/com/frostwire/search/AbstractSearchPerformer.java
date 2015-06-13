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

import com.frostwire.logging.Logger;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * @author gubatron
 * @author aldenml
 */
public abstract class AbstractSearchPerformer implements SearchPerformer {

    private static final Logger LOG = Logger.getLogger(AbstractSearchPerformer.class);

    private final long token;
    private final PublishSubject<Iterable<? extends SearchResult>> subject;

    public AbstractSearchPerformer(long token) {
        this.token = token;
        this.subject = PublishSubject.create();
    }

    @Override
    public long getToken() {
        return token;
    }

    @Override
    public Observable<Iterable<? extends SearchResult>> observable() {
        return subject;
    }

    @Override
    public void stop() {
        subject.onCompleted();
    }

    @Override
    public boolean isStopped() {
        return subject.hasCompleted();
    }

    protected void onResults(Iterable<? extends SearchResult> results) {
        try {
            if (results != null) {
                subject.onNext(results);
            }
        } catch (Throwable e) {
            LOG.warn("Error sending results back to receiver: " + e.getMessage());
        }
    }
}
