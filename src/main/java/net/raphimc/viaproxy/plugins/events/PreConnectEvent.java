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
package net.raphimc.viaproxy.plugins.events;

import io.netty.channel.Channel;
import net.raphimc.netminecraft.util.ServerAddress;
import net.raphimc.viaprotocolhack.util.VersionEnum;
import net.raphimc.viaproxy.plugins.events.types.ICancellable;

public class PreConnectEvent implements ICancellable {

    private final ServerAddress serverAddress;
    private final VersionEnum serverVersion;
    private final VersionEnum clientVersion;
    private final Channel clientChannel;

    private boolean cancelled;
    private String cancelMessage = "§cCould not connect to the backend server! (Server is blacklisted)";

    public PreConnectEvent(final ServerAddress serverAddress, final VersionEnum serverVersion, final VersionEnum clientVersion, final Channel clientChannel) {
        this.serverAddress = serverAddress;
        this.serverVersion = serverVersion;
        this.clientVersion = clientVersion;
        this.clientChannel = clientChannel;
    }

    public ServerAddress getServerAddress() {
        return this.serverAddress;
    }

    public VersionEnum getServerVersion() {
        return this.serverVersion;
    }

    public VersionEnum getClientVersion() {
        return this.clientVersion;
    }

    public Channel getClientChannel() {
        return this.clientChannel;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    public String getCancelMessage() {
        return this.cancelMessage;
    }

    public void setCancelMessage(final String cancelMessage) {
        this.cancelMessage = cancelMessage;
    }

}
