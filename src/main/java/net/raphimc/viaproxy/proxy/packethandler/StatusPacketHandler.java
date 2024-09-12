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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.status.S2CStatusPongResponsePacket;
import net.raphimc.netminecraft.packet.impl.status.S2CStatusResponsePacket;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.util.List;

public class StatusPacketHandler extends PacketHandler {

    public StatusPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleP2S(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof S2CStatusPongResponsePacket) {
            listeners.add(ChannelFutureListener.CLOSE);
        } else if (packet instanceof S2CStatusResponsePacket statusResponsePacket && !ViaProxy.getConfig().getCustomMotd().isBlank()) {
            try {
                final JsonObject obj = JsonParser.parseString(statusResponsePacket.statusJson).getAsJsonObject();
                obj.addProperty("description", ViaProxy.getConfig().getCustomMotd());
                statusResponsePacket.statusJson = obj.toString();
            } catch (Throwable ignored) {
            }
        }

        return true;
    }

}
