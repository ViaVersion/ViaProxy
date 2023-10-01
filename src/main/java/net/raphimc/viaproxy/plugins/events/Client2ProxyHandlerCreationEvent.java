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

import io.netty.channel.ChannelHandler;

public class Client2ProxyHandlerCreationEvent {

    /**
     * The handler which will be used to handle Client<->Proxy packets.
     */
    private ChannelHandler handler;

    /**
     * Whether the handler is a legacy passthrough handler.
     */
    private final boolean isLegacyPassthrough;

    public Client2ProxyHandlerCreationEvent(final ChannelHandler handler, final boolean isLegacyPassthrough) {
        this.handler = handler;
        this.isLegacyPassthrough = isLegacyPassthrough;
    }

    public ChannelHandler getHandler() {
        return this.handler;
    }

    public void setHandler(final ChannelHandler handler) {
        this.handler = handler;
    }

    public boolean isLegacyPassthrough() {
        return this.isLegacyPassthrough;
    }

}
