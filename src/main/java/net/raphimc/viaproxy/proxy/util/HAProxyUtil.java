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
package net.raphimc.viaproxy.proxy.util;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.haproxy.*;
import net.raphimc.vialoader.util.VersionEnum;

import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class HAProxyUtil {

    public static HAProxyMessage createMessage(final Channel sourceChannel, final Channel targetChannel, final VersionEnum clientVersion) {
        final InetSocketAddress sourceAddress = (InetSocketAddress) sourceChannel.remoteAddress();
        final InetSocketAddress targetAddress = (InetSocketAddress) targetChannel.remoteAddress();
        final HAProxyProxiedProtocol protocol = sourceAddress.getAddress() instanceof Inet4Address ? HAProxyProxiedProtocol.TCP4 : HAProxyProxiedProtocol.TCP6;
        final List<HAProxyTLV> tlvs = new ArrayList<>();
        if (clientVersion != null) {
            tlvs.add(new HAProxyTLV((byte) 0xE0, Unpooled.buffer().writeInt(clientVersion.getOriginalVersion())));
        }

        final String sourceAddressString = sourceAddress.getAddress().getHostAddress();
        final String targetAddressString = protocol.addressFamily().equals(HAProxyProxiedProtocol.AddressFamily.AF_IPv6) ? getIPv6Address(targetAddress.getHostString()).getHostAddress() : getIPv4Address(targetAddress.getHostString()).getHostAddress();

        return new HAProxyMessage(HAProxyProtocolVersion.V2, HAProxyCommand.PROXY, protocol, sourceAddressString, targetAddressString, sourceAddress.getPort(), targetAddress.getPort(), tlvs);
    }

    private static Inet6Address getIPv6Address(final String host) {
        try {
            final InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (addr instanceof Inet6Address) {
                    return (Inet6Address) addr;
                }
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private static Inet4Address getIPv4Address(final String host) {
        try {
            final InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (addr instanceof Inet4Address) {
                    return (Inet4Address) addr;
                }
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
