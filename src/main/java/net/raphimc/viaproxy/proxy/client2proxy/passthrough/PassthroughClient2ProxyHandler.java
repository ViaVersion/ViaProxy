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
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.Proxy2ServerHandlerCreationEvent;
import net.raphimc.viaproxy.proxy.proxy2server.passthrough.PassthroughProxy2ServerChannelInitializer;
import net.raphimc.viaproxy.proxy.proxy2server.passthrough.PassthroughProxy2ServerHandler;
import net.raphimc.viaproxy.proxy.util.ExceptionUtil;
import net.raphimc.viaproxy.proxy.util.HAProxyUtil;
import net.raphimc.viaproxy.util.logging.Logger;

import java.util.function.Supplier;

public class PassthroughClient2ProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {

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
            this.connectToServer();
        }

        this.p2sConnection.getChannel().writeAndFlush(msg.retain()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ExceptionUtil.handleNettyException(ctx, cause, null);
    }

    protected void connectToServer() {
        final Supplier<ChannelHandler> handlerSupplier = () -> PluginManager.EVENT_MANAGER.call(new Proxy2ServerHandlerCreationEvent(new PassthroughProxy2ServerHandler(this), true)).getHandler();
        this.p2sConnection = new NetClient(handlerSupplier, PassthroughProxy2ServerChannelInitializer::new) {
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

        if (Options.SERVER_HAPROXY_PROTOCOL) {
            this.p2sConnection.getChannel().writeAndFlush(HAProxyUtil.createMessage(this.c2pChannel, this.p2sConnection.getChannel(), null)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
    }

    protected ServerAddress getServerAddress() {
        return new ServerAddress(Options.CONNECT_ADDRESS, Options.CONNECT_PORT);
    }

    public Channel getC2pChannel() {
        return this.c2pChannel;
    }

}
