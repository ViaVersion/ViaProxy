/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
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

import com.google.common.collect.Lists;
import com.viaversion.viaversion.util.Key;
import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.common.C2SCustomPayloadPacket;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.nio.charset.StandardCharsets;
import java.util.List;

// See https://github.com/ViaVersion/ViaFabric/issues/428
public class FabricParticleApiFixPacketHandler extends PacketHandler {

    private static final String REGISTER_CHANNEL = "minecraft:register";

    public FabricParticleApiFixPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);
    }

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) throws Exception {
        if (packet instanceof C2SCustomPayloadPacket customPayloadPacket) {
            if (Key.namespaced(customPayloadPacket.channel).equals(REGISTER_CHANNEL)) {
                final List<String> channelsToRegister = Lists.newArrayList(new String(customPayloadPacket.data, StandardCharsets.UTF_8).split("\0"));
                if (channelsToRegister.remove("fabric:extended_block_state_particle_effect_sync")) {
                    if (channelsToRegister.isEmpty()) {
                        return false; // Cancel packet
                    } else {
                        customPayloadPacket.data = String.join("\0", channelsToRegister).getBytes(StandardCharsets.UTF_8);
                    }
                }
            }
        }

        return super.handleC2P(packet, listeners);
    }

}
