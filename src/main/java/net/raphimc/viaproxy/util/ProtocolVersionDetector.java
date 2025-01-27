/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2025 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.util;

import com.viaversion.vialoader.util.ProtocolVersionList;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.lenni0451.mcping.MCPing;
import net.lenni0451.mcping.pings.sockets.impl.factories.SocketChannelSocketFactory;
import net.lenni0451.mcping.responses.MCPingResponse;

import java.net.SocketAddress;

public class ProtocolVersionDetector {

    private static final int TIMEOUT = 3000;

    public static ProtocolVersion get(final SocketAddress serverAddress, final ProtocolVersion clientVersion) {
        final MCPingResponse response = MCPing
                .pingModern(clientVersion.getOriginalVersion())
                .tcpSocketFactory(new SocketChannelSocketFactory())
                .address(AddressUtil.toJ16UnixSocketAddress(serverAddress))
                .noResolve()
                .timeout(TIMEOUT, TIMEOUT)
                .getSync();

        if (response.version.protocol == clientVersion.getOriginalVersion()) { // If the server is on the same version as the client, we can just connect
            return clientVersion;
        }

        if (ProtocolVersion.isRegistered(response.version.protocol)) { // If the protocol is registered, we can use it
            return ProtocolVersion.getProtocol(response.version.protocol);
        } else {
            for (ProtocolVersion protocolVersion : ProtocolVersionList.getProtocolsNewToOld()) {
                for (String version : protocolVersion.getIncludedVersions()) {
                    if (response.version.name.contains(version)) {
                        return protocolVersion;
                    }
                }
            }
            throw new RuntimeException("Unable to detect the server version\nServer sent an invalid protocol id: " + response.version.protocol + " (" + response.version.name + "§r)");
        }
    }

}
