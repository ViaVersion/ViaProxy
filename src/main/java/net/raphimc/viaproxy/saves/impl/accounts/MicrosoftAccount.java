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
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.java.StepMCProfile;
import net.raphimc.minecraftauth.step.java.StepPlayerCertificates;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.util.MicrosoftConstants;

import java.util.UUID;

public class MicrosoftAccount extends Account {

    public static final AbstractStep<?, StepFullJavaSession.FullJavaSession> DEVICE_CODE_LOGIN = MinecraftAuth.builder()
            .withClientId(MicrosoftConstants.JAVA_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
            .deviceCode()
            .withDeviceToken("Win32")
            .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
            .buildMinecraftJavaProfileStep(true);

    private StepFullJavaSession.FullJavaSession javaSession;

    public MicrosoftAccount(final JsonObject jsonObject) {
        this.javaSession = DEVICE_CODE_LOGIN.fromJson(jsonObject.getAsJsonObject("javaSession"));
    }

    public MicrosoftAccount(final StepFullJavaSession.FullJavaSession javaSession) {
        this.javaSession = javaSession;
    }

    @Override
    public JsonObject toJson() {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.add("javaSession", DEVICE_CODE_LOGIN.toJson(this.javaSession));
        return jsonObject;
    }

    @Override
    public String getName() {
        return this.javaSession.getMcProfile().getName();
    }

    @Override
    public UUID getUUID() {
        return this.javaSession.getMcProfile().getId();
    }

    public StepMCProfile.MCProfile getMcProfile() {
        return this.javaSession.getMcProfile();
    }

    public StepPlayerCertificates.PlayerCertificates getPlayerCertificates() {
        return this.javaSession.getPlayerCertificates();
    }

    @Override
    public String getDisplayString() {
        return this.getName() + " (Microsoft)";
    }

    @Override
    public boolean refresh() throws Exception {
        if (!super.refresh()) return false;

        this.javaSession = DEVICE_CODE_LOGIN.refresh(MinecraftAuth.createHttpClient(), this.javaSession);
        return true;
    }

}
