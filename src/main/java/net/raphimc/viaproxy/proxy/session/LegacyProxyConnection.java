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
package net.raphimc.viaproxy.proxy.session;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;
import net.raphimc.netminecraft.netty.connection.NetClient;
import net.raphimc.netminecraft.util.ServerAddress;

import java.util.function.Function;
import java.util.function.Supplier;

public class LegacyProxyConnection extends NetClient {

    public static final AttributeKey<LegacyProxyConnection> LEGACY_PROXY_CONNECTION_ATTRIBUTE_KEY = AttributeKey.valueOf("legacy_proxy_connection");

    private final Channel c2p;
    private ServerAddress serverAddress;

    public LegacyProxyConnection(final Supplier<ChannelHandler> handlerSupplier, final Function<Supplier<ChannelHandler>, ChannelInitializer<Channel>> channelInitializerSupplier, final Channel c2p) {
        super(handlerSupplier, channelInitializerSupplier);
        this.c2p = c2p;
    }

    public static LegacyProxyConnection fromChannel(final Channel channel) {
        return channel.attr(LEGACY_PROXY_CONNECTION_ATTRIBUTE_KEY).get();
    }

    @Override
    public void connect(final ServerAddress serverAddress) {
        this.serverAddress = serverAddress;
        super.connect(serverAddress);
    }

    @Override
    public void initialize(final Bootstrap bootstrap) {
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 4_000);
        bootstrap.attr(LEGACY_PROXY_CONNECTION_ATTRIBUTE_KEY, this);
        super.initialize(bootstrap);
    }

    public Channel getC2P() {
        return this.c2p;
    }

    public ServerAddress getServerAddress() {
        return this.serverAddress;
    }

}
