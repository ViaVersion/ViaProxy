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
import net.raphimc.viabedrock.protocol.data.enums.java.ResourcePackAction;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ResourcePackSpooferPacketHandler extends PacketHandler {

    private final int s2cResourcePackId;
    private final int s2cConfigResourcePackId;
    private final int resourcePackPushId;
    private final int configResourcePackPushId;
    private final int resourcePackPopId;
    private final int configResourcePackPopId;
    private final int c2sResourcePackId;
    private final int c2sConfigResourcePackId;

    private final Set<UUID> resourcePacks = new HashSet<>();

    public ResourcePackSpooferPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);

        this.s2cResourcePackId = MCPackets.S2C_RESOURCE_PACK.getId(this.proxyConnection.getClientVersion().getVersion());
        this.s2cConfigResourcePackId = MCPackets.S2C_CONFIG_RESOURCE_PACK.getId(this.proxyConnection.getClientVersion().getVersion());
        this.resourcePackPushId = MCPackets.S2C_RESOURCE_PACK_PUSH.getId(this.proxyConnection.getClientVersion().getVersion());
        this.configResourcePackPushId = MCPackets.S2C_CONFIG_RESOURCE_PACK_PUSH.getId(this.proxyConnection.getClientVersion().getVersion());
        this.resourcePackPopId = MCPackets.S2C_RESOURCE_PACK_POP.getId(this.proxyConnection.getClientVersion().getVersion());
        this.configResourcePackPopId = MCPackets.S2C_CONFIG_RESOURCE_PACK_POP.getId(this.proxyConnection.getClientVersion().getVersion());
        this.c2sResourcePackId = MCPackets.C2S_RESOURCE_PACK.getId(this.proxyConnection.getClientVersion().getVersion());
        this.c2sConfigResourcePackId = MCPackets.C2S_CONFIG_RESOURCE_PACK.getId(this.proxyConnection.getClientVersion().getVersion());
    }

    @Override
    public boolean handleP2S(IPacket packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof UnknownPacket unknownPacket
                && (unknownPacket.packetId == this.resourcePackPushId && this.proxyConnection.getP2sConnectionState() == ConnectionState.PLAY
                || unknownPacket.packetId == this.configResourcePackPushId && this.proxyConnection.getP2sConnectionState() == ConnectionState.CONFIGURATION)) {
            final ByteBuf data = Unpooled.wrappedBuffer(unknownPacket.data);
            final UUID packId = PacketTypes.readUuid(data); // pack id
            this.resourcePacks.add(packId);
            this.sendResponse(packId, null, ResourcePackAction.ACCEPTED.ordinal());
            for (UUID resourcePack : this.resourcePacks) {
                this.sendResponse(resourcePack, null, ResourcePackAction.SUCCESSFULLY_LOADED.ordinal());
            }
            return false;
        } else if (packet instanceof UnknownPacket unknownPacket
                && (unknownPacket.packetId == this.resourcePackPopId && this.proxyConnection.getP2sConnectionState() == ConnectionState.PLAY
                || unknownPacket.packetId == this.configResourcePackPopId && this.proxyConnection.getP2sConnectionState() == ConnectionState.CONFIGURATION)) {
            final ByteBuf data = Unpooled.wrappedBuffer(unknownPacket.data);
            if (data.readBoolean()) { // has pack id
                final UUID packId = PacketTypes.readUuid(data); // pack id
                this.resourcePacks.remove(packId);
            } else {
                this.resourcePacks.clear();
            }
            for (UUID resourcePack : this.resourcePacks) {
                this.sendResponse(resourcePack, null, ResourcePackAction.SUCCESSFULLY_LOADED.ordinal());
            }
            return false;
        } else if (packet instanceof UnknownPacket unknownPacket
                && (unknownPacket.packetId == this.s2cResourcePackId && this.proxyConnection.getP2sConnectionState() == ConnectionState.PLAY
                || unknownPacket.packetId == this.s2cConfigResourcePackId && this.proxyConnection.getP2sConnectionState() == ConnectionState.CONFIGURATION)) {
            final ByteBuf data = Unpooled.wrappedBuffer(unknownPacket.data);
            PacketTypes.readString(data, Short.MAX_VALUE); // url
            final String hash = PacketTypes.readString(data, 40); // hash
            this.sendResponse(null, hash, ResourcePackAction.ACCEPTED.ordinal());
            this.sendResponse(null, hash, ResourcePackAction.SUCCESSFULLY_LOADED.ordinal());
            return false;
        }

        return true;
    }

    private void sendResponse(final UUID packId, final String hash, final int status) {
        final ByteBuf resourcePackResponse = Unpooled.buffer();
        PacketTypes.writeVarInt(resourcePackResponse, this.proxyConnection.getP2sConnectionState() == ConnectionState.PLAY ? this.c2sResourcePackId : this.c2sConfigResourcePackId);
        if (this.proxyConnection.getClientVersion().newerThanOrEqualTo(ProtocolVersion.v1_20_3)) {
            PacketTypes.writeUuid(resourcePackResponse, packId); // pack id
        } else if (this.proxyConnection.getClientVersion().olderThanOrEqualTo(ProtocolVersion.v1_9)) {
            PacketTypes.writeString(resourcePackResponse, hash); // hash
        }
        PacketTypes.writeVarInt(resourcePackResponse, status); // status
        this.proxyConnection.getChannel().writeAndFlush(resourcePackResponse).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

}
