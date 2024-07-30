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
package net.raphimc.viaproxy.util.config;

import net.lenni0451.optconfig.serializer.ConfigTypeSerializer;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig;
import net.raphimc.viaproxy.saves.impl.accounts.Account;

import java.util.List;

public class AccountTypeSerializer extends ConfigTypeSerializer<ViaProxyConfig, Account> {

    public AccountTypeSerializer(final ViaProxyConfig config) {
        super(config);
    }

    @Override
    public Account deserialize(final Class<Account> typeClass, final Object serializedObject) {
        final List<Account> accounts = ViaProxy.getSaveManager().accountsSave.getAccounts();
        final int accountIndex = (int) serializedObject;
        if (this.config.getAuthMethod() == ViaProxyConfig.AuthMethod.ACCOUNT && accountIndex >= 0 && accountIndex < accounts.size()) {
            return accounts.get(accountIndex);
        } else {
            return null;
        }
    }

    @Override
    public Object serialize(final Account object) {
        if (object != null) {
            return ViaProxy.getSaveManager().accountsSave.getAccounts().indexOf(object);
        } else {
            return 0;
        }
    }

}
