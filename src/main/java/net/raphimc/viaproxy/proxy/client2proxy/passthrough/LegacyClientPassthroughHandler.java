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
package net.raphimc.viaproxy.proxy.client2proxy.passthrough;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import net.raphimc.netminecraft.netty.connection.NetClient;
import net.raphimc.netminecraft.util.ServerAddress;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.proxy.util.ExceptionUtil;
import net.raphimc.viaproxy.util.logging.Logger;

import java.util.function.Function;
import java.util.function.Supplier;

public class LegacyClientPassthroughHandler extends SimpleChannelInboundHandler<ByteBuf> {

    protected Channel c2pChannel;
    protected NetClient p2sConnection;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        this.c2pChannel = ctx.channel();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        try {
            if (this.p2sConnection != null) {
                this.p2sConnection.getChannel().close();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        if (!ctx.channel().isOpen()) return;
        if (!msg.isReadable()) return;

        if (this.p2sConnection == null) {
            final int length = msg.getUnsignedByte(0);
            if (length == 0/*classic*/ || length == 1/*a1.0.15*/ || length == 2/*<= 1.6.4*/ || length == 254/*<= 1.6.4 (ping)*/) {
                while (ctx.pipeline().last() != this) {
                    ctx.pipeline().removeLast();
                }

                this.connectToServer();
            } else {
                ctx.pipeline().remove(this);
                ctx.pipeline().fireChannelRead(msg.retain());
                return;
            }
        }

        this.p2sConnection.getChannel().writeAndFlush(msg.retain()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ExceptionUtil.handleNettyException(ctx, cause, null);
    }

    protected void connectToServer() {
        this.p2sConnection = new NetClient(this.getHandlerSupplier(), this.getChannelInitializerSupplier()) {
            @Override
            public void initialize(Bootstrap bootstrap) {
                bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 4_000);
                super.initialize(bootstrap);
            }
        };

        try {
            this.p2sConnection.connect(this.getServerAddress());
        } catch (Throwable e) {
            Logger.LOGGER.error("Failed to connect to target server", e);
            this.p2sConnection = null;
            this.c2pChannel.close();
        }
    }

    protected ServerAddress getServerAddress() {
        return new ServerAddress(Options.CONNECT_ADDRESS, Options.CONNECT_PORT);
    }

    protected Function<Supplier<ChannelHandler>, ChannelInitializer<Channel>> getChannelInitializerSupplier() {
        return s -> new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) {
                channel.pipeline().addLast(s.get());
            }
        };
    }

    protected Supplier<ChannelHandler> getHandlerSupplier() {
        return () -> new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                super.channelInactive(ctx);

                try {
                    LegacyClientPassthroughHandler.this.c2pChannel.close();
                } catch (Throwable ignored) {
                }
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                LegacyClientPassthroughHandler.this.c2pChannel.writeAndFlush(msg.retain()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                ExceptionUtil.handleNettyException(ctx, cause, null);
            }
        };
    }

}
