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
package net.raphimc.viaproxy.proxy.client2proxy;

import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import net.lenni0451.reflect.stream.RStream;

import java.net.InetSocketAddress;

public class HAProxyHandler extends SimpleChannelInboundHandler<HAProxyMessage> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HAProxyMessage message) throws Exception {
        if (message.command() != HAProxyCommand.PROXY) {
            throw new UnsupportedOperationException("Unsupported HAProxy command: " + message.command());
        }
        if (message.sourceAddress() != null) {
            final InetSocketAddress sourceAddress = new InetSocketAddress(message.sourceAddress(), message.sourcePort());
            if (ctx.channel() instanceof AbstractChannel) {
                RStream.of(AbstractChannel.class, ctx.channel()).fields().by("remoteAddress").set(sourceAddress);
            }
        }

        ctx.pipeline().remove(this);
        super.channelActive(ctx);
    }

}
