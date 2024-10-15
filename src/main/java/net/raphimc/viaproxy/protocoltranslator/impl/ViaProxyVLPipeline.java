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
package net.raphimc.viaproxy.protocoltranslator.impl;

import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.channel.ChannelHandler;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.vialoader.netty.VLPipeline;

public class ViaProxyVLPipeline extends VLPipeline {

    public ViaProxyVLPipeline(final UserConnection user) {
        super(user);
    }

    @Override
    public ChannelHandler createViaCodec() {
        return new ViaProxyViaCodec(this.user);
    }

    @Override
    protected String compressionCodecName() {
        return MCPipeline.COMPRESSION_HANDLER_NAME;
    }

    @Override
    protected String packetCodecName() {
        return MCPipeline.PACKET_CODEC_HANDLER_NAME;
    }

    @Override
    protected String lengthCodecName() {
        return MCPipeline.SIZER_HANDLER_NAME;
    }

}
