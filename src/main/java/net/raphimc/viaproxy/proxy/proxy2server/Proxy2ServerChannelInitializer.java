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
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.MinecraftChannelInitializer;
import net.raphimc.netminecraft.packet.registry.PacketRegistryUtil;
import net.raphimc.vialegacy.netty.PreNettyDecoder;
import net.raphimc.vialegacy.netty.PreNettyEncoder;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.baseprotocols.PreNettyBaseProtocol;
import net.raphimc.viaprotocolhack.netty.ViaEncodeHandler;
import net.raphimc.viaprotocolhack.netty.ViaPipeline;
import net.raphimc.viaprotocolhack.util.VersionEnum;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.Proxy2ServerChannelInitializeEvent;
import net.raphimc.viaproxy.plugins.events.types.ITyped;
import net.raphimc.viaproxy.protocolhack.impl.ViaProxyViaDecodeHandler;
import net.raphimc.viaproxy.proxy.ProxyConnection;

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
        channel.pipeline().addBefore(MCPipeline.PACKET_CODEC_HANDLER_NAME, ViaPipeline.HANDLER_ENCODER_NAME, new ViaEncodeHandler(user));
        channel.pipeline().addBefore(MCPipeline.PACKET_CODEC_HANDLER_NAME, ViaPipeline.HANDLER_DECODER_NAME, new ViaProxyViaDecodeHandler(user));

        if (ProxyConnection.fromChannel(channel).getServerVersion().isOlderThanOrEqualTo(VersionEnum.r1_6_4)) {
            user.getProtocolInfo().getPipeline().add(PreNettyBaseProtocol.INSTANCE);
            channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, ViaPipeline.HANDLER_PRE_NETTY_ENCODER_NAME, new PreNettyEncoder(user));
            channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, ViaPipeline.HANDLER_PRE_NETTY_DECODER_NAME, new PreNettyDecoder(user));
        }

        if (PluginManager.EVENT_MANAGER.call(new Proxy2ServerChannelInitializeEvent(ITyped.Type.POST, channel)).isCancelled()) {
            channel.close();
        }
    }

}
