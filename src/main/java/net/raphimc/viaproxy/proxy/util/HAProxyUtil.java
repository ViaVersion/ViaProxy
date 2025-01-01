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
package net.raphimc.viaproxy.proxy.util;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.codec.haproxy.*;

import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class HAProxyUtil {

    public static HAProxyMessage createMessage(final Channel sourceChannel, final Channel targetChannel, final ProtocolVersion clientVersion) {
        final List<HAProxyTLV> tlvs = new ArrayList<>();
        if (clientVersion != null) {
            tlvs.add(new HAProxyTLV((byte) 0xE0, Unpooled.buffer().writeInt(clientVersion.getOriginalVersion())));
        }

        if (sourceChannel.remoteAddress() instanceof InetSocketAddress sourceAddress && targetChannel.remoteAddress() instanceof InetSocketAddress targetAddress) {
            final HAProxyProxiedProtocol protocol = sourceAddress.getAddress() instanceof Inet4Address ? HAProxyProxiedProtocol.TCP4 : HAProxyProxiedProtocol.TCP6;
            final String sourceAddressString = sourceAddress.getAddress().getHostAddress();
            final String targetAddressString;
            if (protocol.addressFamily().equals(HAProxyProxiedProtocol.AddressFamily.AF_IPv4)) {
                targetAddressString = getInetAddress(targetAddress.getHostString(), Inet4Address.class).getHostAddress();
            } else {
                targetAddressString = getInetAddress(targetAddress.getHostString(), Inet6Address.class).getHostAddress();
            }

            return new HAProxyMessage(HAProxyProtocolVersion.V2, HAProxyCommand.PROXY, protocol, sourceAddressString, targetAddressString, sourceAddress.getPort(), targetAddress.getPort(), tlvs);
        } else if (targetChannel.remoteAddress() instanceof DomainSocketAddress targetAddress) {
            return new HAProxyMessage(HAProxyProtocolVersion.V2, HAProxyCommand.PROXY, HAProxyProxiedProtocol.UNIX_STREAM, "", targetAddress.path(), 0, 0, tlvs);
        } else {
            throw new IllegalArgumentException("Unsupported address type: " + targetChannel.remoteAddress().getClass().getName());
        }
    }

    private static <T extends InetAddress> T getInetAddress(final String host, final Class<T> addressClass) {
        try {
            final InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (addressClass.isInstance(addr)) {
                    return (T) addr;
                }
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
