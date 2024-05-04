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

import com.viaversion.viaversion.util.Key;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.UnknownPacket;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.util.logging.Logger;

import java.net.InetSocketAddress;
import java.util.List;

public class SimpleVoiceChatPacketHandler extends CustomPayloadPacketHandler {

    private static final String SECRET_CHANNEL = "voicechat:secret";

    public SimpleVoiceChatPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public ByteBuf handleP2S(UnknownPacket packet, String channel, ByteBuf data, List<ChannelFutureListener> listeners) throws Exception {
        if (Key.namespaced(channel).equals(SECRET_CHANNEL)) {
            try {
                final ByteBuf newData = Unpooled.buffer();
                PacketTypes.writeUuid(newData, PacketTypes.readUuid(data)); // secret
                final int port = data.readInt(); // port
                newData.writeInt(port);
                PacketTypes.writeUuid(newData, PacketTypes.readUuid(data)); // player uuid
                newData.writeByte(data.readByte()); // codec
                newData.writeInt(data.readInt()); // mtu size
                newData.writeDouble(data.readDouble()); // voice chat distance
                newData.writeInt(data.readInt()); // keep alive
                newData.writeBoolean(data.readBoolean()); // groups enabled
                final String voiceHost = PacketTypes.readString(data, Short.MAX_VALUE); // voice host
                if (voiceHost.isEmpty()) {
                    if (this.proxyConnection.getServerAddress() instanceof InetSocketAddress serverAddress) {
                        PacketTypes.writeString(newData, new InetSocketAddress(serverAddress.getAddress(), port).toString());
                    } else {
                        throw new IllegalArgumentException("Server address must be an InetSocketAddress");
                    }
                } else {
                    PacketTypes.writeString(newData, voiceHost);
                }
                newData.writeBytes(data);
                return newData;
            } catch (Throwable e) {
                Logger.LOGGER.error("Failed to handle simple voice chat packet", e);
                return super.handleP2S(packet, channel, data, listeners);
            }
        }

        return super.handleP2S(packet, channel, data, listeners);
    }

}
