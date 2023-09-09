/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
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
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.packet.IPacket;
import net.raphimc.netminecraft.packet.UnknownPacket;
import net.raphimc.netminecraft.packet.impl.configuration.C2SConfigFinishConfiguration1_20_2;
import net.raphimc.netminecraft.packet.impl.configuration.S2CConfigFinishConfiguration1_20_2;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginStartConfiguration1_20_2;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.util.logging.Logger;

import java.util.List;

public class ConfigurationPacketHandler extends PacketHandler {

    private final int configurationAcknowledgedId;
    private final int startConfigurationId;

    public ConfigurationPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);

        this.configurationAcknowledgedId = MCPackets.C2S_CONFIGURATION_ACKNOWLEDGED.getId(proxyConnection.getClientVersion().getVersion());
        this.startConfigurationId = MCPackets.S2C_START_CONFIGURATION.getId(proxyConnection.getClientVersion().getVersion());
    }

    @Override
    public boolean handleC2P(IPacket packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof UnknownPacket unknownPacket && this.proxyConnection.getC2pConnectionState() == ConnectionState.PLAY) {
            if (unknownPacket.packetId == this.configurationAcknowledgedId) {
                this.proxyConnection.setC2pConnectionState(ConnectionState.CONFIGURATION);
                listeners.add(f -> {
                    if (f.isSuccess()) {
                        Logger.u_info("session", this.proxyConnection.getC2P().remoteAddress(), this.proxyConnection.getGameProfile(), "Switching to CONFIGURATION state");
                        this.proxyConnection.setP2sConnectionState(ConnectionState.CONFIGURATION);
                        this.proxyConnection.getChannel().config().setAutoRead(true);
                    }
                });
            }
        } else if (packet instanceof C2SLoginStartConfiguration1_20_2) {
            this.proxyConnection.setC2pConnectionState(ConnectionState.CONFIGURATION);
            listeners.add(f -> {
                if (f.isSuccess()) {
                    this.proxyConnection.setP2sConnectionState(ConnectionState.CONFIGURATION);
                    this.proxyConnection.getChannel().config().setAutoRead(true);
                }
            });
        } else if (packet instanceof C2SConfigFinishConfiguration1_20_2) {
            this.proxyConnection.setC2pConnectionState(ConnectionState.PLAY);
            listeners.add(f -> {
                if (f.isSuccess()) {
                    Logger.u_info("session", this.proxyConnection.getC2P().remoteAddress(), this.proxyConnection.getGameProfile(), "Configuration finished! Switching to PLAY state");
                    this.proxyConnection.setP2sConnectionState(ConnectionState.PLAY);
                    this.proxyConnection.getChannel().config().setAutoRead(true);
                }
            });
        }

        return true;
    }

    @Override
    public boolean handleP2S(IPacket packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof UnknownPacket unknownPacket && this.proxyConnection.getP2sConnectionState() == ConnectionState.PLAY) {
            if (unknownPacket.packetId == this.startConfigurationId) {
                this.proxyConnection.getChannel().config().setAutoRead(false);
            }
        } else if (packet instanceof S2CConfigFinishConfiguration1_20_2) {
            this.proxyConnection.getChannel().config().setAutoRead(false);
        }

        return true;
    }

}
