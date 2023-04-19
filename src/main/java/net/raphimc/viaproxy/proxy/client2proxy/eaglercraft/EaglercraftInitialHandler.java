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
package net.raphimc.viaproxy.proxy.client2proxy.eaglercraft;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyChannelInitializer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class EaglercraftInitialHandler extends ByteToMessageDecoder {

    private static SslContext sslContext;

    static {
        final File certFolder = new File("certs");
        if (certFolder.exists()) {
            try {
                sslContext = SslContextBuilder.forServer(new File(certFolder, "fullchain.pem"), new File(certFolder, "privkey.pem")).build();
            } catch (Throwable e) {
                throw new RuntimeException("Failed to load SSL context", e);
            }
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!ctx.channel().isOpen()) return;
        if (!in.isReadable()) return;

        if (in.readableBytes() >= 3 || in.getByte(0) != 'G') {
            if (in.readableBytes() >= 3 && in.getCharSequence(0, 3, StandardCharsets.UTF_8).equals("GET")) { // Websocket request
                if (sslContext != null) {
                    ctx.pipeline().addBefore(Client2ProxyChannelInitializer.EAGLERCRAFT_INITIAL_HANDLER_NAME, Client2ProxyChannelInitializer.WEBSOCKET_SSL_HANDLER_NAME, sslContext.newHandler(ctx.alloc()));
                }

                ctx.pipeline().addBefore(Client2ProxyChannelInitializer.EAGLERCRAFT_INITIAL_HANDLER_NAME, Client2ProxyChannelInitializer.WEBSOCKET_HTTP_CODEC_NAME, new HttpServerCodec());
                ctx.pipeline().addBefore(Client2ProxyChannelInitializer.EAGLERCRAFT_INITIAL_HANDLER_NAME, Client2ProxyChannelInitializer.WEBSOCKET_HTTP_AGGREGATOR_NAME, new HttpObjectAggregator(65535, true));
                ctx.pipeline().addBefore(Client2ProxyChannelInitializer.EAGLERCRAFT_INITIAL_HANDLER_NAME, Client2ProxyChannelInitializer.WEBSOCKET_COMPRESSION_NAME, new WebSocketServerCompressionHandler());
                ctx.pipeline().addBefore(Client2ProxyChannelInitializer.EAGLERCRAFT_INITIAL_HANDLER_NAME, Client2ProxyChannelInitializer.WEBSOCKET_HANDLER_NAME, new WebSocketServerProtocolHandler("/", null, true));
                ctx.pipeline().addBefore(Client2ProxyChannelInitializer.EAGLERCRAFT_INITIAL_HANDLER_NAME, Client2ProxyChannelInitializer.WEBSOCKET_ACTIVE_NOTIFIER_NAME, new WebSocketActiveNotifier());
                ctx.pipeline().addBefore(Client2ProxyChannelInitializer.EAGLERCRAFT_INITIAL_HANDLER_NAME, Client2ProxyChannelInitializer.EAGLERCRAFT_HANDLER_NAME, new EaglercraftHandler());

                ctx.fireUserEventTriggered(EaglercraftClientConnected.INSTANCE);

                ctx.pipeline().fireChannelRead(in.readBytes(in.readableBytes()));
            } else {
                out.add(in.readBytes(in.readableBytes()));
            }

            ctx.pipeline().remove(this);
        }
    }

    public static final class EaglercraftClientConnected {
        public static final EaglercraftClientConnected INSTANCE = new EaglercraftClientConnected();
    }

}
