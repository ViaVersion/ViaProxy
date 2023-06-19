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
import net.raphimc.mcauth.step.java.StepPlayerCertificates;
import net.raphimc.viaproxy.util.logging.Logger;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.UUID;

public class MicrosoftAccount extends Account {

    private StepMCProfile.MCProfile mcProfile;
    private StepPlayerCertificates.PlayerCertificates playerCertificates;

    public MicrosoftAccount(final JsonObject jsonObject) throws Throwable {
        this.mcProfile = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(jsonObject.getAsJsonObject("mc_profile"));
        if (jsonObject.has("player_certificates")) {
            try {
                this.playerCertificates = MinecraftAuth.JAVA_PLAYER_CERTIFICATES.fromJson(jsonObject.getAsJsonObject("player_certificates"));
            } catch (Throwable e) {
                Logger.LOGGER.warn("Failed to load player certificates for Microsoft account. They will be regenerated.", e);
            }
        }
    }

    public MicrosoftAccount(final StepMCProfile.MCProfile mcProfile) {
        this.mcProfile = mcProfile;
    }

    @Override
    public JsonObject toJson() {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.add("mc_profile", this.mcProfile.toJson());
        if (this.playerCertificates != null) {
            jsonObject.add("player_certificates", this.playerCertificates.toJson());
        }
        return jsonObject;
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

    public StepPlayerCertificates.PlayerCertificates getPlayerCertificates() {
        return this.playerCertificates;
    }

    @Override
    public String getDisplayString() {
        return this.getName() + " (Microsoft)";
    }

    @Override
    public void refresh(CloseableHttpClient httpClient) throws Exception {
        this.mcProfile = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.refresh(httpClient, this.mcProfile);

        try {
            if (this.playerCertificates == null) {
                throw new NullPointerException();
            }
            this.playerCertificates = MinecraftAuth.JAVA_PLAYER_CERTIFICATES.refresh(httpClient, this.playerCertificates);
        } catch (Throwable e) {
            this.playerCertificates = null;
            this.playerCertificates = MinecraftAuth.JAVA_PLAYER_CERTIFICATES.getFromInput(httpClient, this.mcProfile.prevResult().prevResult());
        }
    }

}
