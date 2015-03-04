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

package com.frostwire.search.domainalias;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Default Domain Alias Manifest "fetcher", creates and holds a Domain Alias manifest in memory
 * that can be used while we fetch a signed one from another source.
 * @author gubatron
 *
 */
public class DefaultDomainAliasManifestFetcher extends AbstractDomainAliasManifestFetcher {

    
    public DefaultDomainAliasManifestFetcher(DomainAliasManifestFetcherListener listener) {
        super(listener);
    }

    @Override
    public void fetchManifest() {
        DomainAliasManifest manifest = new DomainAliasManifest();
        manifest.lastUpdated = System.currentTimeMillis();
        manifest.version = 0;
        manifest.aliases = new HashMap<String, List<String>>();

        List<String> katAliases = new ArrayList<String>();
        manifest.aliases.put("kickass.to", katAliases);
        
        List<String> tpbAliases = new ArrayList<String>();
        manifest.aliases.put("thepiratebay.se", tpbAliases);

        List<String> extraTorrentAliases = new ArrayList<String>();
        manifest.aliases.put("extratorrent.cc", extraTorrentAliases);//Collections.<String> emptyList());

        notifyManifestFetched(manifest);
    }
}
