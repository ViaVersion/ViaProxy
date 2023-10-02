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

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import net.raphimc.netminecraft.util.ServerAddress;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.Proxy2ServerHandlerCreationEvent;
import net.raphimc.viaproxy.proxy.proxy2server.passthrough.PassthroughProxy2ServerChannelInitializer;
import net.raphimc.viaproxy.proxy.proxy2server.passthrough.PassthroughProxy2ServerHandler;
import net.raphimc.viaproxy.proxy.session.LegacyProxyConnection;
import net.raphimc.viaproxy.proxy.util.ChannelUtil;
import net.raphimc.viaproxy.proxy.util.ExceptionUtil;
import net.raphimc.viaproxy.proxy.util.HAProxyUtil;
import net.raphimc.viaproxy.proxy.util.ThrowingChannelFutureListener;
import net.raphimc.viaproxy.util.logging.Logger;

import java.util.function.Supplier;

public class PassthroughClient2ProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private LegacyProxyConnection proxyConnection;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        try {
            if (this.proxyConnection != null) {
                this.proxyConnection.getChannel().close();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        if (!ctx.channel().isOpen()) return;
        if (!msg.isReadable()) return;

        if (this.proxyConnection == null) {
            this.connectToServer(ctx.channel());
        }

        this.proxyConnection.getChannel().writeAndFlush(msg.retain()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ExceptionUtil.handleNettyException(ctx, cause, null);
    }

    protected void connectToServer(final Channel c2pChannel) {
        final Supplier<ChannelHandler> handlerSupplier = () -> PluginManager.EVENT_MANAGER.call(new Proxy2ServerHandlerCreationEvent(new PassthroughProxy2ServerHandler(), true)).getHandler();
        this.proxyConnection = new LegacyProxyConnection(handlerSupplier, PassthroughProxy2ServerChannelInitializer::new, c2pChannel);
        this.proxyConnection.getC2P().attr(LegacyProxyConnection.LEGACY_PROXY_CONNECTION_ATTRIBUTE_KEY).set(this.proxyConnection);

        final ServerAddress serverAddress = this.getServerAddress();

        ChannelUtil.disableAutoRead(this.proxyConnection.getC2P());
        Logger.u_info("connect", this.proxyConnection.getC2P().remoteAddress(), null, "[Legacy <-> Legacy] Connecting to " + serverAddress.getAddress() + ":" + serverAddress.getPort());

        this.proxyConnection.connect(serverAddress).addListeners((ThrowingChannelFutureListener) f -> {
            if (f.isSuccess()) {
                if (Options.SERVER_HAPROXY_PROTOCOL) {
                    this.proxyConnection.getChannel().writeAndFlush(HAProxyUtil.createMessage(this.proxyConnection.getC2P(), this.proxyConnection.getChannel(), null)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                }

                ChannelUtil.restoreAutoRead(this.proxyConnection.getC2P());
            }
        }, (ThrowingChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                Logger.LOGGER.error("Failed to connect to target server", f.cause());
                this.proxyConnection.getC2P().close();
                this.proxyConnection = null;
            }
        });
    }

    protected ServerAddress getServerAddress() {
        return new ServerAddress(Options.CONNECT_ADDRESS, Options.CONNECT_PORT);
    }

}
