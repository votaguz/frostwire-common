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

package com.frostwire.bittorrent;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentPrioritizeAlert;
import com.frostwire.logging.Logger;
import com.frostwire.transfers.Transfer;
import com.frostwire.transfers.TransferItem;
import com.frostwire.transfers.TransferState;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.*;

/**
 * @author gubatron
 * @author aldenml
 */
public final class BTDownload extends TorrentAlertAdapter implements Transfer {

    private static final Logger LOG = Logger.getLogger(BTDownload.class);

    private final TorrentHandle th;
    private final File savePath;
    private final Date dateCreated;

    private BTDownloadListener listener;

    public BTDownload(TorrentHandle th) {
        super(th);
        this.th = th;
        this.savePath = new File(th.getSavePath());
        this.dateCreated = new Date(th.getStatus().getAddedTime());

        BTEngine.getInstance().getSession().addListener(this);
    }

    @Override
    public String getName() {
        return th.getName();
    }

    @Override
    public String getDisplayName() {
        Priority[] priorities = th.getFilePriorities();

        int count = 0;
        int index = 0;
        for (int i = 0; i < priorities.length; i++) {
            if (!Priority.IGNORE.equals(priorities[i])) {
                count++;
                index = i;
            }
        }

        return count != 1 ? th.getName() : FilenameUtils.getName(th.getTorrentInfo().getFileAt(index).getPath());
    }

    public long getSize() {
        TorrentInfo ti = th.getTorrentInfo();
        return ti != null ? ti.getTotalSize() : 0;
    }

    public boolean isPaused() {
        return th.getStatus().isPaused();
    }

    public boolean isSeeding() {
        return th.getStatus().isSeeding();
    }

    public boolean isFinished() {
        return th.getStatus().isFinished();
    }

    public TransferState getState() {
        TorrentStatus.State state = th.getStatus().getState();

        if (th.getStatus().isPaused()) {
            return TransferState.PAUSED;
        }

        switch (state) {
            case QUEUED_FOR_CHECKING:
                return TransferState.QUEUED_FOR_CHECKING;
            case CHECKING_FILES:
                return TransferState.CHECKING;
            case DOWNLOADING_METADATA:
                return TransferState.DOWNLOADING_METADATA;
            case DOWNLOADING:
                return TransferState.DOWNLOADING;
            case FINISHED:
                return TransferState.FINISHED;
            case SEEDING:
                return TransferState.SEEDING;
            case ALLOCATING:
                return TransferState.ALLOCATING;
            case CHECKING_RESUME_DATA:
                return TransferState.CHECKING;
            default:
                return TransferState.ERROR;
        }
    }

    @Override
    public File getSavePath() {
        return savePath;
    }

    public int getProgress() {
        float fp = th.getStatus().getProgress();

        if (Float.compare(fp, 1f) == 0) {
            return 100;
        }

        int p = (int) (th.getStatus().getProgress() * 100);
        return Math.min(p, 100);
    }

    public long getBytesReceived() {
        return th.getStatus().getTotalDownload();
    }

    public long getTotalBytesReceived() {
        return th.getStatus().getAllTimeDownload();
    }

    public long getBytesSent() {
        return th.getStatus().getTotalUpload();
    }

    public long getTotalBytesSent() {
        return th.getStatus().getAllTimeUpload();
    }

    public float getDownloadSpeed() {
        return th.getStatus().getDownloadPayloadRate();
    }

    public float getUploadSpeed() {
        return th.getStatus().getUploadPayloadRate();
    }

    public int getConnectedPeers() {
        return th.getStatus().getNumPeers();
    }

    public int getTotalPeers() {
        return th.getStatus().getListPeers();
    }

    public int getConnectedSeeds() {
        return th.getStatus().getNumSeeds();
    }

    public int getTotalSeeds() {
        return th.getStatus().getListSeeds();
    }

    public String getInfoHash() {
        return th.getInfoHash().toString();
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public long getETA() {
        TorrentInfo ti = th.getTorrentInfo();
        if (ti == null) {
            return 0;
        }

        TorrentStatus status = th.getStatus();
        long left = ti.getTotalSize() - status.getTotalDone();
        long rate = status.getDownloadPayloadRate();

        if (left <= 0) {
            return 0;
        }

        if (rate <= 0) {
            return -1;
        }

        return left / rate;
    }

    public void pause() {
        th.setAutoManaged(false);
        th.pause();
    }

    public void resume() {
        th.setAutoManaged(true);
        th.resume();
    }

    public void remove(boolean deleteTorrent, boolean deleteData) {
        String infoHash = this.getInfoHash();

        BTEngine engine = BTEngine.getInstance();
        Session s = engine.getSession();

        s.removeListener(this);

        Set<File> incompleteFiles = getIncompleteFiles();

        if (deleteData) {
            s.removeTorrent(th, Session.Options.DELETE_FILES);
        } else {
            s.removeTorrent(th);
        }

        if (deleteTorrent) {
            File torrent = BTEngine.getInstance().readTorrentPath(infoHash);
            if (torrent != null && torrent.exists()) {
                torrent.delete();
            }
        }

        engine.resumeDataFile(infoHash).delete();
        engine.resumeTorrentFile(infoHash).delete();

        if (listener != null) {
            try {
                listener.removed(this, incompleteFiles);
            } catch (Throwable e) {
                LOG.error("Error calling listener", e);
            }
        }
    }

    public BTDownloadListener getListener() {
        return listener;
    }

    public void setListener(BTDownloadListener listener) {
        this.listener = listener;
    }

    @Override
    public void torrentPrioritize(TorrentPrioritizeAlert alert) {
        if (listener != null) {
            try {
                listener.update(this);
            } catch (Throwable e) {
                LOG.error("Error calling listener", e);
            }
        }
    }

    @Override
    public void torrentFinished(TorrentFinishedAlert alert) {
        if (listener != null) {
            try {
                listener.finished(this);
            } catch (Throwable e) {
                LOG.error("Error calling listener", e);
            }
        }
    }

    public boolean isPartial() {
        Priority[] priorities = th.getFilePriorities();

        for (Priority p : priorities) {
            if (Priority.IGNORE.equals(p)) {
                return true;
            }
        }

        return false;
    }

    public String makeMagnetUri() {
        return th.makeMagnetUri();
    }

    public int getDownloadRateLimit() {
        return th.getDownloadLimit();
    }

    public void setDownloadRateLimit(int limit) {
        th.setDownloadLimit(limit);
        th.saveResumeData();
    }

    public int getUploadRateLimit() {
        return th.getUploadLimit();
    }

    public void setUploadRateLimit(int limit) {
        th.setUploadLimit(limit);
        th.saveResumeData();
    }

    public void requestTrackerAnnounce() {
        th.forceReannounce();
    }

    public void requestTrackerScrape() {
        th.scrapeTracker();
    }

    public Set<String> getTrackers() {
        List<AnnounceEntry> trackers = th.getTrackers();

        Set<String> urls = new HashSet<String>(trackers.size());

        for (AnnounceEntry e : trackers) {
            urls.add(e.getUrl());
        }

        return urls;
    }

    public void setTrackers(Set<String> trackers) {
        List<AnnounceEntry> list = new ArrayList<AnnounceEntry>(trackers.size());

        for (String url : trackers) {
            list.add(new AnnounceEntry(url));
        }

        th.replaceTrackers(list);
        th.saveResumeData();
    }

    @Override
    public List<TransferItem> getItems() {
        List<TransferItem> items = Collections.emptyList();

        if (th.isValid()) {
            TorrentInfo ti = th.getTorrentInfo();
            if (ti != null && ti.isValid()) {
                FileStorage fs = ti.getFiles();
                if (fs.isValid()) {
                    int numFiles = fs.geNumFiles();

                    items = new ArrayList<TransferItem>(numFiles);

                    for (int i = 0; i < numFiles; i++) {
                        items.add(new BTDownloadItem(th, fs, i));
                    }
                }
            }
        }

        return items;
    }

    public File getTorrentFile() {
        return BTEngine.getInstance().readTorrentPath(this.getInfoHash());
    }

    public Set<File> getIncompleteFiles() {
        Set<File> s = new HashSet<File>();

        try {
            long[] progress = th.getFileProgress(TorrentHandle.FileProgressFlags.PIECE_GRANULARITY);

            FileStorage fs = th.getTorrentInfo().getFiles();
            String prefix = savePath.getAbsolutePath();

            for (int i = 0; i < progress.length; i++) {
                if (progress[i] < fs.getFileSize(i)) {
                    s.add(new File(fs.getFilePath(i, prefix)));
                }
            }
        } catch (Throwable e) {
            LOG.error("Error calculating the incomplete files set", e);
        }

        return s;
    }
}
