/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
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

import net.lenni0451.classtransform.TransformerManager;

import java.util.Map;

public abstract class ViaProxyPlugin {

    private Map<String, Object> viaProxyYaml;

    final void init(final Map<String, Object> viaProxyYaml) {
        this.viaProxyYaml = viaProxyYaml;
    }

    public abstract void onEnable();

    public void registerTransformers(final TransformerManager transformerManager) {
    }

    public final String getName() {
        return (String) this.viaProxyYaml.get("name");
    }

    public final String getAuthor() {
        return (String) this.viaProxyYaml.get("author");
    }

    public final String getVersion() {
        return (String) this.viaProxyYaml.get("version");
    }

}
