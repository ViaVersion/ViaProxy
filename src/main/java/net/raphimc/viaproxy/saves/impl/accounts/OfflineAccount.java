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
package net.raphimc.viaproxy.saves.impl.accounts;

import com.google.gson.JsonObject;
import net.raphimc.vialegacy.api.util.UuidUtil;

import java.util.UUID;

public class OfflineAccount extends Account {

    private final String name;
    private final UUID uuid;

    public OfflineAccount(JsonObject jsonObject) {
        this.name = jsonObject.get("name").getAsString();
        this.uuid = UUID.fromString(jsonObject.get("uuid").getAsString());
    }

    public OfflineAccount(final String name) {
        this.name = name;
        this.uuid = UuidUtil.createOfflinePlayerUuid(name);
    }

    @Override
    public JsonObject toJson() {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", this.name);
        jsonObject.addProperty("uuid", this.uuid.toString());
        return jsonObject;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    @Override
    public String getDisplayString() {
        return this.name + " (Offline)";
    }

    @Override
    public boolean refresh() {
        return false;
    }

}
