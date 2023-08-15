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

import net.raphimc.vialoader.util.VersionEnum;
import net.raphimc.viaproxy.plugins.events.types.EventCancellable;

public class ResolveSrvEvent extends EventCancellable {

    private final VersionEnum serverVersion;
    private String host;
    private int port;

    public ResolveSrvEvent(final VersionEnum serverVersion, final String host, final int port) {
        this.serverVersion = serverVersion;
        this.host = host;
        this.port = port;
    }

    public VersionEnum getServerVersion() {
        return this.serverVersion;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

}
