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

package com.frostwire.bittorrent.libtorrent;

import com.frostwire.bittorrent.BTDownloadItem;
import com.frostwire.jlibtorrent.FileStorage;
import com.frostwire.jlibtorrent.Priority;
import com.frostwire.jlibtorrent.TorrentHandle;

import java.io.File;

/**
 * @author gubatron
 * @author aldenml
 */
public final class LTDownloadItem implements BTDownloadItem {

    private final TorrentHandle th;
    private final FileStorage fs;
    private final int index;

    public LTDownloadItem(TorrentHandle th, FileStorage fs, int index) {
        this.th = th;
        this.fs = fs;
        this.index = index;
    }

    public TorrentHandle getTorrentHandle() {
        return th;
    }

    public FileStorage getFileStorage() {
        return fs;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public boolean isSkipped() {
        return th.getFilePriority(index) == Priority.IGNORE;
    }

    @Override
    public File getFile() {
        return new File(fs.getFilePath(index, th.getSavePath()));
    }

    @Override
    public long getSize() {
        return fs.getFileSize(index);
    }

    @Override
    public long getDownloaded() {
        long[] progress = th.getFileProgress(TorrentHandle.FileProgressFlags.PIECE_GRANULARITY);
        return progress[index];
    }
}
