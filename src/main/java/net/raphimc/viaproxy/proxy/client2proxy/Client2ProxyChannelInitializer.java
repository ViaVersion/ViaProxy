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
package net.raphimc.viaproxy.proxy.client2proxy;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.codec.NoReadFlowControlHandler;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;
import net.raphimc.netminecraft.packet.registry.DefaultPacketRegistry;
import net.raphimc.vialoader.netty.VLPipeline;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.events.Client2ProxyChannelInitializeEvent;
import net.raphimc.viaproxy.plugins.events.types.ITyped;
import net.raphimc.viaproxy.protocoltranslator.impl.ViaProxyVLPipeline;
import net.raphimc.viaproxy.proxy.client2proxy.passthrough.LegacyPassthroughInitialHandler;

import java.util.function.Supplier;

public class Client2ProxyChannelInitializer extends MinecraftChannelInitializer {

    public static final String VIAPROXY_HAPROXY_DECODER_NAME = "viaproxy-haproxy-decoder";
    public static final String VIAPROXY_HAPROXY_HANDLER_NAME = "viaproxy-haproxy-handler";
    public static final String LEGACY_PASSTHROUGH_INITIAL_HANDLER_NAME = "legacy-passthrough-initial-handler";

    public Client2ProxyChannelInitializer(final Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(Channel channel) {
        if (ViaProxy.EVENT_MANAGER.call(new Client2ProxyChannelInitializeEvent(ITyped.Type.PRE, channel, false)).isCancelled()) {
            channel.close();
            return;
        }

        if (ViaProxy.getConfig().useFrontendHaProxy()) {
            channel.pipeline().addLast(VIAPROXY_HAPROXY_DECODER_NAME, new HAProxyMessageDecoder());
            channel.pipeline().addLast(VIAPROXY_HAPROXY_HANDLER_NAME, new HAProxyHandler());
        }
        if (ViaProxy.getConfig().shouldAllowLegacyClientPassthrough()) {
            channel.pipeline().addLast(LEGACY_PASSTHROUGH_INITIAL_HANDLER_NAME, new LegacyPassthroughInitialHandler());
        }

        super.initChannel(channel);
        channel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(new DefaultPacketRegistry(false, -1));

        final UserConnection user = new UserConnectionImpl(channel, false);
        new ProtocolPipelineImpl(user);
        channel.pipeline().addLast(new ViaProxyVLPipeline(user));
        channel.pipeline().addAfter(VLPipeline.VIA_CODEC_NAME, "via-" + MCPipeline.FLOW_CONTROL_HANDLER_NAME, new NoReadFlowControlHandler());

        if (ViaProxy.EVENT_MANAGER.call(new Client2ProxyChannelInitializeEvent(ITyped.Type.POST, channel, false)).isCancelled()) {
            channel.close();
        }
    }

}
