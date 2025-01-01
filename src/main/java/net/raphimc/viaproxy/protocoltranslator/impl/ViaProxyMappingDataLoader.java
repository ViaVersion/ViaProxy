/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2025 RK_01/RaphiMC and contributors
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

package net.raphimc.viaproxy.protocoltranslator.impl;

import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ViaProxyMappingDataLoader extends MappingDataLoader {

    public static final Map<String, Material> MATERIALS = new HashMap<>();
    public static final Map<String, Map<ProtocolVersion, String>> BLOCK_MATERIALS = new HashMap<>();

    public static final ViaProxyMappingDataLoader INSTANCE = new ViaProxyMappingDataLoader();

    private ViaProxyMappingDataLoader() {
        super(ViaProxyMappingDataLoader.class, "assets/viaproxy/data/");

        final JsonObject materialsData = this.loadData("materials-1.19.4.json");
        for (Map.Entry<String, JsonElement> entry : materialsData.getAsJsonObject("materials").entrySet()) {
            final JsonObject materialData = entry.getValue().getAsJsonObject();
            MATERIALS.put(entry.getKey(), new Material(
                    materialData.get("blocksMovement").getAsBoolean(),
                    materialData.get("burnable").getAsBoolean(),
                    materialData.get("liquid").getAsBoolean(),
                    materialData.get("blocksLight").getAsBoolean(),
                    materialData.get("replaceable").getAsBoolean(),
                    materialData.get("solid").getAsBoolean()
            ));
        }
        for (Map.Entry<String, JsonElement> blockEntry : materialsData.getAsJsonObject("blocks").entrySet()) {
            final Map<ProtocolVersion, String> blockMaterials = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : blockEntry.getValue().getAsJsonObject().entrySet()) {
                blockMaterials.put(ProtocolVersion.getClosest(entry.getKey()), entry.getValue().getAsString());
            }
            BLOCK_MATERIALS.put(blockEntry.getKey(), blockMaterials);
        }
    }

    public record Material(boolean blocksMovement, boolean burnable, boolean liquid, boolean blocksLight, boolean replaceable, boolean solid) {
    }

}
