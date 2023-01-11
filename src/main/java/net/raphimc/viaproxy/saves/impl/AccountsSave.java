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
package net.raphimc.viaproxy.saves.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.java.StepGameOwnership;
import net.raphimc.mcauth.step.java.StepMCProfile;
import net.raphimc.mcauth.util.MicrosoftConstants;
import net.raphimc.viaproxy.saves.AbstractSave;
import net.raphimc.viaproxy.util.logging.Logger;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AccountsSave extends AbstractSave {

    private List<StepMCProfile.MCProfile> accounts = new ArrayList<>();

    public AccountsSave() {
        super("accounts");
    }

    @Override
    public void load(JsonElement jsonElement) throws Exception {
        this.accounts = new ArrayList<>();
        for (JsonElement element : jsonElement.getAsJsonArray()) {
            final JsonObject object = element.getAsJsonObject();
            if (object.has("is_offline_mode_account") && object.get("is_offline_mode_account").getAsBoolean()) {
                this.addOfflineAccount(object.get("name").getAsString());
            } else {
                this.addAccount(MinecraftAuth.Java.Title.MC_PROFILE.fromJson(object));
            }
        }
    }

    @Override
    public JsonElement save() {
        final JsonArray array = new JsonArray();
        for (StepMCProfile.MCProfile account : this.accounts) {
            if (account.prevResult().items().isEmpty()) {
                final JsonObject object = new JsonObject();
                object.addProperty("is_offline_mode_account", true);
                object.addProperty("name", account.name());
                array.add(object);
            } else {
                array.add(account.toJson());
            }
        }
        return array;
    }

    public StepMCProfile.MCProfile addAccount(final StepMCProfile.MCProfile profile) {
        this.accounts.add(profile);
        return profile;
    }

    public StepMCProfile.MCProfile addAccount(final int index, final StepMCProfile.MCProfile profile) {
        this.accounts.add(index, profile);
        return profile;
    }

    public StepMCProfile.MCProfile addOfflineAccount(final String name) {
        return this.addAccount(new StepMCProfile.MCProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes()), name, null, new StepGameOwnership.GameOwnership(Collections.emptyList(), null)));
    }

    public void removeAccount(final StepMCProfile.MCProfile profile) {
        this.accounts.remove(profile);
    }

    public void refreshAccounts() {
        final List<StepMCProfile.MCProfile> accounts = new ArrayList<>();
        for (StepMCProfile.MCProfile account : this.accounts) {
            if (account.prevResult().items().isEmpty()) {
                accounts.add(account);
                continue;
            }

            try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
                accounts.add(MinecraftAuth.Java.Title.MC_PROFILE.refresh(httpClient, account));
            } catch (Throwable e) {
                Logger.LOGGER.error("Failed to refresh account " + account.name() + ", removing it from the list.", e);
            }
        }
        this.accounts = accounts;
    }

    public List<StepMCProfile.MCProfile> getAccounts() {
        return Collections.unmodifiableList(this.accounts);
    }

}
