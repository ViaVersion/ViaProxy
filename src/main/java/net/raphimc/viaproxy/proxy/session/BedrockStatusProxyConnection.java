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
package net.raphimc.viaproxy.proxy.session;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import net.raphimc.netminecraft.util.EventLoops;
import net.raphimc.netminecraft.util.TransportType;

public class BedrockStatusProxyConnection extends ProxyConnection {

    public BedrockStatusProxyConnection(final ChannelInitializer<Channel> channelInitializer, final Channel c2p) {
        super(channelInitializer, c2p);
    }

    @Override
    public void initialize(final TransportType transportType, final Bootstrap bootstrap) {
        bootstrap
                .group(EventLoops.getClientEventLoop(transportType))
                .channel(transportType.udpClientChannelClass())
                .attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY, this)
                .handler(this.channelInitializer);

        this.channelFuture = bootstrap.register().syncUninterruptibly();
    }

}
