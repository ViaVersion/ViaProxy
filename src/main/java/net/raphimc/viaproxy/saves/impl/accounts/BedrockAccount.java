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
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.bedrock.StepMCChain;
import net.raphimc.minecraftauth.step.bedrock.StepPlayFabToken;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession;
import net.raphimc.minecraftauth.step.xbl.StepXblXstsToken;
import net.raphimc.minecraftauth.util.MicrosoftConstants;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.UUID;

public class BedrockAccount extends Account {

    public static final AbstractStep<?, StepFullBedrockSession.FullBedrockSession> DEVICE_CODE_LOGIN = MinecraftAuth.builder()
            .withClientId(MicrosoftConstants.BEDROCK_ANDROID_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
            .deviceCode()
            .withDeviceToken("Android")
            .sisuTitleAuthentication(MicrosoftConstants.BEDROCK_XSTS_RELYING_PARTY)
            .buildMinecraftBedrockChainStep(true, true);

    private StepFullBedrockSession.FullBedrockSession bedrockSession;

    public BedrockAccount(final JsonObject jsonObject) {
        this.bedrockSession = DEVICE_CODE_LOGIN.fromJson(jsonObject.getAsJsonObject("bedrockSession"));
    }

    public BedrockAccount(final StepFullBedrockSession.FullBedrockSession bedrockSession) {
        this.bedrockSession = bedrockSession;
    }

    @Override
    public JsonObject toJson() {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.add("bedrockSession", DEVICE_CODE_LOGIN.toJson(this.bedrockSession));
        return jsonObject;
    }

    @Override
    public String getName() {
        return this.bedrockSession.getMcChain().getDisplayName();
    }

    @Override
    public UUID getUUID() {
        return this.bedrockSession.getMcChain().getId();
    }

    public StepMCChain.MCChain getMcChain() {
        return this.bedrockSession.getMcChain();
    }

    public StepPlayFabToken.PlayFabToken getPlayFabToken() {
        return this.bedrockSession.getPlayFabToken();
    }

    public StepXblXstsToken.XblXsts<?> getRealmsXsts() {
        return this.bedrockSession.getRealmsXsts();
    }

    @Override
    public String getDisplayString() {
        return this.getName() + " (Bedrock)";
    }

    @Override
    public boolean refresh(CloseableHttpClient httpClient) throws Exception {
        if (!super.refresh(httpClient)) return false;

        this.bedrockSession = DEVICE_CODE_LOGIN.refresh(httpClient, this.bedrockSession);
        return true;
    }

}
