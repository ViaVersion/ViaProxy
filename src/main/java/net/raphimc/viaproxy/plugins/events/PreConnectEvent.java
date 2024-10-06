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
package net.raphimc.viaproxy.plugins.events;

import com.google.common.net.HostAndPort;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.Channel;
import net.raphimc.netminecraft.constants.IntendedState;
import net.raphimc.viaproxy.plugins.events.types.EventCancellable;

import java.net.SocketAddress;

public class PreConnectEvent extends EventCancellable {

    private SocketAddress serverAddress;
    private ProtocolVersion serverVersion;
    private final ProtocolVersion clientVersion;
    private final HostAndPort clientHandshakeAddress;
    private final IntendedState intendedState;
    private final Channel clientChannel;

    private String cancelMessage = "§cCould not connect to the backend server! (Server is blacklisted)";

    public PreConnectEvent(final SocketAddress serverAddress, final ProtocolVersion serverVersion, final ProtocolVersion clientVersion, final HostAndPort clientHandshakeAddress, final IntendedState intendedState, final Channel clientChannel) {
        this.serverAddress = serverAddress;
        this.serverVersion = serverVersion;
        this.clientVersion = clientVersion;
        this.clientHandshakeAddress = clientHandshakeAddress;
        this.intendedState = intendedState;
        this.clientChannel = clientChannel;
    }

    public SocketAddress getServerAddress() {
        return this.serverAddress;
    }

    public void setServerAddress(final SocketAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    public ProtocolVersion getServerVersion() {
        return this.serverVersion;
    }

    public void setServerVersion(final ProtocolVersion serverVersion) {
        this.serverVersion = serverVersion;
    }

    public ProtocolVersion getClientVersion() {
        return this.clientVersion;
    }

    public HostAndPort getClientHandshakeAddress() {
        return this.clientHandshakeAddress;
    }

    public IntendedState getIntendedState() {
        return this.intendedState;
    }

    public Channel getClientChannel() {
        return this.clientChannel;
    }

    public String getCancelMessage() {
        return this.cancelMessage;
    }

    public void setCancelMessage(final String cancelMessage) {
        this.setCancelled(true);
        this.cancelMessage = cancelMessage;
    }

}
