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
package net.raphimc.viaproxy.protocolhack.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import net.raphimc.vialegacy.ViaLegacy;
import net.raphimc.vialegacy.protocols.alpha.protocola1_0_17_1_0_17_4toa1_0_16_2.storage.TimeLockStorage;
import net.raphimc.vialegacy.protocols.classic.protocola1_0_15toc0_28_30.providers.ClassicCustomCommandProvider;

import java.util.logging.Level;

public class ViaProxyClassicCustomCommandProvider extends ClassicCustomCommandProvider {

    @Override
    public boolean handleChatMessage(UserConnection user, String message) {
        try {
            if (message.startsWith("/")) {
                message = message.substring(1);
                final String[] args = message.split(" ");
                if (args.length == 0) return super.handleChatMessage(user, message);
                if (args[0].equals("settime")) {
                    try {
                        if (args.length > 1) {
                            final long time = Long.parseLong(args[1]) % 24_000L;
                            user.get(TimeLockStorage.class).setTime(time);
                            this.sendFeedback(user, "§aTime has been set to §6" + time);
                        } else {
                            throw new RuntimeException("Invalid usage");
                        }
                    } catch (Throwable ignored) {
                        this.sendFeedback(user, "§cUsage: /settime <Time (Long)>");
                    }
                    return true;
                }
            }
        } catch (Throwable e) {
            ViaLegacy.getPlatform().getLogger().log(Level.WARNING, "Error handling custom classic command", e);
        }

        return super.handleChatMessage(user, message);
    }

}
