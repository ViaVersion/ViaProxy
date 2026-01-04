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
package net.raphimc.viaproxy.proxy.session;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;
import net.raphimc.netminecraft.netty.connection.NetClient;
import net.raphimc.netminecraft.util.TransportType;
import net.raphimc.viaproxy.ViaProxy;

import java.net.SocketAddress;

public class LegacyProxyConnection extends NetClient {

    public static final AttributeKey<LegacyProxyConnection> LEGACY_PROXY_CONNECTION_ATTRIBUTE_KEY = AttributeKey.valueOf("legacy_proxy_connection");

    private final Channel c2p;
    private SocketAddress serverAddress;

    public LegacyProxyConnection(final ChannelInitializer<Channel> channelInitializer, final Channel c2p) {
        super(channelInitializer);
        this.c2p = c2p;
    }

    public static LegacyProxyConnection fromChannel(final Channel channel) {
        return channel.attr(LEGACY_PROXY_CONNECTION_ATTRIBUTE_KEY).get();
    }

    @Override
    public void initialize(final TransportType transportType, final Bootstrap bootstrap) {
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ViaProxy.getConfig().getConnectTimeout());
        bootstrap.attr(LEGACY_PROXY_CONNECTION_ATTRIBUTE_KEY, this);
        super.initialize(transportType, bootstrap);
    }

    @Override
    public ChannelFuture connect(final SocketAddress serverAddress) {
        this.serverAddress = serverAddress;
        return super.connect(serverAddress);
    }

    public Channel getC2P() {
        return this.c2p;
    }

    public SocketAddress getServerAddress() {
        return this.serverAddress;
    }

}
