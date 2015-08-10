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

package com.frostwire.search;

/**
 * TODO: Remove this class
 *
 * This works only on the real Java Runtime.
 *
 * The geniuses at Google, decided to use a moronic native implementation of
 * java.regex.Matcher.find() method (findImpl) which takes a String takes it to
 * C-land and gives no fucks about the CharSequence passed to the matcher.
 *
 * Possible solutions for Android would be:
 * - Using Pure Java Matcher implementation (might be slower)
 * - Using a better native implementation than Google's.
 * - Find some other way to stop the execution of matcher.find()
 *
 * Private class, only public to be able to share with other
 * search packages.
 * 
 * @author gubatron
 * @author aldenml
 *
 */
public class MaxIterCharSequence implements CharSequence {

    private final CharSequence inner;
    private int counter;

    public MaxIterCharSequence(CharSequence inner, int counter) {
        this.inner = inner;
        this.counter = Integer.MAX_VALUE; // ignore counter, not used anymore.
    }

    @Override
    public char charAt(int index) {
        if (counter < 0) {
            throw new RuntimeException("Iteration over the string exhausted (index:" + index + ")");
        }
        counter--;
        return inner.charAt(index);
    }

    @Override
    public int length() {
        return inner.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new MaxIterCharSequence(inner.subSequence(start, end), counter);
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}
