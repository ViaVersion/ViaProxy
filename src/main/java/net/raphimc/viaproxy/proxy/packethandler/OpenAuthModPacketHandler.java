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
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import net.lenni0451.mcstructs.text.components.StringComponent;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.packet.IPacket;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.UnknownPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginCustomPayloadPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginKeyPacket1_7;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginCustomPayloadPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginDisconnectPacket1_20_3;
import net.raphimc.viaproxy.proxy.external_interface.OpenAuthModConstants;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class OpenAuthModPacketHandler extends PacketHandler {

    private final int c2sCustomPayloadId;
    private final int s2cCustomPayloadId;
    private final AtomicInteger id = new AtomicInteger(0);
    private final Map<Integer, CompletableFuture<ByteBuf>> customPayloadListener = new ConcurrentHashMap<>();

    public OpenAuthModPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);

        this.c2sCustomPayloadId = MCPackets.C2S_CUSTOM_PAYLOAD.getId(proxyConnection.getClientVersion().getVersion());
        this.s2cCustomPayloadId = MCPackets.S2C_CUSTOM_PAYLOAD.getId(proxyConnection.getClientVersion().getVersion());
    }

    @Override
    public boolean handleC2P(IPacket packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof UnknownPacket unknownPacket && this.proxyConnection.getC2pConnectionState() == ConnectionState.PLAY) {
            if (unknownPacket.packetId == this.c2sCustomPayloadId) {
                final ByteBuf data = Unpooled.wrappedBuffer(unknownPacket.data);
                final String channel = PacketTypes.readString(data, Short.MAX_VALUE); // channel
                if (channel.equals(OpenAuthModConstants.DATA_CHANNEL) && this.handleCustomPayload(PacketTypes.readVarInt(data), data)) {
                    return false;
                }
            }
        } else if (packet instanceof C2SLoginCustomPayloadPacket loginCustomPayload) {
            if (loginCustomPayload.response != null && this.handleCustomPayload(loginCustomPayload.queryId, Unpooled.wrappedBuffer(loginCustomPayload.response))) {
                return false;
            }
        } else if (packet instanceof C2SLoginKeyPacket1_7 loginKeyPacket) {
            if (this.proxyConnection.getClientVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2) && new String(loginKeyPacket.encryptedNonce, StandardCharsets.UTF_8).equals(OpenAuthModConstants.DATA_CHANNEL)) { // 1.8-1.12.2 OpenAuthMod response handling
                final ByteBuf byteBuf = Unpooled.wrappedBuffer(loginKeyPacket.encryptedSecretKey);
                this.handleCustomPayload(PacketTypes.readVarInt(byteBuf), byteBuf);
                return false;
            }
        }

        return true;
    }

    public CompletableFuture<ByteBuf> sendCustomPayload(final String channel, final ByteBuf data) {
        if (channel.length() > 20) throw new IllegalStateException("Channel name can't be longer than 20 characters");
        final CompletableFuture<ByteBuf> future = new CompletableFuture<>();
        final int id = this.id.getAndIncrement();

        switch (this.proxyConnection.getC2pConnectionState()) {
            case LOGIN:
                if (this.proxyConnection.getClientVersion().newerThanOrEqualTo(ProtocolVersion.v1_13)) {
                    this.proxyConnection.getC2P().writeAndFlush(new S2CLoginCustomPayloadPacket(id, channel, PacketTypes.readReadableBytes(data))).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                } else {
                    final ByteBuf disconnectPacketData = Unpooled.buffer();
                    PacketTypes.writeString(disconnectPacketData, channel);
                    PacketTypes.writeVarInt(disconnectPacketData, id);
                    disconnectPacketData.writeBytes(data);
                    this.proxyConnection.getC2P().writeAndFlush(new S2CLoginDisconnectPacket1_20_3(new StringComponent("§cYou need to install OpenAuthMod in order to join this server.§k\n" + Base64.getEncoder().encodeToString(ByteBufUtil.getBytes(disconnectPacketData)) + "\n" + OpenAuthModConstants.LEGACY_MAGIC_STRING))).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                }
                break;
            case PLAY:
                final ByteBuf customPayloadPacket = Unpooled.buffer();
                PacketTypes.writeVarInt(customPayloadPacket, this.s2cCustomPayloadId);
                PacketTypes.writeString(customPayloadPacket, channel); // channel
                PacketTypes.writeVarInt(customPayloadPacket, id);
                customPayloadPacket.writeBytes(data);
                this.proxyConnection.getC2P().writeAndFlush(customPayloadPacket).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                break;
            default:
                throw new IllegalStateException("Can't send a custom payload packet during " + this.proxyConnection.getC2pConnectionState());
        }

        this.customPayloadListener.put(id, future);
        return future;
    }

    private boolean handleCustomPayload(final int id, final ByteBuf data) {
        if (this.customPayloadListener.containsKey(id)) {
            this.customPayloadListener.remove(id).complete(data);
            return true;
        }
        return false;
    }

}
