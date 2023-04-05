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

import java.util.function.Supplier;

public class Client2ProxyChannelInitializer extends MinecraftChannelInitializer {

    public Client2ProxyChannelInitializer(final Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(Channel channel) {
        if (PluginManager.EVENT_MANAGER.call(new Client2ProxyChannelInitializeEvent(ITyped.Type.PRE, channel)).isCancelled()) {
            channel.close();
            return;
        }

        if (Options.LEGACY_CLIENT_PASSTHROUGH) {
            channel.pipeline().addLast("legacy-passthrough-handler", new LegacyClientPassthroughHandler());
        }
        super.initChannel(channel);
        channel.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getHandshakeRegistry(false));

        if (PluginManager.EVENT_MANAGER.call(new Client2ProxyChannelInitializeEvent(ITyped.Type.POST, channel)).isCancelled()) {
            channel.close();
        }
    }

}
