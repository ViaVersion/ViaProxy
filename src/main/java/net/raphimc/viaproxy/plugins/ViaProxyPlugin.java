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
import net.lenni0451.classtransform.utils.loader.InjectionClassLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class ViaProxyPlugin {

    private InjectionClassLoader classLoader;
    private Map<String, Object> viaProxyYaml;
    private boolean enabled;

    final void init(final InjectionClassLoader classLoader, final Map<String, Object> viaProxyYaml) {
        this.classLoader = classLoader;
        this.viaProxyYaml = viaProxyYaml;
    }

    final void enable() {
        this.onEnable();
        this.enabled = true;
    }

    public abstract void onEnable();

    public void registerTransformers(final TransformerManager transformerManager) {
    }

    public final InjectionClassLoader getClassLoader() {
        return this.classLoader;
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

    public boolean isEnabled() {
        return this.enabled;
    }

}
