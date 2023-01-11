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
package net.raphimc.viaproxy.protocolhack.impl;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.exception.CancelCodecException;
import com.viaversion.viaversion.util.PipelineUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.raphimc.viaprotocolhack.netty.ViaDecodeHandler;
import net.raphimc.viaproxy.util.logging.Logger;

import java.util.List;

public class ViaProxyViaDecodeHandler extends ViaDecodeHandler {

    public ViaProxyViaDecodeHandler(final UserConnection info) {
        super(info);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf bytebuf, List<Object> out) throws Exception {
        try {
            super.decode(ctx, bytebuf, out);
        } catch (Throwable e) {
            if (PipelineUtil.containsCause(e, CancelCodecException.class)) throw e;
            Logger.LOGGER.error("ProtocolHack Packet Error occurred", e);
            Logger.u_err("ProtocolHack Error", this.user, "Caught unhandled exception: " + e.getClass().getSimpleName());
        }
    }

}
