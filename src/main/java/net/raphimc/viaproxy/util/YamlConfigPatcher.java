/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.util;

import net.lenni0451.optconfig.MapConfigLoader;
import net.lenni0451.optconfig.provider.ConfigProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class YamlConfigPatcher {

    private final MapConfigLoader configLoader;
    private final Map<String, Object> config;

    public YamlConfigPatcher(final File file) throws IOException {
        if (!file.exists()) {
            Files.writeString(file.toPath(), "{}");
        }

        this.configLoader = new MapConfigLoader(ConfigProvider.file(file));
        this.config = this.configLoader.load();
    }

    public Map<String, Object> getConfig() {
        return this.config;
    }

    public void write() throws IOException {
        this.configLoader.save(this.config);
    }

}
