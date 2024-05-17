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

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.gson.JsonElement;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.packet.IPacket;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.UnknownPacket;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ResourcePackPacketHandler extends PacketHandler {

    private final int joinGameId;

    public ResourcePackPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);

        this.joinGameId = MCPackets.S2C_LOGIN.getId(this.proxyConnection.getClientVersion().getVersion());
    }

    @Override
    public boolean handleP2S(IPacket packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof UnknownPacket unknownPacket && this.proxyConnection.getP2sConnectionState() == ConnectionState.PLAY) {
            if (unknownPacket.packetId == this.joinGameId) {
                listeners.add(f -> {
                    if (f.isSuccess()) {
                        this.sendResourcePack();
                    }
                });
            }
        }

        return true;
    }

    private void sendResourcePack() {
        if (!ViaProxy.getConfig().getResourcePackUrl().isBlank()) {
            this.proxyConnection.getChannel().eventLoop().schedule(() -> {
                if (this.proxyConnection.getClientVersion().newerThanOrEqualTo(ProtocolVersion.v1_8)) {
                    final ByteBuf resourcePackPacket = Unpooled.buffer();
                    PacketTypes.writeVarInt(resourcePackPacket, MCPackets.S2C_RESOURCE_PACK.getId(this.proxyConnection.getClientVersion().getVersion()));
                    PacketTypes.writeString(resourcePackPacket, ViaProxy.getConfig().getResourcePackUrl()); // url
                    PacketTypes.writeString(resourcePackPacket, ""); // hash
                    if (this.proxyConnection.getClientVersion().newerThanOrEqualTo(ProtocolVersion.v1_17)) {
                        resourcePackPacket.writeBoolean(Via.getConfig().isForcedUse1_17ResourcePack()); // required
                        final JsonElement promptMessage = Via.getConfig().get1_17ResourcePackPrompt();
                        if (promptMessage != null) {
                            resourcePackPacket.writeBoolean(true); // has message
                            PacketTypes.writeString(resourcePackPacket, promptMessage.toString()); // message
                        } else {
                            resourcePackPacket.writeBoolean(false); // has message
                        }
                    }
                    this.proxyConnection.getC2P().writeAndFlush(resourcePackPacket).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                } else if (this.proxyConnection.getClientVersion().newerThanOrEqualTo(ProtocolVersion.v1_7_2)) {
                    final byte[] data = ViaProxy.getConfig().getResourcePackUrl().getBytes(StandardCharsets.UTF_8);

                    final ByteBuf customPayloadPacket = Unpooled.buffer();
                    PacketTypes.writeVarInt(customPayloadPacket, MCPackets.S2C_CUSTOM_PAYLOAD.getId(this.proxyConnection.getClientVersion().getVersion()));
                    PacketTypes.writeString(customPayloadPacket, "MC|RPack"); // channel
                    customPayloadPacket.writeShort(data.length); // length
                    customPayloadPacket.writeBytes(data); // data
                    this.proxyConnection.getC2P().writeAndFlush(customPayloadPacket).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                }
            }, 250, TimeUnit.MILLISECONDS);
        }
    }

}
