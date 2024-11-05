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
package net.raphimc.viaproxy.saves.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.lenni0451.reflect.Classes;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.ViaProxyPlugin;
import net.raphimc.viaproxy.saves.AbstractSave;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.saves.impl.accounts.OfflineAccount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountsSaveV3 extends AbstractSave {

    private List<Account> accounts = new ArrayList<>();

    public AccountsSaveV3() {
        super("accountsV3");
    }

    @Override
    public void load(JsonElement jsonElement) throws Exception {
        final List<ClassLoader> classLoaders = new ArrayList<>();
        classLoaders.add(ViaProxy.class.getClassLoader());
        classLoaders.addAll(ViaProxy.getPluginManager().getPlugins().stream().map(ViaProxyPlugin::getClassLoader).toList());

        this.accounts = new ArrayList<>();
        for (JsonElement element : jsonElement.getAsJsonArray()) {
            final JsonObject jsonObject = element.getAsJsonObject();
            final String type = jsonObject.get("accountType").getAsString();
            final Class<?> clazz = Classes.find(type, true, classLoaders);

            final Account account = (Account) clazz.getConstructor(JsonObject.class).newInstance(jsonObject);
            this.accounts.add(account);
        }
    }

    @Override
    public JsonElement save() {
        final JsonArray array = new JsonArray();
        for (Account account : this.accounts) {
            final JsonObject jsonObject = account.toJson();
            jsonObject.addProperty("accountType", account.getClass().getName());
            array.add(jsonObject);
        }
        return array;
    }

    public Account addAccount(final String username) {
        final Account account = new OfflineAccount(username);
        this.accounts.add(account);
        return account;
    }

    public Account addAccount(final Account account) {
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

    public void ensureRefreshed(final Account account) throws Throwable {
        synchronized (this) {
            if (account.refresh()) {
                ViaProxy.getSaveManager().save();
            }
        }
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(this.accounts);
    }

}
