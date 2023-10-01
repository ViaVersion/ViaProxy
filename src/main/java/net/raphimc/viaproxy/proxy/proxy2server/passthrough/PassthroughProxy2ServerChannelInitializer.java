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
package net.raphimc.viaproxy.proxy.proxy2server.passthrough;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.haproxy.HAProxyMessageEncoder;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.Proxy2ServerChannelInitializeEvent;
import net.raphimc.viaproxy.plugins.events.types.ITyped;
import net.raphimc.viaproxy.proxy.proxy2server.Proxy2ServerChannelInitializer;

import java.util.function.Supplier;

public class PassthroughProxy2ServerChannelInitializer extends Proxy2ServerChannelInitializer {

    public PassthroughProxy2ServerChannelInitializer(final Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(Channel channel) {
        if (PluginManager.EVENT_MANAGER.call(new Proxy2ServerChannelInitializeEvent(ITyped.Type.PRE, channel, true)).isCancelled()) {
            channel.close();
            return;
        }

        if (Options.PROXY_URL != null) {
            channel.pipeline().addLast(VIAPROXY_PROXY_HANDLER_NAME, this.getProxyHandler());
        }
        if (Options.SERVER_HAPROXY_PROTOCOL) {
            channel.pipeline().addLast(VIAPROXY_HAPROXY_ENCODER_NAME, HAProxyMessageEncoder.INSTANCE);
        }

        channel.pipeline().addLast(MCPipeline.HANDLER_HANDLER_NAME, this.handlerSupplier.get());

        if (PluginManager.EVENT_MANAGER.call(new Proxy2ServerChannelInitializeEvent(ITyped.Type.POST, channel, true)).isCancelled()) {
            channel.close();
        }
    }

}
