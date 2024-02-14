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
package net.raphimc.viaproxy.proxy.packethandler;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.packet.IPacket;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.UnknownPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginCompressionPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginSuccessPacket1_7;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.proxy.util.ChannelUtil;

import java.util.List;

public class CompressionPacketHandler extends PacketHandler {

    private final int setCompressionId;

    public CompressionPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);

        this.setCompressionId = MCPackets.S2C_SET_COMPRESSION.getId(proxyConnection.getClientVersion().getVersion());
    }

    @Override
    public boolean handleP2S(IPacket packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof UnknownPacket unknownPacket && this.proxyConnection.getC2pConnectionState() == ConnectionState.PLAY) {
            if (unknownPacket.packetId == this.setCompressionId) {
                final ByteBuf data = Unpooled.wrappedBuffer(unknownPacket.data);
                final int compressionThreshold = PacketTypes.readVarInt(data); // compression threshold
                this.proxyConnection.getChannel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(compressionThreshold);
                data.release();

                return false;
            }
        } else if (packet instanceof S2CLoginSuccessPacket1_7) {
            if (this.proxyConnection.getClientVersion().newerThanOrEqualTo(ProtocolVersion.v1_8)) {
                if (Options.COMPRESSION_THRESHOLD > -1 && this.proxyConnection.getC2P().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get() == -1) {
                    ChannelUtil.disableAutoRead(this.proxyConnection.getChannel());
                    this.proxyConnection.getC2P().writeAndFlush(new S2CLoginCompressionPacket(Options.COMPRESSION_THRESHOLD)).addListeners(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE, (ChannelFutureListener) f -> {
                        if (f.isSuccess()) {
                            this.proxyConnection.getC2P().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(Options.COMPRESSION_THRESHOLD);
                            ChannelUtil.restoreAutoRead(this.proxyConnection.getChannel());
                        }
                    });
                }
            }
        } else if (packet instanceof S2CLoginCompressionPacket loginCompressionPacket) {
            this.proxyConnection.getChannel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(loginCompressionPacket.compressionThreshold);
            return false;
        }

        return true;
    }

}
