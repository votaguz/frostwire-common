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

package com.frostwire.bittorrent;

import com.frostwire.jlibtorrent.FileEntry;
import com.frostwire.jlibtorrent.FileSlice;
import com.frostwire.jlibtorrent.Priority;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.transfers.TransferItem;

import java.io.File;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * @author gubatron
 * @author aldenml
 */
public class BTDownloadItem implements TransferItem {

    private final TorrentHandle th;
    private final int index;

    private final File file;
    private final String name;
    private final long size;

    private final SortedSlices slices;

    public BTDownloadItem(TorrentHandle th, int index, FileEntry fe) {
        this.th = th;
        this.index = index;

        this.file = new File(th.getSavePath(), fe.getPath());
        this.name = file.getName();
        this.size = fe.getSize();

        this.slices = new SortedSlices();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public boolean isSkipped() {
        return th.getFilePriority(index) == Priority.IGNORE;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public long getDownloaded() {
        if (!th.isValid()) {
            return 0;
        }

        long[] progress = th.getFileProgress(TorrentHandle.FileProgressFlags.PIECE_GRANULARITY);
        return progress[index];
    }

    @Override
    public int getProgress() {
        if (!th.isValid() || size == 0) { // edge cases
            return 0;
        }

        int progress;
        long downloaded = getDownloaded();

        if (downloaded == size) {
            progress = 100;
        } else {
            progress = (int) ((float) (getDownloaded() * 100) / (float) size);
        }

        return progress;
    }

    @Override
    public boolean isComplete() {
        return getDownloaded() == size;
    }

    /**
     * This method perform a linear search inside the stored slices, so should be used taking
     * care of potential performance issues.
     *
     * @return
     */
    public long getSequentialDownloaded() {
        Iterator<FileSlice> it = slices.iterator();

        long downloaded = 0;
        boolean done = false;

        while (!done && it.hasNext()) {
            FileSlice slice = it.next();

            if (slice.getOffset() > downloaded) {
                done = true;
            } else {
                downloaded = downloaded + slice.getSize();
            }
        }

        return downloaded;
    }

    void updatePiece(FileSlice slice) {
        slices.add(slice);
    }

    private static final class SortedSlices extends PriorityQueue<FileSlice> {

        public SortedSlices() {
            super(new FileSliceComparator());
        }
    }

    private static final class FileSliceComparator implements Comparator<FileSlice> {

        @Override
        public int compare(FileSlice o1, FileSlice o2) {
            return Long.compare(o1.getOffset(), o2.getOffset());
        }
    }
}
