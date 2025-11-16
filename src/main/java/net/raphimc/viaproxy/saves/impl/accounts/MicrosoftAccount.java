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
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.viaproxy.ViaProxy;

import java.io.IOException;
import java.util.UUID;

public class MicrosoftAccount extends Account {

    private final JavaAuthManager javaAuthManager;

    public MicrosoftAccount(final JsonObject jsonObject) {
        this.javaAuthManager = JavaAuthManager.fromJson(MinecraftAuth.createHttpClient(), jsonObject);
        if (!this.javaAuthManager.getMinecraftProfile().hasValue()) {
            this.javaAuthManager.getMinecraftProfile().refreshUnchecked();
        }
        this.javaAuthManager.getChangeListeners().add(() -> ViaProxy.getSaveManager().save());
    }

    public MicrosoftAccount(final JavaAuthManager javaAuthManager) throws IOException {
        this.javaAuthManager = javaAuthManager;
        javaAuthManager.getMinecraftToken().refreshIfExpired();
        javaAuthManager.getMinecraftProfile().refreshIfExpired();
        javaAuthManager.getMinecraftPlayerCertificates().refreshIfExpired();
        javaAuthManager.getChangeListeners().add(() -> ViaProxy.getSaveManager().save());
    }

    @Override
    public JsonObject toJson() {
        return JavaAuthManager.toJson(this.javaAuthManager);
    }

    @Override
    public String getName() {
        return this.javaAuthManager.getMinecraftProfile().getCached().getName();
    }

    @Override
    public UUID getUUID() {
        return this.javaAuthManager.getMinecraftProfile().getCached().getId();
    }

    public JavaAuthManager getAuthManager() {
        return this.javaAuthManager;
    }

    @Override
    public String getDisplayString() {
        return this.getName() + " (Microsoft)";
    }

}
