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
import net.raphimc.mcauth.step.bedrock.StepMCChain;
import net.raphimc.mcauth.step.java.StepMCProfile;
import net.raphimc.mcauth.util.MicrosoftConstants;
import net.raphimc.viaproxy.saves.AbstractSave;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount;
import net.raphimc.viaproxy.saves.impl.accounts.OfflineAccount;
import net.raphimc.viaproxy.util.logging.Logger;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NewAccountsSave extends AbstractSave {

    private List<Account> accounts = new ArrayList<>();

    public NewAccountsSave() {
        super("new_accounts");
    }

    @Override
    public void load(JsonElement jsonElement) throws Exception {
        this.accounts = new ArrayList<>();
        for (JsonElement element : jsonElement.getAsJsonArray()) {
            final JsonObject jsonObject = element.getAsJsonObject();
            final String type = jsonObject.get("account_type").getAsString();
            final Class<?> clazz = Class.forName(type);
            final Account account = (Account) clazz.getConstructor(JsonObject.class).newInstance(jsonObject);
            this.accounts.add(account);
        }
    }

    @Override
    public JsonElement save() throws Throwable {
        final JsonArray array = new JsonArray();
        for (Account account : this.accounts) {
            final JsonObject jsonObject = account.toJson();
            jsonObject.addProperty("account_type", account.getClass().getName());
            array.add(jsonObject);
        }
        return array;
    }

    public Account addAccount(final StepMCProfile.MCProfile mcProfile) {
        if (mcProfile.prevResult().items().isEmpty()) {
            return this.addAccount(mcProfile.name());
        } else {
            final Account account = new MicrosoftAccount(mcProfile);
            this.accounts.add(account);
            return account;
        }
    }

    public Account addAccount(final StepMCChain.MCChain mcChain) {
        final Account account = new BedrockAccount(mcChain);
        this.accounts.add(account);
        return account;
    }

    public Account addAccount(final String username) {
        final Account account = new OfflineAccount(username);
        this.accounts.add(account);
        return account;
    }

    public Account addAccount(final int index, final Account account) {
        this.accounts.add(index, account);
        return account;
    }

    public void removeAccount(final Account account) {
        this.accounts.remove(account);
    }

    public void refreshAccounts() {
        for (Account account : new ArrayList<>(this.accounts)) {
            try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
                account.refresh(httpClient);
            } catch (Throwable e) {
                this.accounts.remove(account);
                Logger.LOGGER.error("Failed to refresh account " + account.getName() + ", removing it from the list.", e);
            }
        }
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(this.accounts);
    }

}
