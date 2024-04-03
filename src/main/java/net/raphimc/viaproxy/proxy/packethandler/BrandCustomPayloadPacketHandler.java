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
import com.viaversion.viaversion.util.Key;
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

public class BrandCustomPayloadPacketHandler extends PacketHandler {

    private static final String BRAND_CHANNEL = "minecraft:brand";
    private static final String LEGACY_BRAND_CHANNEL = "MC|Brand";

    private final int customPayloadId;
    private final int configCustomPayloadId;

    public BrandCustomPayloadPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);

        this.customPayloadId = MCPackets.S2C_PLUGIN_MESSAGE.getId(proxyConnection.getClientVersion().getVersion());
        this.configCustomPayloadId = MCPackets.S2C_CONFIG_CUSTOM_PAYLOAD.getId(proxyConnection.getClientVersion().getVersion());
    }

    @Override
    public boolean handleP2S(IPacket packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof UnknownPacket unknownPacket
                && (unknownPacket.packetId == this.customPayloadId && this.proxyConnection.getP2sConnectionState() == ConnectionState.PLAY
                || unknownPacket.packetId == this.configCustomPayloadId && this.proxyConnection.getP2sConnectionState() == ConnectionState.CONFIGURATION)) {
            final ByteBuf data = Unpooled.wrappedBuffer(unknownPacket.data);
            final String channel = PacketTypes.readString(data, Short.MAX_VALUE); // channel
            if (Key.namespaced(channel).equals(BRAND_CHANNEL) || channel.equals(LEGACY_BRAND_CHANNEL)) {

                String brand;
                try {
                    brand = PacketTypes.readString(data, Short.MAX_VALUE);
                } catch (Exception e) {
                    if (this.proxyConnection.getServerVersion().newerThan(ProtocolVersion.v1_20)) {
                        throw e;
                    } else { // <=1.20 clients ignore errors
                        brand = "Unknown";
                    }
                }
                final String newBrand = "ViaProxy (" + this.proxyConnection.getClientVersion().getName() + ") -> " + brand + " §r(" + this.proxyConnection.getServerVersion().getName() + ")";

                final ByteBuf newCustomPayloadData = Unpooled.buffer();
                PacketTypes.writeString(newCustomPayloadData, channel); // channel
                PacketTypes.writeString(newCustomPayloadData, newBrand); // data
                unknownPacket.data = ByteBufUtil.getBytes(newCustomPayloadData);
            }
        }

        return true;
    }

}
