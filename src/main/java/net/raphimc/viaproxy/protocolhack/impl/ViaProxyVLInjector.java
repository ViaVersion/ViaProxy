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
package net.raphimc.viaproxy.protocolhack.impl;

import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonObject;
import net.raphimc.vialoader.impl.viaversion.VLInjector;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.ViaProxyPlugin;

public class ViaProxyVLInjector extends VLInjector {

    @Override
    public JsonObject getDump() {
        final JsonObject root = new JsonObject();

        root.addProperty("version", ViaProxy.VERSION);
        root.addProperty("impl_version", ViaProxy.IMPL_VERSION);

        final JsonArray plugins = new JsonArray();
        for (ViaProxyPlugin plugin : PluginManager.getPlugins()) {
            final JsonObject pluginObj = new JsonObject();
            pluginObj.addProperty("name", plugin.getName());
            pluginObj.addProperty("version", plugin.getVersion());
            pluginObj.addProperty("author", plugin.getAuthor());
            plugins.add(pluginObj);
        }
        root.add("plugins", plugins);

        return root;
    }

}
