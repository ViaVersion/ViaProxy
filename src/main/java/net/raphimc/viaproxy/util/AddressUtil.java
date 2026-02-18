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
package net.raphimc.viaproxy.util;

import com.google.common.net.HostAndPort;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import dev.kastle.netty.channel.nethernet.config.NetherNetAddress;
import io.netty.channel.unix.DomainSocketAddress;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.netminecraft.util.MinecraftServerAddress;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.regex.Pattern;

public class AddressUtil {

    private static final Pattern NETHERNET_NETWORK_ID_PATTERN = Pattern.compile("^(?>[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}|\\d+)$", Pattern.CASE_INSENSITIVE);

    public static SocketAddress parse(final String serverAddress, final ProtocolVersion version) {
        if (serverAddress.startsWith("file://") || serverAddress.startsWith("unix://")) { // Unix Socket
            return new DomainSocketAddress(serverAddress.substring(serverAddress.indexOf("://") + 3));
        } else if (serverAddress.startsWith("nethernet-rpc://")) { // NetherNet JSON-RPC Address
            final String addressPart = serverAddress.substring(serverAddress.indexOf("://") + 3);
            if (NETHERNET_NETWORK_ID_PATTERN.matcher(addressPart).matches()) {
                return new NetherNetJsonRpcAddress(addressPart);
            } else {
                throw new IllegalArgumentException("Invalid NetherNet JSON RPC address");
            }
        } else if (serverAddress.startsWith("nethernet://")) { // NetherNet Address
            final String addressPart = serverAddress.substring(serverAddress.indexOf("://") + 3);
            if (NETHERNET_NETWORK_ID_PATTERN.matcher(addressPart).matches()) {
                return new NetherNetAddress(addressPart);
            } else {
                final HostAndPort hostAndPort = HostAndPort.fromString(addressPart);
                if (hostAndPort.getHost().isBlank()) {
                    throw new IllegalArgumentException("Server address cannot be blank");
                }
                return new NetherNetInetSocketAddress(hostAndPort.getHost(), hostAndPort.getPortOrDefault(ProtocolConstants.BEDROCK_NETHERNET_DEFAULT_PORT));
            }
        } else { // IP Address
            final HostAndPort hostAndPort = HostAndPort.fromString(serverAddress);
            if (hostAndPort.getHost().isBlank()) {
                throw new IllegalArgumentException("Server address cannot be blank");
            }

            final int port;
            if (version != null) {
                port = hostAndPort.getPortOrDefault(version.equals(BedrockProtocolVersion.bedrockLatest) ? ProtocolConstants.BEDROCK_RAKNET_DEFAULT_PORT : 25565);
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
        if (address instanceof DomainSocketAddress domainSocketAddress) {
            return "unix://" + domainSocketAddress.path();
        } else if (address instanceof NetherNetJsonRpcAddress netherNetAddress) {
            return "nethernet-rpc://" + netherNetAddress;
        } else if (address instanceof NetherNetAddress netherNetAddress) {
            return "nethernet://" + netherNetAddress;
        } else if (address instanceof NetherNetInetSocketAddress netherNetAddress) {
            return "nethernet://" + netherNetAddress.getHostString() + ":" + netherNetAddress.getPort();
        } else if (address instanceof InetSocketAddress inetSocketAddress) {
            return inetSocketAddress.getHostString() + ":" + inetSocketAddress.getPort();
        } else {
            return address.toString();
        }
    }

    @Deprecated(forRemoval = true)
    public static int getDefaultPort(final ProtocolVersion version) {
        if (version.equals(BedrockProtocolVersion.bedrockLatest)) {
            return ProtocolConstants.BEDROCK_RAKNET_DEFAULT_PORT;
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
