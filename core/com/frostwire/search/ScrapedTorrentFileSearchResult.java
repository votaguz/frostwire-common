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

package com.frostwire.search;

import com.frostwire.search.torrent.AbstractTorrentSearchResult;
import com.frostwire.search.torrent.TorrentSearchResult;
import org.apache.commons.io.FilenameUtils;

/**
 * Created on 4/16/15.
 *
 * When a SearchPerformer crawls a page, its
 * crawlResult(CrawlableSearchResult sr, byte[] data) method is invoked.
 * usually the 'data' holds the HTML with the main information of the torrent
 * and such page may contain information about the torrent's files (TorrentScrapedFileSearchResults)
 * or a link to another page holding those, in which case your implementation of crawlResult
 * would have to make a second request.
 *
 * This class is for you to extend and use when modeling the torrent scraped file search results found.
 * You will initialize such scraped results by passing the main torrent results found by the
 * parent call to crawlResult.
 *
 *
 * In this HTML there might be information about the torrent files
 *
 * @author gubatron
 * @author aldenml
 *
 *
 */
public class ScrapedTorrentFileSearchResult<T extends AbstractTorrentSearchResult> extends AbstractCrawledSearchResult<T> implements TorrentSearchResult {
    private final String filePath;
    private final String displayName;
    private final String filename;
    private final long size;

    public ScrapedTorrentFileSearchResult(T parent, String filePath, long fileSize) {
        super(parent);
        this.filePath = filePath;
        this.filename = FilenameUtils.getName(this.filePath);
        this.size = fileSize;
        this.displayName = FilenameUtils.getBaseName(this.filename);
    }

    // all the data that must be scraped.
    public String getFilePath() { return filePath; }

    @Override
    public String getFilename() { return filename; }

    @Override
    public long getSize() { return size; }

    @Override
    public String getDisplayName() { return displayName; }

    // all data that can be obtained from the parent

    @Override
    public String getTorrentUrl() {
        return getParent().getTorrentUrl();
    }

    @Override
    public int getSeeds() {
        return getParent().getSeeds();
    }

    @Override
    public String getHash() {
        return getParent().getHash();
    }
}