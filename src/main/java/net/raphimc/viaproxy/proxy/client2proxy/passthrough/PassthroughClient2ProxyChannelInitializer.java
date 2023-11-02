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
package net.raphimc.viaproxy.proxy.client2proxy.passthrough;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.events.Client2ProxyChannelInitializeEvent;
import net.raphimc.viaproxy.plugins.events.types.ITyped;
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyChannelInitializer;

import java.util.function.Supplier;

public class PassthroughClient2ProxyChannelInitializer extends Client2ProxyChannelInitializer {

    public PassthroughClient2ProxyChannelInitializer(final Supplier<ChannelHandler> handlerSupplier) {
        super(handlerSupplier);
    }

    @Override
    protected void initChannel(Channel channel) {
        if (ViaProxy.EVENT_MANAGER.call(new Client2ProxyChannelInitializeEvent(ITyped.Type.PRE, channel, true)).isCancelled()) {
            channel.close();
            return;
        }

        channel.pipeline().addLast(MCPipeline.FLOW_CONTROL_HANDLER_NAME, MCPipeline.FLOW_CONTROL_HANDLER.get());
        channel.pipeline().addLast(MCPipeline.HANDLER_HANDLER_NAME, this.handlerSupplier.get());

        if (ViaProxy.EVENT_MANAGER.call(new Client2ProxyChannelInitializeEvent(ITyped.Type.POST, channel, true)).isCancelled()) {
            channel.close();
        }
    }

}
