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
import net.raphimc.netminecraft.packet.IPacket;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.UnknownPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginCustomPayloadPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginKeyPacket1_7;
import net.raphimc.viaproxy.proxy.external_interface.OpenAuthModConstants;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class OpenAuthModPacketHandler extends PacketHandler {

    private final int customPayloadId;

    public OpenAuthModPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);

        this.customPayloadId = MCPackets.C2S_PLUGIN_MESSAGE.getId(proxyConnection.getClientVersion().getVersion());
    }

    @Override
    public boolean handleC2P(IPacket packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof UnknownPacket unknownPacket && this.proxyConnection.getC2pConnectionState() == ConnectionState.PLAY) {
            if (unknownPacket.packetId == this.customPayloadId) {
                final ByteBuf data = Unpooled.wrappedBuffer(unknownPacket.data);
                final String channel = PacketTypes.readString(data, Short.MAX_VALUE); // channel
                if (channel.equals(OpenAuthModConstants.DATA_CHANNEL) && this.proxyConnection.handleCustomPayload(PacketTypes.readVarInt(data), data)) {
                    return false;
                }
            }
        } else if (packet instanceof C2SLoginCustomPayloadPacket loginCustomPayload) {
            if (loginCustomPayload.response != null && this.proxyConnection.handleCustomPayload(loginCustomPayload.queryId, Unpooled.wrappedBuffer(loginCustomPayload.response))) {
                return false;
            }
        } else if (packet instanceof C2SLoginKeyPacket1_7 loginKeyPacket) {
            if (this.proxyConnection.getClientVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2) && new String(loginKeyPacket.encryptedNonce, StandardCharsets.UTF_8).equals(OpenAuthModConstants.DATA_CHANNEL)) { // 1.8-1.12.2 OpenAuthMod response handling
                final ByteBuf byteBuf = Unpooled.wrappedBuffer(loginKeyPacket.encryptedSecretKey);
                this.proxyConnection.handleCustomPayload(PacketTypes.readVarInt(byteBuf), byteBuf);
                return false;
            }
        }

        return true;
    }

}
