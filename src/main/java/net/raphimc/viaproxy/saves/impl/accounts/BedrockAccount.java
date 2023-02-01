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
import net.raphimc.mcauth.step.bedrock.StepMCChain;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.UUID;

public class BedrockAccount extends Account {

    private StepMCChain.MCChain mcChain;

    public BedrockAccount(final JsonObject jsonObject) throws Throwable {
        this.mcChain = MinecraftAuth.Bedrock.Title.MC_CHAIN.fromJson(jsonObject);
    }

    public BedrockAccount(final StepMCChain.MCChain mcChain) {
        this.mcChain = mcChain;
    }

    @Override
    public JsonObject toJson() {
        return this.mcChain.toJson();
    }

    @Override
    public String getName() {
        return this.mcChain.displayName();
    }

    @Override
    public UUID getUUID() {
        return this.mcChain.id();
    }

    public StepMCChain.MCChain getMcChain() {
        return this.mcChain;
    }

    @Override
    public String getDisplayString() {
        return this.getName() + " (Bedrock)";
    }

    @Override
    public void refresh(CloseableHttpClient httpClient) throws Exception {
        this.mcChain = MinecraftAuth.Bedrock.Title.MC_CHAIN.refresh(httpClient, this.mcChain);
    }

}
