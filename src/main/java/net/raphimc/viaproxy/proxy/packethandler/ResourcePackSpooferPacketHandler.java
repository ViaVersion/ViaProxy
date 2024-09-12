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

import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.common.S2CResourcePackPacket;
import net.raphimc.netminecraft.packet.impl.common.S2CResourcePackPopPacket;
import net.raphimc.netminecraft.packet.impl.common.S2CResourcePackPushPacket;
import net.raphimc.netminecraft.packet.impl.configuration.C2SConfigResourcePackPacket;
import net.raphimc.netminecraft.packet.impl.play.C2SPlayResourcePackPacket;
import net.raphimc.viabedrock.protocol.data.enums.java.ResourcePackAction;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ResourcePackSpooferPacketHandler extends PacketHandler {

    private final Set<UUID> resourcePacks = new HashSet<>();

    public ResourcePackSpooferPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleP2S(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof S2CResourcePackPushPacket resourcePackPushPacket) {
            this.resourcePacks.add(resourcePackPushPacket.packId);
            this.sendResponse(resourcePackPushPacket.packId, null, ResourcePackAction.ACCEPTED.ordinal());
            for (UUID resourcePack : this.resourcePacks) {
                this.sendResponse(resourcePack, null, ResourcePackAction.SUCCESSFULLY_LOADED.ordinal());
            }
            return false;
        } else if (packet instanceof S2CResourcePackPopPacket resourcePackPopPacket) {
            if (resourcePackPopPacket.packId != null) {
                this.resourcePacks.remove(resourcePackPopPacket.packId);
            } else {
                this.resourcePacks.clear();
            }
            for (UUID resourcePack : this.resourcePacks) {
                this.sendResponse(resourcePack, null, ResourcePackAction.SUCCESSFULLY_LOADED.ordinal());
            }
            return false;
        } else if (packet instanceof S2CResourcePackPacket resourcePackPacket) {
            this.sendResponse(null, resourcePackPacket.hash, ResourcePackAction.ACCEPTED.ordinal());
            this.sendResponse(null, resourcePackPacket.hash, ResourcePackAction.SUCCESSFULLY_LOADED.ordinal());
            return false;
        }

        return true;
    }

    private void sendResponse(final UUID packId, final String hash, final int status) {
        if (this.proxyConnection.getP2sConnectionState() == ConnectionState.PLAY) {
            this.proxyConnection.getChannel().writeAndFlush(new C2SPlayResourcePackPacket(status, packId, hash)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } else {
            this.proxyConnection.getChannel().writeAndFlush(new C2SConfigResourcePackPacket(status, packId, hash)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
    }

}
