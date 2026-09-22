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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.raphimc.minecraftauth.bedrock.model.MinecraftMultiplayerToken;
import net.raphimc.netminecraft.util.EventLoops;
import net.raphimc.netminecraft.util.TransportType;
import net.raphimc.viabedrock.netty.raknet.MessageCodec;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.util.address.*;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannelFactory;
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelOption;
import org.cloudburstmc.netty.channel.nethernet.signaling.*;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.RakClientChannel;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.netty.util.nethernet.OperatorIdentity;

import java.net.SocketAddress;
import java.util.concurrent.ThreadLocalRandom;

public class BedrockProxyConnection extends ProxyConnection {

    private SocketAddress serverAddress;

    public BedrockProxyConnection(final ChannelInitializer<Channel> channelInitializer, final Channel c2p) {
        super(channelInitializer, c2p);
    }

    @Override
    public void initialize(final TransportType transportType, final Bootstrap bootstrap) {
        bootstrap
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ViaProxy.getConfig().getConnectTimeout())
                .attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY, this)
                .handler(this.channelInitializer);

        if (this.serverAddress instanceof NetherNetAddress) {
            this.initializeNetherNet(transportType, bootstrap);
        } else {
            this.initializeRakNet(transportType, bootstrap);
        }

        this.channelFuture = bootstrap.register().syncUninterruptibly();
    }

    @Override
    public ChannelFuture connectToServer(final SocketAddress serverAddress, final ProtocolVersion targetVersion) {
        this.serverAddress = serverAddress;
        return super.connectToServer(serverAddress, targetVersion);
    }

    protected void initializeRakNet(TransportType transportType, final Bootstrap bootstrap) {
        if (!DatagramChannel.class.isAssignableFrom(transportType.udpClientChannelClass())) {
            throw new IllegalArgumentException("Channel type must be a DatagramChannel");
        }
        if (transportType == TransportType.KQUEUE) {
            transportType = TransportType.NIO; // KQueue doesn't work due to requiring the channel to be bound instead of connected
        }

        final RakChannelFactory<RakClientChannel> channelFactory = RakChannelFactory.client((Class<? extends DatagramChannel>) transportType.udpClientChannelClass());
        bootstrap
                .group(EventLoops.getClientEventLoop(transportType))
                .channelFactory(() -> {
                    final Channel channel = channelFactory.newChannel();
                    if (channel.config().setOption(RakChannelOption.RAK_IP_DONT_FRAGMENT, true)) {
                        channel.config().setOption(RakChannelOption.RAK_MTU_SIZES, new Integer[]{1492, 1200, 576});
                    }
                    return channel;
                })
                .option(RakChannelOption.RAK_PROTOCOL_VERSION, ProtocolConstants.BEDROCK_RAKNET_PROTOCOL_VERSION)
                .option(RakChannelOption.RAK_COMPATIBILITY_MODE, true)
                .option(RakChannelOption.RAK_CLIENT_INTERNAL_ADDRESSES, 20)
                .option(RakChannelOption.RAK_TIME_BETWEEN_SEND_CONNECTION_ATTEMPTS_MS, 500)
                .option(RakChannelOption.RAK_CONNECT_TIMEOUT, (long) ViaProxy.getConfig().getConnectTimeout())
                .option(RakChannelOption.RAK_SESSION_TIMEOUT, 30_000L)
                .option(RakChannelOption.RAK_GUID, ThreadLocalRandom.current().nextLong());
    }

    protected void initializeNetherNet(final TransportType transportType, final Bootstrap bootstrap) {
        final NetherNetClientSignaling netherNetSignaling;
        if (this.serverAddress instanceof NetherNetHttpAddress) {
            netherNetSignaling = new NetherNetHTTPClientSignaling();
        } else if (this.serverAddress instanceof NetherNetLanAddress) {
            netherNetSignaling = new NetherNetDiscoverySignaling();
        } else if (this.serverAddress instanceof NetherNetXboxAddress || this.serverAddress instanceof NetherNetXboxJsonRpcAddress) {
            if (this.getUserOptions().account() instanceof BedrockAccount bedrockAccount) {
                if (this.serverAddress instanceof NetherNetXboxAddress) {
                    netherNetSignaling = new NetherNetXboxSignaling(bedrockAccount.getAuthManager().getMinecraftSession().getUpToDateUnchecked().getAuthorizationHeader());
                } else if (this.serverAddress instanceof NetherNetXboxJsonRpcAddress) {
                    netherNetSignaling = new NetherNetXboxRpcSignaling(bedrockAccount.getAuthManager().getMinecraftSession().getUpToDateUnchecked().getAuthorizationHeader());
                } else {
                    throw new IllegalStateException("Unsupported NetherNet address type: " + this.serverAddress.getClass().getName());
                }
            } else {
                this.kickClient("§cThe configured target server requires Xbox signaling, but no Minecraft: Bedrock Edition account is selected.");
                return;
            }
        } else {
            throw new IllegalStateException("Unsupported NetherNet address type: " + this.serverAddress.getClass().getName());
        }

        final OperatorIdentity identity;
        if (this.getUserOptions().account() instanceof BedrockAccount bedrockAccount) {
            final BedrockAuthManager authManager = bedrockAccount.getAuthManager();
            final MinecraftMultiplayerToken multiplayerToken = authManager.getMinecraftMultiplayerToken().getUpToDateUnchecked();
            identity = OperatorIdentity.fromToken(authManager.getSessionKeyPair(), multiplayerToken.getToken(), "https://authorization.franchise.minecraft-services.net/");
        } else {
            identity = null;
        }

        final ChannelHandler channelHandler = bootstrap.config().handler();
        bootstrap
                .group(EventLoops.getClientEventLoop(TransportType.NIO))
                .channelFactory(NetherNetChannelFactory.client(netherNetSignaling))
                .option(NetherChannelOption.NETHER_CLIENT_HANDSHAKE_TIMEOUT_MS, ViaProxy.getConfig().getConnectTimeout())
                .option(NetherChannelOption.NETHER_CLIENT_MAX_HANDSHAKE_ATTEMPTS, 1)
                .option(NetherChannelOption.NETHER_CLIENT_IDENTITY, identity)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(final Channel channel) {
                        channel.pipeline().addLast(channelHandler);
                        channel.pipeline().remove(MessageCodec.NAME);
                    }
                });
    }

}
