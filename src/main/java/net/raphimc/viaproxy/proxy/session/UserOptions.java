/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.proxy.session;

import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.saves.impl.accounts.ClassicAccount;

public record UserOptions(Account account) {

    @Deprecated(forRemoval = true)
    public UserOptions(final String classicMpPass, final Account account) {
        this(migrateClassicMpPass(classicMpPass, account));
    }

    private static Account migrateClassicMpPass(final String classicMpPass, final Account account) {
        if (classicMpPass != null && account != null) {
            return new ClassicAccount(account.getName(), classicMpPass);
        }
        return account;
    }

}
