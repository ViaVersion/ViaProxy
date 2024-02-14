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
package net.raphimc.viaproxy.protocoltranslator;

import com.viaversion.viaversion.util.Config;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigPatcher extends Config {

    private final Map<String, Object> patches;

    public ConfigPatcher(final File configFile, final Map<String, Object> patches) {
        super(configFile);

        this.patches = patches;
        this.reload();
    }

    @Override
    public URL getDefaultConfigURL() {
        return ConfigPatcher.class.getClassLoader().getResource("assets/viaproxy/dummy.yml");
    }

    @Override
    protected void handleConfig(Map<String, Object> config) {
        for (final Map.Entry<String, Object> entry : this.patches.entrySet()) {
            if (!config.containsKey(entry.getKey())) {
                config.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public List<String> getUnsupportedOptions() {
        return new ArrayList<>();
    }

}
