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
package net.raphimc.viaproxy.proxy.client2proxy.passthrough;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.events.Client2ProxyHandlerCreationEvent;
import net.raphimc.viaproxy.proxy.util.ExceptionUtil;

import java.util.function.Supplier;

public class LegacyPassthroughInitialHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        if (!ctx.channel().isOpen()) return;
        if (!msg.isReadable()) return;

        final int lengthOrPacketId = msg.getUnsignedByte(0);
        ctx.pipeline().remove(this);

        if (lengthOrPacketId == 0/*classic*/ || lengthOrPacketId == 1/*a1.0.15*/ || lengthOrPacketId == 2/*<= 1.6.4*/ || lengthOrPacketId == 254/*<= 1.6.4 (ping)*/) {
            while (ctx.pipeline().last() != null) {
                ctx.pipeline().removeLast();
            }

            final Supplier<ChannelHandler> handlerSupplier = () -> ViaProxy.EVENT_MANAGER.call(new Client2ProxyHandlerCreationEvent(new PassthroughClient2ProxyHandler(), true)).getHandler();
            final PassthroughClient2ProxyChannelInitializer channelInitializer = new PassthroughClient2ProxyChannelInitializer(handlerSupplier);
            ctx.channel().pipeline().addLast(channelInitializer);
            ctx.channel().pipeline().fireChannelActive();
        }

        ctx.pipeline().fireChannelRead(msg.retain());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ExceptionUtil.handleNettyException(ctx, cause, null, true);
    }

}
