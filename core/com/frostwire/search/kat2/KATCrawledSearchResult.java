/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
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

package com.frostwire.search.kat2;

import com.frostwire.search.AbstractCrawledSearchResult;
import com.frostwire.search.torrent.TorrentSearchResult;
import org.apache.commons.io.FilenameUtils;

/**
 * @author gubatron
 * @author aldenml
 */
public final class KATCrawledSearchResult extends AbstractCrawledSearchResult<KATSearchResult> implements TorrentSearchResult {

    private final String filePath;
    private final String displayName;
    private final String filename;
    private final long size;

    public KATCrawledSearchResult(KATSearchResult sr, String filePath, long fileSize) {
        super(sr);
        this.filePath = filePath;
        this.filename = FilenameUtils.getName(this.filePath);
        this.size = fileSize;
        this.displayName = FilenameUtils.getBaseName(this.filename);
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public String getTorrentUrl() {
        return parent.getTorrentUrl();
    }

    @Override
    public int getSeeds() {
        return parent.getSeeds();
    }

    @Override
    public String getHash() {
        return parent.getHash();
    }

    @Override
    public String getThumbnailUrl() {
        return parent.getThumbnailUrl();
    }
}
