/*
 * Copyright 2017 Boleslav Bobcik - Auderis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.auderis.tools.gradle.semver;

import java.util.AbstractList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

class VersionOverrideSourceList extends AbstractList<VersionOverrideSource> {

    private final List<VersionOverrideSource> sources;
    private VersionOverrideSource defaultSource;

    VersionOverrideSourceList(VersionOverrideSource defaultSource) {
        this.defaultSource = defaultSource;
        this.sources = new LinkedList<>();
    }

    @Override
    public VersionOverrideSource get(int index) {
        if (!sources.isEmpty()) {
            return sources.get(index);
        } else if ((null != defaultSource) && (0 == index)) {
            return defaultSource;
        }
        throw new NoSuchElementException("override source with index " + index + " does not exist");
    }

    @Override
    public int size() {
        int size = sources.size();
        if ((0 == size) && (null != defaultSource)) {
            size = 1;
        }
        return size;
    }

    @Override
    public boolean add(VersionOverrideSource src) {
        if (null == src) {
            return false;
        }
        return sources.add(src);
    }

    @Override
    public void clear() {
        sources.clear();
        defaultSource = null;
    }

    VersionOverrideSource getDefaultSource() {
        return defaultSource;
    }

    void setDefaultSource(VersionOverrideSource defaultSource) {
        this.defaultSource = defaultSource;
    }

}
