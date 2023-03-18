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
package net.raphimc.viaproxy.proxy.proxy2server;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;
import net.raphimc.netminecraft.packet.registry.PacketRegistryUtil;
import net.raphimc.vialegacy.netty.PreNettyDecoder;
import net.raphimc.vialegacy.netty.PreNettyEncoder;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.baseprotocols.PreNettyBaseProtocol;
import net.raphimc.viaprotocolhack.netty.VPHEncodeHandler;
import net.raphimc.viaprotocolhack.netty.VPHPipeline;
import net.raphimc.viaprotocolhack.util.VersionEnum;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.Proxy2ServerChannelInitializeEvent;
import net.raphimc.viaproxy.plugins.events.types.ITyped;
import net.raphimc.viaproxy.protocolhack.impl.ViaProxyViaDecodeHandler;
import net.raphimc.viaproxy.proxy.ProxyConnection;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Locale;
import java.util.function.Supplier;

public class Proxy2ServerChannelInitializer extends MinecraftChannelInitializer {

    public Proxy2ServerChannelInitializer(final Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(Channel channel) {
        if (PluginManager.EVENT_MANAGER.call(new Proxy2ServerChannelInitializeEvent(ITyped.Type.PRE, channel)).isCancelled()) {
            channel.close();
            return;
        }

        final UserConnection user = new UserConnectionImpl(channel, true);
        new ProtocolPipelineImpl(user);
        ProxyConnection.fromChannel(channel).setUserConnection(user);

        super.initChannel(channel);
        channel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getHandshakeRegistry(true));
        channel.pipeline().addBefore(MCPipeline.PACKET_CODEC_HANDLER_NAME, VPHPipeline.ENCODER_HANDLER_NAME, new VPHEncodeHandler(user));
        channel.pipeline().addBefore(MCPipeline.PACKET_CODEC_HANDLER_NAME, VPHPipeline.DECODER_HANDLER_NAME, new ViaProxyViaDecodeHandler(user));

        if (ProxyConnection.fromChannel(channel).getServerVersion().isOlderThanOrEqualTo(VersionEnum.r1_6_4)) {
            user.getProtocolInfo().getPipeline().add(PreNettyBaseProtocol.INSTANCE);
            channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, VPHPipeline.PRE_NETTY_ENCODER_HANDLER_NAME, new PreNettyEncoder(user));
            channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, VPHPipeline.PRE_NETTY_DECODER_HANDLER_NAME, new PreNettyDecoder(user));
        }

        if (Options.PROXY_URL != null) {
            channel.pipeline().addFirst("viaproxy-proxy-handler", this.getProxyHandler());
        }

        if (PluginManager.EVENT_MANAGER.call(new Proxy2ServerChannelInitializeEvent(ITyped.Type.POST, channel)).isCancelled()) {
            channel.close();
        }
    }

    protected ProxyHandler getProxyHandler() {
        final URI proxyUrl = Options.PROXY_URL;
        final InetSocketAddress proxyAddress = new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort());
        final String username = proxyUrl.getUserInfo() != null ? proxyUrl.getUserInfo().split(":")[0] : null;
        final String password = proxyUrl.getUserInfo() != null && proxyUrl.getUserInfo().contains(":") ? proxyUrl.getUserInfo().split(":")[1] : null;

        switch (proxyUrl.getScheme().toUpperCase(Locale.ROOT)) {
            case "HTTP":
            case "HTTPS":
                if (username != null && password != null) return new HttpProxyHandler(proxyAddress, username, password);
                else return new HttpProxyHandler(proxyAddress);
            case "SOCKS4":
                if (username != null) return new Socks4ProxyHandler(proxyAddress, username);
                else return new Socks4ProxyHandler(proxyAddress);
            case "SOCKS5":
                if (username != null && password != null) return new Socks5ProxyHandler(proxyAddress, username, password);
                else return new Socks5ProxyHandler(proxyAddress);
        }

        throw new IllegalArgumentException("Unknown proxy type: " + proxyUrl.getScheme());
    }

}
