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
package net.raphimc.viaproxy.proxy.proxy2server;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.platform.ViaChannelInitializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.codec.NoReadFlowControlHandler;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;
import net.raphimc.netminecraft.packet.registry.DefaultPacketRegistry;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.netty.BatchLengthCodec;
import net.raphimc.viabedrock.netty.DisconnectHandler;
import net.raphimc.viabedrock.netty.PacketCodec;
import net.raphimc.viabedrock.netty.raknet.MessageCodec;
import net.raphimc.viabedrock.netty.util.DatagramCodec;
import net.raphimc.viabedrock.protocol.NetherNetStatusProtocol;
import net.raphimc.viabedrock.protocol.RakNetStatusProtocol;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.netty.PreNettyLengthCodec;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.events.Proxy2ServerChannelInitializeEvent;
import net.raphimc.viaproxy.plugins.events.types.ITyped;
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyViaCodec;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.util.NetherNetInetSocketAddress;

import java.net.InetSocketAddress;
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

        if (ViaProxy.getConfig().getBackendProxy() != null && !proxyConnection.getServerVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
            channel.pipeline().addLast(VIAPROXY_PROXY_HANDLER_NAME, ViaProxy.getConfig().getBackendProxy().createNettyProxyHandler());
        }
        if (ViaProxy.getConfig().useBackendHaProxy()) {
            channel.pipeline().addLast(VIAPROXY_HAPROXY_ENCODER_NAME, HAProxyMessageEncoder.INSTANCE);
        }

        super.initChannel(channel);
        channel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(new DefaultPacketRegistry(true, proxyConnection.getClientVersion().getVersion()));

        final UserConnection user = ViaChannelInitializer.createUserConnection(channel, true);
        proxyConnection.setUserConnection(user);

        channel.pipeline().addBefore(MCPipeline.PACKET_CODEC_HANDLER_NAME, ViaProxyViaCodec.NAME, new ViaProxyViaCodec(user));
        channel.pipeline().addAfter(ViaProxyViaCodec.NAME, "via-" + MCPipeline.FLOW_CONTROL_HANDLER_NAME, new NoReadFlowControlHandler());
        if (proxyConnection.getServerVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_6_4)) {
            channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, PreNettyLengthCodec.NAME, new PreNettyLengthCodec(user));
        } else if (proxyConnection.getServerVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
            channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, DisconnectHandler.NAME, new DisconnectHandler());
            channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, MessageCodec.NAME, new MessageCodec());
            channel.pipeline().replace(MCPipeline.SIZER_HANDLER_NAME, MCPipeline.SIZER_HANDLER_NAME, new BatchLengthCodec());
            channel.pipeline().addBefore(ViaProxyViaCodec.NAME, PacketCodec.NAME, new PacketCodec());

            channel.pipeline().remove(MCPipeline.COMPRESSION_HANDLER_NAME);
            channel.pipeline().remove(MCPipeline.ENCRYPTION_HANDLER_NAME);

            if (proxyConnection.getC2pConnectionState() == ConnectionState.STATUS) {
                channel.pipeline().remove(MCPipeline.SIZER_HANDLER_NAME);
                channel.pipeline().remove(PacketCodec.NAME);
                channel.pipeline().replace(MessageCodec.NAME, "viabedrock-datagram-codec", new DatagramCodec());
                if (proxyConnection.getServerAddress() instanceof NetherNetInetSocketAddress) {
                    user.getProtocolInfo().getPipeline().add(NetherNetStatusProtocol.INSTANCE);
                } else if (proxyConnection.getServerAddress() instanceof InetSocketAddress) {
                    user.getProtocolInfo().getPipeline().add(RakNetStatusProtocol.INSTANCE);
                } else {
                    throw new UnsupportedOperationException("Unsupported address type for Bedrock status: " + proxyConnection.getServerAddress().getClass().getName());
                }
            }
        }

        if (ViaProxy.EVENT_MANAGER.call(new Proxy2ServerChannelInitializeEvent(ITyped.Type.POST, channel, false)).isCancelled()) {
            channel.close();
        }
    }

}
