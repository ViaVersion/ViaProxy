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
package net.raphimc.viaproxy.saves.impl.accounts;

import com.google.gson.JsonObject;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.java.StepMCProfile;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.UUID;

public class MicrosoftAccount extends Account {

    private StepMCProfile.MCProfile mcProfile;

    public MicrosoftAccount(final JsonObject jsonObject) throws Throwable {
        this.mcProfile = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(jsonObject);
    }

    public MicrosoftAccount(final StepMCProfile.MCProfile mcProfile) {
        this.mcProfile = mcProfile;
    }

    @Override
    public JsonObject toJson() {
        return this.mcProfile.toJson();
    }

    @Override
    public String getName() {
        return this.mcProfile.name();
    }

    @Override
    public UUID getUUID() {
        return this.mcProfile.id();
    }

    public StepMCProfile.MCProfile getMcProfile() {
        return this.mcProfile;
    }

    @Override
    public String getDisplayString() {
        return this.getName() + " (Microsoft)";
    }

    @Override
    public void refresh(CloseableHttpClient httpClient) throws Exception {
        this.mcProfile = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.refresh(httpClient, this.mcProfile);
    }

}
