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
package net.raphimc.viaproxy.protocolhack.impl;

import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.channel.ChannelHandlerContext;
import net.raphimc.vialoader.netty.ViaCodec;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.util.logging.Logger;

public class ViaProxyViaCodec extends ViaCodec {

    public ViaProxyViaCodec(UserConnection user) {
        super(user);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (Options.IGNORE_PACKET_TRANSLATION_ERRORS) {
            try {
                super.channelRead(ctx, msg);
            } catch (Throwable e) {
                Logger.LOGGER.error("ProtocolHack packet translation error occurred", e);
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

}
