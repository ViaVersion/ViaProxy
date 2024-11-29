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

import net.raphimc.viaproxy.plugins.events.types.EventCancellable;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

public class JoinServerRequestEvent extends EventCancellable  {

    private final ProxyConnection proxyConnection;
    private final String serverIdHash;

    public JoinServerRequestEvent(final ProxyConnection proxyConnection, final String serverIdHash) {
        this.proxyConnection = proxyConnection;
        this.serverIdHash = serverIdHash;
    }

    public ProxyConnection getProxyConnection() {
        return this.proxyConnection;
    }

    public String getServerIdHash() {
        return this.serverIdHash;
    }

}
