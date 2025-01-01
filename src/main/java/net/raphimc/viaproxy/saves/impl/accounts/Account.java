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
package net.raphimc.viaproxy.saves.impl.accounts;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;

import java.util.UUID;

public abstract class Account {

    private long lastRefresh = 0L;

    public Account() {
    }

    public abstract JsonObject toJson();

    public abstract String getName();

    public abstract UUID getUUID();

    public GameProfile getGameProfile() {
        return new GameProfile(this.getUUID(), this.getName());
    }

    public abstract String getDisplayString();

    public boolean refresh() throws Exception {
        if (System.currentTimeMillis() - this.lastRefresh < 10_000L) {
            return false;
        }
        this.lastRefresh = System.currentTimeMillis();
        return true;
    }

}
