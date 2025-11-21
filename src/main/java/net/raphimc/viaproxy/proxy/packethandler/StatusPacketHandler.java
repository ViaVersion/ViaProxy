/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2025 RK_01/RaphiMC and contributors
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
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

public class StatusPacketHandler extends PacketHandler {

    private static String FAVICON_BASE_64;

    public StatusPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleP2S(Packet packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof S2CStatusPongResponsePacket) {
            listeners.add(ChannelFutureListener.CLOSE);
        } else if (packet instanceof S2CStatusResponsePacket statusResponsePacket && (!ViaProxy.getConfig().getCustomMotd().isBlank() || !ViaProxy.getConfig().getCustomFaviconPath().isBlank())) {
            try {
                final JsonObject obj = JsonParser.parseString(statusResponsePacket.statusJson).getAsJsonObject();
                if (!ViaProxy.getConfig().getCustomMotd().isBlank()) {
                    obj.addProperty("description", ViaProxy.getConfig().getCustomMotd());
                }
                if (!ViaProxy.getConfig().getCustomFaviconPath().isBlank()) {
                    if (FAVICON_BASE_64 == null) {
                        try {
                            final byte[] faviconBytes = Files.readAllBytes(new File(ViaProxy.getCwd(), ViaProxy.getConfig().getCustomFaviconPath()).toPath());
                            FAVICON_BASE_64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(faviconBytes);
                        } catch (Throwable e) {
                            Logger.LOGGER.error("Failed to load custom favicon from path: " + ViaProxy.getConfig().getCustomFaviconPath(), e);
                            FAVICON_BASE_64 = "";
                        }
                    }
                    if (!FAVICON_BASE_64.isBlank()) {
                        obj.addProperty("favicon", FAVICON_BASE_64);
                    }
                }
                statusResponsePacket.statusJson = obj.toString();
            } catch (Throwable ignored) {
            }
        }

        return true;
    }

}
