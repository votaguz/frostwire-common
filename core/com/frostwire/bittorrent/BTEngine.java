package com.frostwire.bittorrent;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.SaveResumeDataAlert;
import com.frostwire.jlibtorrent.alerts.TorrentAlert;
import com.frostwire.jlibtorrent.swig.entry;
import com.frostwire.logging.Logger;
import com.frostwire.search.torrent.TorrentCrawledSearchResult;
import com.frostwire.util.OSUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author gubatron
 * @author aldenml
 */
public final class BTEngine {

    private static final Logger LOG = Logger.getLogger(BTEngine.class);

    public static BTContext ctx;

    private final ReentrantLock sync = new ReentrantLock();
    private final InnerListener innerListener = new InnerListener();

    private Session session;
    private Downloader downloader;
    private SessionSettings defaultSettings;

    private boolean firewalled;
    private BTEngineListener listener;

    private BTEngine() {
    }

    private static class Loader {
        static BTEngine INSTANCE = new BTEngine();
    }

    public static BTEngine getInstance() {
        if (ctx == null) {
            throw new IllegalStateException("Context can't be null");
        }
        return Loader.INSTANCE;
    }

    public Session getSession() {
        return session;
    }

    private SessionSettings getSettings() {
        if (session == null) {
            return null;
        }

        return session.getSettings();
    }

    public BTEngineListener getListener() {
        return listener;
    }

    public void setListener(BTEngineListener listener) {
        this.listener = listener;
    }

    public boolean isFirewalled() {
        return firewalled;
    }

    public long getDownloadRate() {
        if (session == null) {
            return 0;
        }

        return session.getStatus().getDownloadRate();
    }

    public long getUploadRate() {
        if (session == null) {
            return 0;
        }

        return session.getStatus().getUploadRate();
    }

    public long getTotalDownload() {
        if (session == null) {
            return 0;
        }

        return session.getStatus().getTotalDownload();
    }

    public long getTotalUpload() {
        if (session == null) {
            return 0;
        }

        return session.getStatus().getTotalUpload();
    }

    public int getDownloadRateLimit() {
        if (session == null) {
            return 0;
        }

        return session.getSettings().getDownloadRateLimit();
    }

    public int getUploadRateLimit() {
        if (session == null) {
            return 0;
        }

        return session.getSettings().getDownloadRateLimit();
    }

    public boolean isStarted() {
        return session != null;
    }

    public void start() {
        sync.lock();

        try {
            if (session != null) {
                return;
            }

            Pair<Integer, Integer> prange = new Pair<Integer, Integer>(ctx.port0, ctx.port1);
            session = new Session(prange, ctx.iface);

            downloader = new Downloader(session);
            defaultSettings = session.getSettings();

            loadSettings();
            session.addListener(innerListener);

        } finally {
            sync.unlock();
        }
    }

    public void stop() {
        sync.lock();

        try {
            if (session == null) {
                return;
            }

            session.removeListener(innerListener);
            saveSettings();

            downloader = null;
            defaultSettings = null;

            session.abort();
            session = null;

        } finally {
            sync.unlock();
        }
    }

    public void restart() {
        sync.lock();

        try {

            stop();
            Thread.sleep(1000); // allow some time to release native resources
            start();

        } catch (InterruptedException e) {
            // ignore
        } finally {
            sync.unlock();
        }
    }

    public void pause() {
        if (session != null && !session.isPaused()) {
            session.pause();
        }
    }

    public void resume() {
        if (session != null) {
            session.resume();
        }
    }

    public void loadSettings() {
        if (session == null) {
            return;
        }

        try {
            File f = settingsFile();
            if (f.exists()) {
                byte[] data = FileUtils.readFileToByteArray(f);
                session.loadState(data);
            } else {
                revertToDefaultConfiguration();
            }
        } catch (Throwable e) {
            LOG.error("Error loading session state", e);
        }
    }

    public void saveSettings() {
        if (session == null) {
            return;
        }

        try {
            byte[] data = session.saveState();
            FileUtils.writeByteArrayToFile(settingsFile(), data);
        } catch (Throwable e) {
            LOG.error("Error saving session state", e);
        }
    }

    private void saveSettings(SessionSettings s) {
        if (session == null) {
            return;
        }

        session.setSettings(s);
        saveSettings();
    }

    public void revertToDefaultConfiguration() {
        if (session == null) {
            return;
        }

        session.setSettings(defaultSettings);

        if (OSUtils.isAndroid()) {
            SessionSettings s = session.getSettings(); // working with a copy?
            // TODO: modify this for android
            session.setSettings(s);
        }

        saveSettings();
    }

    public void download(File torrent, File saveDir) {
        download(torrent, saveDir, null);
    }

    public void download(File torrent, File saveDir, boolean[] selection) {
        if (session == null) {
            return;
        }

        if (saveDir == null) {
            saveDir = ctx.dataDir;
        }

        TorrentInfo ti = new TorrentInfo(torrent);

        Priority[] priorities = null;

        if (selection != null) {
            TorrentHandle th = downloader.find(ti.getInfoHash());

            if (th != null) {
                priorities = th.getFilePriorities();
            } else {
                priorities = Priority.array(Priority.IGNORE, ti.getNumFiles());
            }

            for (int i = 0; i < selection.length; i++) {
                if (selection[i]) {
                    priorities[i] = Priority.NORMAL;
                }
            }
        }

        downloader.download(ti, saveDir, priorities, null);

        saveResumeTorrent(torrent);
    }

    public void download(TorrentInfo ti, File saveDir, boolean[] selection) {
        if (session == null) {
            return;
        }

        if (saveDir == null) {
            saveDir = ctx.dataDir;
        }

        Priority[] priorities = null;

        if (selection != null) {
            TorrentHandle th = downloader.find(ti.getInfoHash());

            if (th != null) {
                priorities = th.getFilePriorities();
            } else {
                priorities = Priority.array(Priority.IGNORE, ti.getNumFiles());
            }

            for (int i = 0; i < selection.length; i++) {
                if (selection[i]) {
                    priorities[i] = Priority.NORMAL;
                }
            }
        }

        downloader.download(ti, saveDir, priorities, null);

        File torrent = saveTorrent(ti);
        saveResumeTorrent(torrent);
    }

    public void download(TorrentCrawledSearchResult sr, File saveDir) {
        if (session == null) {
            return;
        }

        if (saveDir == null) {
            saveDir = ctx.dataDir;
        }

        TorrentInfo ti = sr.getTorrentInfo();
        int fileIndex = sr.getFileIndex();

        TorrentHandle th = downloader.find(ti.getInfoHash());

        if (th != null) {
            Priority[] priorities = th.getFilePriorities();
            if (priorities[fileIndex] == Priority.IGNORE) {
                priorities[fileIndex] = Priority.NORMAL;
                downloader.download(ti, saveDir, priorities, null);
            }
        } else {
            Priority[] priorities = Priority.array(Priority.IGNORE, ti.getNumFiles());
            priorities[fileIndex] = Priority.NORMAL;
            downloader.download(ti, saveDir, priorities, null);
        }

        File torrent = saveTorrent(ti);
        saveResumeTorrent(torrent);
    }

    public byte[] fetchMagnet(String uri, long timeout) {
        if (session == null) {
            return null;
        }

        return downloader.fetchMagnet(uri, timeout);
    }

    public void restoreDownloads() {
        if (session == null) {
            return;
        }

        File[] torrents = ctx.homeDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return FilenameUtils.getExtension(name).equals("torrent");
            }
        });

        for (File t : torrents) {
            try {
                File resumeFile = new File(ctx.homeDir, FilenameUtils.getBaseName(t.getName()) + ".resume");
                session.asyncAddTorrent(t, null, resumeFile);
            } catch (Throwable e) {
                LOG.error("Error restoring torrent download: " + t, e);
            }
        }
    }

    File settingsFile() {
        return new File(ctx.homeDir, "settings.dat");
    }

    File resumeTorrentFile(String infoHash) {
        return new File(ctx.homeDir, infoHash + ".torrent");
    }

    File resumeDataFile(String infoHash) {
        return new File(ctx.homeDir, infoHash + ".resume");
    }

    File readTorrentPath(String infoHash) {
        File torrent = null;

        try {
            byte[] arr = FileUtils.readFileToByteArray(resumeTorrentFile(infoHash));
            entry e = entry.bdecode(Vectors.bytes2char_vector(arr));
            torrent = new File(e.dict().get("torrent_orig_path").string());
        } catch (Throwable e) {
            // can't recover original torrent path
        }

        return torrent;
    }

    private File saveTorrent(TorrentInfo ti) {
        File torrentFile;

        try {
            String name = ti.getName();
            if (name == null || name.length() == 0) {
                name = ti.getInfoHash().toString();
            }
            name = OSUtils.escapeFilename(name);

            torrentFile = new File(ctx.torrentsDir, name + ".torrent");
            byte[] arr = ti.toEntry().bencode();

            FileUtils.writeByteArrayToFile(torrentFile, arr);
        } catch (Throwable e) {
            torrentFile = null;
            LOG.warn("Error saving torrent info to file", e);
        }

        return torrentFile;
    }

    private void saveResumeTorrent(File torrent) {
        try {
            TorrentInfo ti = new TorrentInfo(torrent);
            entry e = ti.toEntry().getSwig();
            e.dict().set("torrent_orig_path", new entry(torrent.getAbsolutePath()));
            byte[] arr = Vectors.char_vector2bytes(e.bencode());
            FileUtils.writeByteArrayToFile(resumeTorrentFile(ti.getInfoHash().toString()), arr);
        } catch (Throwable e) {
            LOG.warn("Error saving resume torrent", e);
        }
    }

    private void saveResumeData(SaveResumeDataAlert alert) {
        try {
            TorrentHandle th = alert.getHandle();
            if (th.isValid()) {
                String infoHash = th.getInfoHash().toString();
                byte[] arr = alert.getResumeData().bencode();
                FileUtils.writeByteArrayToFile(resumeDataFile(infoHash), arr);
            }
        } catch (Throwable e) {
            LOG.warn("Error saving resume data", e);
        }
    }

    private void doResumeData(TorrentAlert<?> alert) {
        TorrentHandle th = alert.getHandle();
        if (th.isValid() && th.needSaveResumeData()) {
            th.saveResumeData();
        }
    }

    private final class InnerListener implements AlertListener {
        @Override
        public void alert(Alert<?> alert) {
            //LOG.info(a.message());

            AlertType type = alert.getType();

            switch (type) {
                case TORRENT_ADDED:
                    if (listener != null) {
                        listener.downloadAdded(new BTDownload(((TorrentAlert<?>) alert).getHandle()));
                        doResumeData((TorrentAlert<?>) alert);
                    }
                    break;
                case SAVE_RESUME_DATA:
                    saveResumeData((SaveResumeDataAlert) alert);
                    break;
                case BLOCK_FINISHED:
                    doResumeData((TorrentAlert<?>) alert);
                    break;
                case PORTMAP:
                    firewalled = false;
                    break;
                case PORTMAP_ERROR:
                    firewalled = true;
                    break;
            }
        }
    }

    //--------------------------------------------------
    // Settings methods
    //--------------------------------------------------

    public int getDownloadSpeedLimit() {
        if (session == null) {
            return 0;
        }

        return getSettings().getDownloadRateLimit();
    }

    public void setDownloadSpeedLimit(int limit) {
        if (session == null) {
            return;
        }

        SessionSettings s = getSettings();
        s.setDownloadRateLimit(limit);
        saveSettings(s);
    }

    public int getUploadSpeedLimit() {
        if (session == null) {
            return 0;
        }

        return getSettings().getUploadRateLimit();
    }

    public void setUploadSpeedLimit(int limit) {
        if (session == null) {
            return;
        }

        SessionSettings s = getSettings();
        s.setUploadRateLimit(limit);
        saveSettings(s);
    }

    public int getMaxActiveDownloads() {
        if (session == null) {
            return 0;
        }

        return getSettings().getActiveDownloads();
    }

    public void setMaxActiveDownloads(int limit) {
        if (session == null) {
            return;
        }

        SessionSettings s = getSettings();
        s.setActiveDownloads(limit);
        saveSettings(s);
    }

    public int getMaxActiveSeeds() {
        if (session == null) {
            return 0;
        }

        return getSettings().getActiveSeeds();
    }

    public void setMaxActiveSeeds(int limit) {
        if (session == null) {
            return;
        }

        SessionSettings s = getSettings();
        s.setActiveSeeds(limit);
        saveSettings(s);
    }

    public int getMaxConnections() {
        if (session == null) {
            return 0;
        }

        return getSettings().getConnectionsLimit();
    }

    public void setMaxConnections(int limit) {
        if (session == null) {
            return;
        }

        SessionSettings s = getSettings();
        s.setConnectionsLimit(limit);
        saveSettings(s);
    }

    public int getMaxPeers() {
        if (session == null) {
            return 0;
        }

        return getSettings().getMaxPeerlistSize();
    }

    public void setMaxPeers(int limit) {
        if (session == null) {
            return;
        }

        SessionSettings s = getSettings();
        s.setMaxPeerlistSize(limit);
        saveSettings(s);
    }
}
