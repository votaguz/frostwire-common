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
import com.frostwire.logging.Logger;
import com.frostwire.transfers.Transfer;
import com.frostwire.transfers.TransferItem;
import com.frostwire.transfers.TransferState;

import java.io.File;
import java.util.*;

/**
 * @author gubatron
 * @author aldenml
 */
public final class BTDownload extends TorrentAlertAdapter implements Transfer {

    private static final Logger LOG = Logger.getLogger(BTDownload.class);

    private final TorrentHandle th;

    private BTDownloadListener listener;

    public BTDownload(TorrentHandle th) {
        super(th);
        this.th = th;

        BTEngine.getInstance().getSession().addListener(this);
    }

    public String getName() {
        return th.getName();
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
                throw new IllegalArgumentException("No enum value supported");
        }
    }

    public String getSavePath() {
        return th.getSavePath();
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
        return th.getStatus().listPeers;
    }

    public int getConnectedSeeds() {
        return th.getStatus().getNumSeeds();
    }

    public int getTotalSeeds() {
        return th.getStatus().listSeeds;
    }

    public String getInfoHash() {
        return th.getInfoHash().toString();
    }

    public Date getDateCreated() {
        return new Date(th.getStatus().getAddedTime());
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
        th.pause();
    }

    public void resume() {
        th.resume();
    }

    public void stop() {
        this.stop(false, false);
    }

    public void stop(boolean deleteTorrent, boolean deleteData) {
        String infoHash = this.getInfoHash();

        BTEngine engine = BTEngine.getInstance();
        Session s = engine.getSession();

        s.removeListener(this);

        if (deleteData) {
            s.removeTorrent(th, Session.Options.DELETE_FILES);
        } else {
            s.removeTorrent(th);
        }

        if (deleteTorrent) {
            File torrent = BTEngine.getInstance().readTorrentPath(infoHash);
            if (torrent.exists()) {
                torrent.delete();
            }
        }

        engine.resumeDataFile(infoHash).delete();
        engine.resumeTorrentFile(infoHash).delete();

        if (listener != null) {
            try {
                listener.stopped(this);
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

    TorrentHandle getTorrentHandle() {
        return th;
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
        // TODO:BITTORRENT
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
                    String savePath = th.getSavePath();

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

    public void setFilesSelection(boolean[] filesSelection) {
        Priority[] priorities = new Priority[filesSelection.length];

        for (int i = 0; i < priorities.length; i++) {
            priorities[i] = filesSelection[i] ? Priority.NORMAL : Priority.IGNORE;
        }
        th.prioritizeFiles(priorities);
    }
}
