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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.packet.IPacket;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.UnknownPacket;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.util.List;

public abstract class CustomPayloadPacketHandler extends PacketHandler {

    private final int s2cCustomPayloadId;
    private final int s2cConfigCustomPayloadId;
    private final int c2sCustomPayloadId;
    private final int c2sConfigCustomPayloadId;

    public CustomPayloadPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);

        this.s2cCustomPayloadId = MCPackets.S2C_CUSTOM_PAYLOAD.getId(proxyConnection.getClientVersion().getVersion());
        this.s2cConfigCustomPayloadId = MCPackets.S2C_CONFIG_CUSTOM_PAYLOAD.getId(proxyConnection.getClientVersion().getVersion());
        this.c2sCustomPayloadId = MCPackets.C2S_CUSTOM_PAYLOAD.getId(proxyConnection.getClientVersion().getVersion());
        this.c2sConfigCustomPayloadId = MCPackets.C2S_CONFIG_CUSTOM_PAYLOAD.getId(proxyConnection.getClientVersion().getVersion());
    }

    @Override
    public boolean handleP2S(IPacket packet, List<ChannelFutureListener> listeners) throws Exception {
        if (packet instanceof UnknownPacket unknownPacket
                && (unknownPacket.packetId == this.s2cCustomPayloadId && this.proxyConnection.getP2sConnectionState() == ConnectionState.PLAY
                || unknownPacket.packetId == this.s2cConfigCustomPayloadId && this.proxyConnection.getP2sConnectionState() == ConnectionState.CONFIGURATION)) {
            final ByteBuf data = Unpooled.wrappedBuffer(unknownPacket.data);
            final String channel = PacketTypes.readString(data, Short.MAX_VALUE); // channel
            final ByteBuf newData = this.handleP2S(unknownPacket, channel, data, listeners);
            if (newData == data) {
                return true;
            } else if (newData != null) {
                final ByteBuf newCustomPayloadData = Unpooled.buffer();
                PacketTypes.writeString(newCustomPayloadData, channel); // channel
                newCustomPayloadData.writeBytes(newData);
                unknownPacket.data = ByteBufUtil.getBytes(newCustomPayloadData);
                return true;
            } else {
                return false;
            }
        }

        return super.handleP2S(packet, listeners);
    }

    @Override
    public boolean handleC2P(IPacket packet, List<ChannelFutureListener> listeners) throws Exception {
        if (packet instanceof UnknownPacket unknownPacket
                && (unknownPacket.packetId == this.c2sCustomPayloadId && this.proxyConnection.getC2pConnectionState() == ConnectionState.PLAY
                || unknownPacket.packetId == this.c2sConfigCustomPayloadId && this.proxyConnection.getC2pConnectionState() == ConnectionState.CONFIGURATION)) {
            final ByteBuf data = Unpooled.wrappedBuffer(unknownPacket.data);
            final String channel = PacketTypes.readString(data, Short.MAX_VALUE); // channel
            final ByteBuf newData = this.handleC2P(unknownPacket, channel, data, listeners);
            if (newData == data) {
                return true;
            } else if (newData != null) {
                final ByteBuf newCustomPayloadData = Unpooled.buffer();
                PacketTypes.writeString(newCustomPayloadData, channel); // channel
                newCustomPayloadData.writeBytes(newData);
                unknownPacket.data = ByteBufUtil.getBytes(newCustomPayloadData);
                return true;
            } else {
                return false;
            }
        }

        return super.handleC2P(packet, listeners);
    }

    public ByteBuf handleC2P(final UnknownPacket packet, final String channel, final ByteBuf data, final List<ChannelFutureListener> listeners) throws Exception {
        return data;
    }

    public ByteBuf handleP2S(final UnknownPacket packet, final String channel, final ByteBuf data, final List<ChannelFutureListener> listeners) throws Exception {
        return data;
    }

}
