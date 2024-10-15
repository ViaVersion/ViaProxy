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
package net.raphimc.viaproxy.proxy.proxy2server;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.codec.NoReadFlowControlHandler;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;
import net.raphimc.netminecraft.packet.registry.DefaultPacketRegistry;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialoader.netty.VLPipeline;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.events.Proxy2ServerChannelInitializeEvent;
import net.raphimc.viaproxy.plugins.events.types.ITyped;
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyVLPipeline;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Locale;
import java.util.function.Supplier;

public class Proxy2ServerChannelInitializer extends MinecraftChannelInitializer {

    public static final String VIAPROXY_PROXY_HANDLER_NAME = "viaproxy-proxy-handler";
    public static final String VIAPROXY_HAPROXY_ENCODER_NAME = "viaproxy-haproxy-encoder";

    public Proxy2ServerChannelInitializer(final Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(Channel channel) {
        if (ViaProxy.EVENT_MANAGER.call(new Proxy2ServerChannelInitializeEvent(ITyped.Type.PRE, channel, false)).isCancelled()) {
            channel.close();
            return;
        }

        final ProxyConnection proxyConnection = ProxyConnection.fromChannel(channel);

        if (ViaProxy.getConfig().getBackendProxyUrl() != null && !proxyConnection.getServerVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
            channel.pipeline().addLast(VIAPROXY_PROXY_HANDLER_NAME, this.getProxyHandler());
        }
        if (ViaProxy.getConfig().useBackendHaProxy()) {
            channel.pipeline().addLast(VIAPROXY_HAPROXY_ENCODER_NAME, HAProxyMessageEncoder.INSTANCE);
        }

        super.initChannel(channel);
        channel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(new DefaultPacketRegistry(true, proxyConnection.getClientVersion().getVersion()));

        final UserConnection user = new UserConnectionImpl(channel, true);
        new ProtocolPipelineImpl(user);
        proxyConnection.setUserConnection(user);
        channel.pipeline().addLast(new ViaProxyVLPipeline(user));
        channel.pipeline().addAfter(VLPipeline.VIA_CODEC_NAME, "via-" + MCPipeline.FLOW_CONTROL_HANDLER_NAME, new NoReadFlowControlHandler());
        if (proxyConnection.getServerVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
            channel.pipeline().remove(MCPipeline.COMPRESSION_HANDLER_NAME);
            channel.pipeline().remove(MCPipeline.ENCRYPTION_HANDLER_NAME);
        }

        if (ViaProxy.EVENT_MANAGER.call(new Proxy2ServerChannelInitializeEvent(ITyped.Type.POST, channel, false)).isCancelled()) {
            channel.close();
        }
    }

    protected ProxyHandler getProxyHandler() {
        final URI proxyUrl = ViaProxy.getConfig().getBackendProxyUrl();
        final InetSocketAddress proxyAddress = new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort());
        final String username = proxyUrl.getUserInfo() != null ? proxyUrl.getUserInfo().split(":")[0] : null;
        final String password = proxyUrl.getUserInfo() != null && proxyUrl.getUserInfo().contains(":") ? proxyUrl.getUserInfo().split(":")[1] : null;

        switch (proxyUrl.getScheme().toUpperCase(Locale.ROOT)) {
            case "HTTP", "HTTPS" -> {
                if (username != null && password != null) return new HttpProxyHandler(proxyAddress, username, password);
                else return new HttpProxyHandler(proxyAddress);
            }
            case "SOCKS4" -> {
                if (username != null) return new Socks4ProxyHandler(proxyAddress, username);
                else return new Socks4ProxyHandler(proxyAddress);
            }
            case "SOCKS5" -> {
                if (username != null && password != null) return new Socks5ProxyHandler(proxyAddress, username, password);
                else return new Socks5ProxyHandler(proxyAddress);
            }
        }

        throw new IllegalArgumentException("Unknown proxy type: " + proxyUrl.getScheme());
    }

}
