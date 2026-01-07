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
package net.raphimc.viaproxy.protocoltranslator.providers;

import com.viaversion.vialoader.netty.VLPipeline;
import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.channel.Channel;
import net.lenni0451.commons.unchecked.Sneaky;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.viabedrock.api.io.compression.ProtocolCompression;
import net.raphimc.viabedrock.netty.CompressionCodec;
import net.raphimc.viabedrock.netty.raknet.AesEncryptionCodec;
import net.raphimc.viabedrock.protocol.provider.NettyPipelineProvider;
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

        channel.pipeline().addBefore(MCPipeline.SIZER_HANDLER_NAME, MCPipeline.COMPRESSION_HANDLER_NAME, new CompressionCodec(protocolCompression));
    }

    @Override
    public void enableEncryption(UserConnection user, SecretKey key) {
        final ProxyConnection proxyConnection = ProxyConnection.fromUserConnection(user);
        final Channel channel = proxyConnection.getChannel();

        if (channel.pipeline().names().contains(MCPipeline.ENCRYPTION_HANDLER_NAME)) {
            throw new IllegalStateException("Encryption already enabled");
        }

        if (channel.pipeline().get(VLPipeline.VIABEDROCK_RAKNET_MESSAGE_CODEC_NAME) != null) { // Only enable encryption for RakNet connections
            try {
                channel.pipeline().addAfter(VLPipeline.VIABEDROCK_RAKNET_MESSAGE_CODEC_NAME, MCPipeline.ENCRYPTION_HANDLER_NAME, new AesEncryptionCodec(key));
            } catch (Throwable e) {
                Sneaky.sneakyThrow(e);
            }
        }
    }

}
