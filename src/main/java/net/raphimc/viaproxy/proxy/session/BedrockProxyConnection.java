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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.util.LazyLoadBase;
import net.raphimc.netminecraft.util.ServerAddress;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.vialoader.netty.VLPipeline;
import net.raphimc.vialoader.netty.viabedrock.PingEncapsulationCodec;
import net.raphimc.vialoader.util.VersionEnum;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;

public class BedrockProxyConnection extends ProxyConnection {

    public BedrockProxyConnection(Supplier<ChannelHandler> handlerSupplier, Function<Supplier<ChannelHandler>, ChannelInitializer<Channel>> channelInitializerSupplier, Channel c2p) {
        super(handlerSupplier, channelInitializerSupplier, c2p);
    }

    @Override
    public void initialize(Bootstrap bootstrap) {
        if (Epoll.isAvailable()) {
            bootstrap
                    .group(LazyLoadBase.CLIENT_EPOLL_EVENTLOOP.getValue())
                    .channelFactory(RakChannelFactory.client(EpollDatagramChannel.class));
        } else {
            bootstrap
                    .group(LazyLoadBase.CLIENT_NIO_EVENTLOOP.getValue())
                    .channelFactory(RakChannelFactory.client(NioDatagramChannel.class));
        }

        bootstrap
                .option(RakChannelOption.CONNECT_TIMEOUT_MILLIS, 4_000)
                .option(RakChannelOption.IP_TOS, 0x18)
                .option(RakChannelOption.RAK_PROTOCOL_VERSION, ProtocolConstants.BEDROCK_RAKNET_PROTOCOL_VERSION)
                .option(RakChannelOption.RAK_CONNECT_TIMEOUT, 4_000L)
                .option(RakChannelOption.RAK_SESSION_TIMEOUT, 30_000L)
                .option(RakChannelOption.RAK_GUID, ThreadLocalRandom.current().nextLong())
                .attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY, this)
                .handler(this.channelInitializerSupplier.apply(this.handlerSupplier));

        this.channelFuture = bootstrap.register().syncUninterruptibly();
    }

    @Override
    public ChannelFuture connectToServer(ServerAddress serverAddress, VersionEnum targetVersion) {
        if (this.getC2pConnectionState() == ConnectionState.STATUS) {
            RStream.of(this).withSuper().fields().by("serverAddress").set(serverAddress);
            RStream.of(this).withSuper().fields().by("serverVersion").set(targetVersion);
            return this.ping(serverAddress);
        } else {
            return super.connectToServer(serverAddress, targetVersion);
        }
    }

    private ChannelFuture ping(final ServerAddress serverAddress) {
        if (this.channelFuture == null) this.initialize(new Bootstrap());

        this.channelFuture.channel().eventLoop().submit(() -> {
            this.getChannel().pipeline().replace(VLPipeline.VIABEDROCK_FRAME_ENCAPSULATION_HANDLER_NAME, "ping_encapsulation", new PingEncapsulationCodec(serverAddress.toSocketAddress()));
            this.getChannel().pipeline().remove(VLPipeline.VIABEDROCK_PACKET_ENCAPSULATION_HANDLER_NAME);
            this.getChannel().pipeline().remove(MCPipeline.SIZER_HANDLER_NAME);
        });

        return this.getChannel().bind(new InetSocketAddress(0));
    }

}
