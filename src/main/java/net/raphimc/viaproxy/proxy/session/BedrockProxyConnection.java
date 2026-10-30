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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.raphimc.minecraftauth.bedrock.model.MinecraftMultiplayerToken;
import net.raphimc.netminecraft.util.EventLoops;
import net.raphimc.netminecraft.util.TransportType;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.util.address.NetherNetLanAddress;
import net.raphimc.viaproxy.util.address.NetherNetXboxAddress;
import net.raphimc.viaproxy.util.address.NetherNetXboxJsonRpcAddress;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChannelFactory;
import org.cloudburstmc.netty.channel.nethernet.config.NetherChannelOption;
import org.cloudburstmc.netty.channel.nethernet.signaling.*;
import org.cloudburstmc.netty.util.nethernet.OperatorIdentity;

import java.net.SocketAddress;

public class BedrockProxyConnection extends ProxyConnection {

    private SocketAddress serverAddress;

    public BedrockProxyConnection(final ChannelInitializer<Channel> channelInitializer, final Channel c2p) {
        super(channelInitializer, c2p);
    }

    @Override
    public void initialize(final TransportType transportType, final Bootstrap bootstrap) {
        final NetherNetClientSignaling netherNetSignaling;
        if (this.serverAddress instanceof NetherNetLanAddress) {
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
            netherNetSignaling = new NetherNetHTTPClientSignaling();
        }

        final OperatorIdentity identity;
        if (this.getUserOptions().account() instanceof BedrockAccount bedrockAccount) {
            final BedrockAuthManager authManager = bedrockAccount.getAuthManager();
            final MinecraftMultiplayerToken multiplayerToken = authManager.getMinecraftMultiplayerToken().getUpToDateUnchecked();
            identity = OperatorIdentity.fromToken(authManager.getSessionKeyPair(), multiplayerToken.getToken(), "https://authorization.franchise.minecraft-services.net/");
        } else {
            identity = null;
        }

        bootstrap
                .group(EventLoops.getClientEventLoop(TransportType.NIO))
                .channelFactory(NetherNetChannelFactory.client(netherNetSignaling))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ViaProxy.getConfig().getConnectTimeout())
                .option(NetherChannelOption.NETHER_CLIENT_HANDSHAKE_TIMEOUT_MS, ViaProxy.getConfig().getConnectTimeout())
                .option(NetherChannelOption.NETHER_CLIENT_MAX_HANDSHAKE_ATTEMPTS, 1)
                .option(NetherChannelOption.NETHER_CLIENT_IDENTITY, identity)
                .attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY, this)
                .handler(this.channelInitializer);
        this.channelFuture = bootstrap.register().syncUninterruptibly();
    }

    @Override
    public ChannelFuture connectToServer(final SocketAddress serverAddress, final ProtocolVersion targetVersion) {
        this.serverAddress = serverAddress;
        return super.connectToServer(serverAddress, targetVersion);
    }

}
