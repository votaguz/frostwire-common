/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014,, FrostWire(R). All rights reserved.
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

package com.frostwire.search.extratorrent;

import com.frostwire.logging.Logger;
import com.frostwire.search.MaxIterCharSequence;
import com.frostwire.search.SearchMatcher;
import com.frostwire.search.SearchResult;
import com.frostwire.search.domainalias.DomainAliasManager;
import com.frostwire.search.torrent.TorrentCrawlableSearchResult;
import com.frostwire.search.torrent.TorrentJsonSearchPerformer;
import com.frostwire.util.HtmlManipulator;
import com.frostwire.util.JsonUtils;
import com.google.code.regexp.Pattern;

import java.util.*;

/**
 * @author gubatron
 * @author aldenml
 *
 */
public class ExtratorrentSearchPerformer extends TorrentJsonSearchPerformer<ExtratorrentItem, ExtratorrentSearchResult> {

    private static final Logger LOG = Logger.getLogger(ExtratorrentSearchPerformer.class);

    private static final Map<String, Integer> UNIT_TO_BYTES;

    static {
        UNIT_TO_BYTES = new HashMap<String, Integer>();
        UNIT_TO_BYTES.put("bytes", 1);
        UNIT_TO_BYTES.put("B", 1);
        UNIT_TO_BYTES.put("KB", 1024);
        UNIT_TO_BYTES.put("MB", 1024 * 1024);
        UNIT_TO_BYTES.put("GB", 1024 * 1024 * 1024);
    }

    private static final String FILES_REGEX = "(?is)<tr>.*?<td.*?<img.*?<img.*?<td colspan.*?nowrap=\"nowrap\">(?<filename>[^<>]*?)&nbsp;<font.*?>\\((?<size>.*?)&nbsp;(?<unit>.*?)\\)</font>.*?</td></tr>";
    private static final Pattern FILES_PATTERN = Pattern.compile(FILES_REGEX);

    public ExtratorrentSearchPerformer(DomainAliasManager domainAliasManager, long token, String keywords, int timeout) {
        super(domainAliasManager, token, keywords, timeout, 1);
    }

    @Override
    protected String getUrl(int page, String encodedKeywords) {
        return "http://extratorrent.cc/json/?search=" + encodedKeywords;
    }

    @Override
    protected List<ExtratorrentItem> parseJson(String json) {
        ExtratorrentResponse response = JsonUtils.toObject(json, ExtratorrentResponse.class);
        for (ExtratorrentItem item : response.list) {
            item.link = item.link.replaceAll("extratorrent.com", "extratorrent.cc");
            item.torrentLink = item.torrentLink.replaceAll("extratorrent.com","extratorrent.cc");
        }
        return response.list;
    }

    @Override
    protected List<? extends SearchResult> crawlResult(TorrentCrawlableSearchResult sr, byte[] data) throws Exception {
        if (!(sr instanceof ExtratorrentSearchResult)) {
            return Collections.emptyList();
        }

        List<SearchResult> result = new LinkedList<SearchResult>();

        ExtratorrentSearchResult esr = (ExtratorrentSearchResult) sr;
        final String torrent_files_url = esr.getDetailsUrl().replace("/torrent/","/torrent_files/");
        String completePage = fetch(torrent_files_url);
        String page = completePage.substring(completePage.indexOf("Torrent files list"),completePage.indexOf("Recent Searches"));

        SearchMatcher matcher = SearchMatcher.from(FILES_PATTERN.matcher(new MaxIterCharSequence(page, 2 * page.length())));

        while (matcher.find()) {
            try {
                String filename = HtmlManipulator.replaceHtmlEntities(matcher.group("filename"));
                String sizeStr = matcher.group("size");
                String unit = matcher.group("unit");

                double size = Double.parseDouble(sizeStr);

                if (UNIT_TO_BYTES.containsKey(unit)) {
                    size = size * UNIT_TO_BYTES.get(unit);
                } else {
                    size = -1;
                }

                result.add(new ExtratorrentCrawledSearchResult(esr, filename, (int) size));

            } catch (Throwable e) {
                LOG.warn("Error creating single file search result", e);
            }
        }

        return result;
    }


    @Override
    protected ExtratorrentSearchResult fromItem(ExtratorrentItem item) {
        return new ExtratorrentSearchResult(item);
    }
}
