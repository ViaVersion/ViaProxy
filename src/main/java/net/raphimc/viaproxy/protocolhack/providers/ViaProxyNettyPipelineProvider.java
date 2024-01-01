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
package net.raphimc.viaproxy.protocolhack.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.channel.Channel;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.viabedrock.netty.AesEncryption;
import net.raphimc.viabedrock.netty.SnappyCompression;
import net.raphimc.viabedrock.netty.ZLibCompression;
import net.raphimc.viabedrock.protocol.providers.NettyPipelineProvider;
import net.raphimc.vialoader.netty.VLPipeline;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import javax.crypto.SecretKey;

public class ViaProxyNettyPipelineProvider extends NettyPipelineProvider {

    @Override
    public void enableCompression(UserConnection user, int threshold, int algorithm) {
        final ProxyConnection proxyConnection = ProxyConnection.fromUserConnection(user);
        final Channel channel = proxyConnection.getChannel();

        try {
            channel.pipeline().remove(MCPipeline.COMPRESSION_HANDLER_NAME);
            switch (algorithm) {
                case 0:
                    channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, MCPipeline.COMPRESSION_HANDLER_NAME, new ZLibCompression());
                    break;
                case 1:
                    channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, MCPipeline.COMPRESSION_HANDLER_NAME, new SnappyCompression());
                    break;
                default:
                    throw new IllegalStateException("Invalid compression algorithm: " + algorithm);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void enableEncryption(UserConnection user, SecretKey key) {
        final ProxyConnection proxyConnection = ProxyConnection.fromUserConnection(user);
        final Channel channel = proxyConnection.getChannel();

        try {
            channel.pipeline().remove(MCPipeline.ENCRYPTION_HANDLER_NAME);
            channel.pipeline().addAfter(VLPipeline.VIABEDROCK_FRAME_ENCAPSULATION_HANDLER_NAME, MCPipeline.ENCRYPTION_HANDLER_NAME, new AesEncryption(key));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
