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

import com.frostwire.jlibtorrent.Session;
import com.frostwire.jlibtorrent.SessionSettings;

/**
 * @author gubatron
 * @author aldenml
 */
public final class BTConfigurator {

    private BTConfigurator() {
    }

    public static int getDownloadSpeedLimit() {
        return getSettings().getDownloadRateLimit();
    }

    public static void setDownloadSpeedLimit(int limit) {
        SessionSettings s = getSettings();
        s.setDownloadRateLimit(limit);
        saveSettings(s);
    }

    public static int getUploadSpeedLimit() {
        return getSettings().getUploadRateLimit();
    }

    public static void setUploadSpeedLimit(int limit) {
        SessionSettings s = getSettings();
        s.setUploadRateLimit(limit);
        saveSettings(s);
    }

    public static int getMaxUploads() {
        return getSettings().getActiveSeeds();
    }

    public static void setMaxUploads(int limit) {
        SessionSettings s = getSettings();
        s.setActiveSeeds(limit);
        saveSettings(s);
    }

    private static Session getSession() {
        return BTEngine.getInstance().getSession();
    }

    private static SessionSettings getSettings() {
        return getSession().getSettings();
    }

    private static void saveSettings(SessionSettings s) {
        getSession().setSettings(s);
        BTEngine.getInstance().saveSettings();
    }
}
