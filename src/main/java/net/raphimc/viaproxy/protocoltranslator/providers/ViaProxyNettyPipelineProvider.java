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
package net.raphimc.viaproxy.protocoltranslator.providers;

import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.channel.Channel;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.viabedrock.api.io.compression.ProtocolCompression;
import net.raphimc.viabedrock.netty.AesEncryptionCodec;
import net.raphimc.viabedrock.netty.CompressionCodec;
import net.raphimc.viabedrock.protocol.providers.NettyPipelineProvider;
import net.raphimc.vialoader.netty.VLPipeline;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import javax.crypto.SecretKey;

public class ViaProxyNettyPipelineProvider extends NettyPipelineProvider {

    @Override
    public void enableCompression(UserConnection user, ProtocolCompression protocolCompression) {
        final ProxyConnection proxyConnection = ProxyConnection.fromUserConnection(user);
        final Channel channel = proxyConnection.getChannel();

        if (channel.pipeline().names().contains(MCPipeline.COMPRESSION_HANDLER_NAME)) {
            throw new IllegalStateException("Compression already enabled");
        }

        try {
            channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, MCPipeline.COMPRESSION_HANDLER_NAME, new CompressionCodec(protocolCompression));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void enableEncryption(UserConnection user, SecretKey key) {
        final ProxyConnection proxyConnection = ProxyConnection.fromUserConnection(user);
        final Channel channel = proxyConnection.getChannel();

        if (channel.pipeline().names().contains(MCPipeline.ENCRYPTION_HANDLER_NAME)) {
            throw new IllegalStateException("Encryption already enabled");
        }

        try {
            channel.pipeline().addAfter(VLPipeline.VIABEDROCK_FRAME_ENCAPSULATION_HANDLER_NAME, MCPipeline.ENCRYPTION_HANDLER_NAME, new AesEncryptionCodec(key));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
