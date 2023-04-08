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
package net.raphimc.viaproxy.proxy.client2proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;
import net.raphimc.netminecraft.packet.registry.PacketRegistryUtil;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.Client2ProxyChannelInitializeEvent;
import net.raphimc.viaproxy.plugins.events.types.ITyped;
import net.raphimc.viaproxy.proxy.client2proxy.eaglercraft.EaglercraftInitialHandler;
import net.raphimc.viaproxy.proxy.client2proxy.passthrough.LegacyClientPassthroughHandler;

import java.util.function.Supplier;

public class Client2ProxyChannelInitializer extends MinecraftChannelInitializer {

    public static final String EAGLERCRAFT_INITIAL_HANDLER_NAME = "eaglercraft-initial-handler";
    public static final String WEBSOCKET_SSL_HANDLER_NAME = "ws-ssl-handler";
    public static final String WEBSOCKET_HTTP_CODEC_NAME = "ws-http-codec";
    public static final String WEBSOCKET_HTTP_AGGREGATOR_NAME = "ws-http-aggregator";
    public static final String WEBSOCKET_COMPRESSION_NAME = "ws-compression";
    public static final String WEBSOCKET_HANDLER_NAME = "ws-handler";
    public static final String WEBSOCKET_ACTIVE_NOTIFIER_NAME = "ws-active-notifier";
    public static final String EAGLERCRAFT_HANDLER_NAME = "eaglercraft-handler";

    public static final String LEGACY_PASSTHROUGH_HANDLER_NAME = "legacy-passthrough-handler";

    public Client2ProxyChannelInitializer(final Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(Channel channel) {
        if (PluginManager.EVENT_MANAGER.call(new Client2ProxyChannelInitializeEvent(ITyped.Type.PRE, channel)).isCancelled()) {
            channel.close();
            return;
        }

        if (Options.ALLOW_EAGLERCRAFT_CLIENTS) {
            channel.pipeline().addLast(EAGLERCRAFT_INITIAL_HANDLER_NAME, new EaglercraftInitialHandler());
        }
        if (Options.LEGACY_CLIENT_PASSTHROUGH) {
            channel.pipeline().addLast(LEGACY_PASSTHROUGH_HANDLER_NAME, new LegacyClientPassthroughHandler());
        }

        super.initChannel(channel);
        channel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getHandshakeRegistry(false));

        if (PluginManager.EVENT_MANAGER.call(new Client2ProxyChannelInitializeEvent(ITyped.Type.POST, channel)).isCancelled()) {
            channel.close();
        }
    }

}
