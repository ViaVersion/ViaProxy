/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2024 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.plugins;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class ViaProxyPlugin {

    private URLClassLoader classLoader;
    private Map<String, Object> viaProxyYaml;
    private boolean enabled;

    final void init(final URLClassLoader classLoader, final Map<String, Object> viaProxyYaml) {
        this.classLoader = classLoader;
        this.viaProxyYaml = viaProxyYaml;
    }

    final void enable() {
        this.onEnable();
        this.enabled = true;
    }

    public abstract void onEnable();

    final void disable() {
        try {
            this.onDisable();
        } finally {
            this.enabled = false;
        }
    }

    public void onDisable() {
    }

    public final File getDataFolder() {
        final File dataFolder = new File(PluginManager.PLUGINS_DIR, (String) this.viaProxyYaml.get("name"));
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        return dataFolder;
    }

    public final String getName() {
        return (String) this.viaProxyYaml.get("name");
    }

    public final String getAuthor() {
        return (String) this.viaProxyYaml.get("author");
    }

    public final String getVersion() {
        return String.valueOf(this.viaProxyYaml.get("version"));
    }

    public final List<String> getDepends() {
        return Collections.unmodifiableList((List<String>) this.viaProxyYaml.getOrDefault("depends", Collections.emptyList()));
    }

    public final URLClassLoader getClassLoader() {
        return this.classLoader;
    }

    public final boolean isEnabled() {
        return this.enabled;
    }

}
