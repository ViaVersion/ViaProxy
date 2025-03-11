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
package net.raphimc.viaproxy.protocoltranslator.viaproxy;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.command.ViaCommandSender;
import net.raphimc.viaproxy.cli.ConsoleFormatter;

import java.util.UUID;

public class ConsoleCommandSender implements ViaCommandSender {

    private static final UUID CONSOLE_UUID = new UUID(0, 0);

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    @Override
    public void sendMessage(String msg) {
        Via.getPlatform().getLogger().info(ConsoleFormatter.convert(msg));
    }

    @Override
    public UUID getUUID() {
        return CONSOLE_UUID;
    }

    @Override
    public String getName() {
        return "Console";
    }

}
