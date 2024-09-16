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
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.common.S2CDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginDisconnectPacket;
import net.raphimc.viaproxy.cli.ConsoleFormatter;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.util.logging.Logger;

import java.util.List;

public class DisconnectPacketHandler extends PacketHandler {

    public DisconnectPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleP2S(Packet packet, List<ChannelFutureListener> listeners) throws Exception {
        if (packet instanceof S2CLoginDisconnectPacket loginDisconnectPacket) {
            Logger.u_info("server disconnect", this.proxyConnection, ConsoleFormatter.convert(loginDisconnectPacket.reason.asLegacyFormatString()));
        } else if (packet instanceof S2CDisconnectPacket disconnectPacket) {
            Logger.u_info("server disconnect", this.proxyConnection, ConsoleFormatter.convert(disconnectPacket.reason.asLegacyFormatString()));
        }
        return super.handleP2S(packet, listeners);
    }

}
