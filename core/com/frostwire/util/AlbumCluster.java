/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014,, FrostWire(R). All rights reserved.
 *
 * Code heavily inspired on https://github.com/musicbrainz/picard
 * Picard, the next-generation MusicBrainz tagger
 * Copyright (C) 2004 Robert Kaye
 * Copyright (C) 2006 Lukáš Lalinský
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

package com.frostwire.util;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author gubatron
 * @author aldenml
 */
public final class AlbumCluster {

    /**
     * Try to extract album and artist from path.
     *
     * @param filepath
     * @return
     */
    public static final String[] albumArtistFromPath(String filepath, String defaultAlbum, String defaultArtist) {
        String album = defaultAlbum;
        String artist = defaultArtist;

        if (!filepath.contains("/")) { // does not contain directory parts
            return new String[]{album, artist};
        }
        ArrayList<String> dirs = new ArrayList<String>(Arrays.asList(filepath.split("/")));

        if (dirs.get(0).equals("")) {
            dirs.remove(0);
        }

        if (dirs.size() > 0) {
            dirs.remove(dirs.size() - 1);
        }

        // strip disc subdirectory from list
        if (dirs.size() > 0) {
            String last = dirs.get(dirs.size() - 1);
            if (last.matches("(?is)(^|\\s)(CD|DVD|Disc)\\s*\\d+(\\s|$)")) {
                dirs.remove(dirs.size() - 1);
            }
        }

        if (dirs.size() > 0) {
            // for clustering assume %artist%/%album%/file or %artist% - %album%/file
            album = dirs.get(dirs.size() - 1);
            if (album.contains(" - ")) {
                String[] parts = album.split(" - ");
                artist = parts[0];
                album = parts[1];
            } else if (dirs.size() > 1) {
                artist = dirs.get(dirs.size() - 2);
            }
        }

        return new String[]{album, artist};
    }

    /**
     * Remove all non-alphanumeric characters.
     *
     * @param s
     * @return
     */
    public static String stripNonAlnum(String s) {
        return s.replaceAll("\\W+", "");
    }

    /**
     * Strips non-alphanumeric characters from a string unless doing so would make it blank.
     *
     * @param s
     * @return
     */
    public static String normalize(String s) {
        String s1 = stripNonAlnum(s);
        return s1.length() != 0 ? s1 : s;
    }

    /**
     * Compute Levenshtein distance.
     *
     * @param s1
     * @param s2
     * @return
     */
    public static float astrcmp(String s1, String s2) {
        float d = org.apache.commons.lang3.StringUtils.getLevenshteinDistance(s1, s2);
        float len1 = s1.length();
        float len2 = s2.length();

        return (float) 1 - d / Math.max(len1, len2);
    }

    /**
     * Calculates similarity of single words as a function of their edit distance.
     *
     * @param s1
     * @param s2
     * @return
     */
    public static float similarity(String s1, String s2) {
        s1 = normalize(s1);
        s2 = normalize(s2);

        return astrcmp(s1, s2);
    }

    public static final class ClusterDict {

        private Map<String, II> words; // word -> id index
        private Map<Integer, WW> ids; // id -> word, token index
        private int id; // counter for new id generation

        public ClusterDict() {
            this.words = new HashMap<String, II>();
            this.ids = new HashMap<Integer, WW>();
            this.id = 0;
        }

        public int getSize() {
            return id;
        }

        public String tokenize(String word) {
            String token = word.toLowerCase(Locale.US).replaceAll("\\W", "");
            if (token.length() > 0) {
                return token;
            } else {
                return word.toLowerCase(Locale.US).replaceAll("\\s", "");
            }
        }

        /**
         * Add a new entry to the cluster if it does not exist. If it
         * does exist, increment the count. Return the index of the word
         * in the dictionary or -1 is the word is empty.
         *
         * @param word
         */
        public int add(String word) {
            if (org.apache.commons.lang3.StringUtils.isBlank(word)) {
                return -1;
            }

            String token = tokenize(word);

            if (StringUtils.isBlank(token)) {
                return -1;
            }

            if (words.containsKey(word)) {
                II p = words.get(word);
                p.count = p.count + 1;
                return p.index;
            } else {
                int index = this.id;

                words.put(word, new II(this.id, 1));
                ids.put(index, new WW(word, token));

                this.id = this.id + 1;

                return index;
            }
        }

        private static final class II {

            public II(int index, int count) {
                this.index = index;
                this.count = count;
            }

            public int index;
            public int count;
        }

        private static final class WW {

            public WW(String word, String token) {
                this.word = word;
                this.token = token;
            }

            public String word;
            public String token;
        }
    }

    public static void main(String[] args) {
        test_album_artist_from_path();
        test_similarity();
    }

    private static void assertEqual(String[] a, String[] b) {
        if (!Arrays.equals(a, b)) {
            throw new IllegalStateException("Arrays must be equals");
        }
    }

    private static void assertEqual(float a, float b) {
        if (!new Float(a).equals(b)) {
            throw new IllegalStateException("Numbers must be equals");
        }
    }

    private static void assertAlmostEqual(float a, float b, float d) {
        if (Math.abs(a - b) >= d) {
            throw new IllegalStateException("Numbers must be equals");
        }
    }

    private static void test_album_artist_from_path() {
        String file_1 = "/10cc/Original Soundtrack/02 I'm Not in Love.mp3";
        String file_2 = "/10cc - Original Soundtrack/02 I'm Not in Love.mp3";
        String file_3 = "/Original Soundtrack/02 I'm Not in Love.mp3";
        String file_4 = "/02 I'm Not in Love.mp3";

        assertEqual(albumArtistFromPath(file_1, "", ""), new String[]{"Original Soundtrack", "10cc"});
        assertEqual(albumArtistFromPath(file_2, "", ""), new String[]{"Original Soundtrack", "10cc"});
        assertEqual(albumArtistFromPath(file_3, "", ""), new String[]{"Original Soundtrack", ""});
        assertEqual(albumArtistFromPath(file_4, "", ""), new String[]{"", ""});
        assertEqual(albumArtistFromPath(file_4, "album", ""), new String[]{"album", ""});
        assertEqual(albumArtistFromPath(file_4, "", "artist"), new String[]{"", "artist"});
        assertEqual(albumArtistFromPath(file_4, "album", "artist"), new String[]{"album", "artist"});
    }

    private static void test_similarity() {
        assertEqual(similarity("K!", "K!"), 1.0f);
        assertEqual(similarity("BBB", "AAA"), 0.0f);
        assertAlmostEqual(similarity("ABC", "ABB"), 0.7f, 0.1f);
    }
}
