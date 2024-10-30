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
package net.raphimc.viaproxy.util;

import com.google.common.net.HostAndPort;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.unix.DomainSocketAddress;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.netminecraft.util.MinecraftServerAddress;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class AddressUtil {

    public static SocketAddress parse(final String serverAddress, final ProtocolVersion version) {
        if (serverAddress.startsWith("file:///") || serverAddress.startsWith("unix:///")) { // Unix Socket
            final String filePath = serverAddress.substring(7);

            return new DomainSocketAddress(filePath);
        } else { // IP Address
            final HostAndPort hostAndPort = HostAndPort.fromString(serverAddress);
            if (hostAndPort.getHost().isBlank()) {
                throw new IllegalArgumentException("Server address cannot be blank");
            }

            final int port;
            if (version != null) {
                port = hostAndPort.getPortOrDefault(getDefaultPort(version));
            } else {
                port = hostAndPort.getPort();
            }

            if (version == null || version.olderThan(LegacyProtocolVersion.r1_3_1tor1_3_2) || version.equals(BedrockProtocolVersion.bedrockLatest)) {
                return new InetSocketAddress(hostAndPort.getHost(), port);
            } else {
                return MinecraftServerAddress.ofResolved(hostAndPort.getHost(), port);
            }
        }
    }

    public static String toString(final SocketAddress address) {
        if (address instanceof InetSocketAddress inetSocketAddress) {
            return inetSocketAddress.getHostString() + ":" + inetSocketAddress.getPort();
        } else if (address instanceof DomainSocketAddress domainSocketAddress) {
            return domainSocketAddress.path();
        } else {
            return address.toString();
        }
    }

    public static int getDefaultPort(final ProtocolVersion version) {
        if (version.equals(BedrockProtocolVersion.bedrockLatest)) {
            return 19132;
        }

        return 25565;
    }

    /**
     * Converts the netty domain socket address to a Java 16 unix domain socket address if possible
     *
     * @param address The netty domain socket address
     * @return The Java 16 unix domain socket address or the original address if it is not a domain socket address
     */
    public static SocketAddress toJ16UnixSocketAddress(final SocketAddress address) {
        try {
            if (address instanceof DomainSocketAddress domainSocketAddress) {
                return RStream.of("java.net.UnixDomainSocketAddress").methods().by("of", String.class).invokeArgs(domainSocketAddress.path());
            }
        } catch (Throwable ignored) {
        }

        return address;
    }

}
