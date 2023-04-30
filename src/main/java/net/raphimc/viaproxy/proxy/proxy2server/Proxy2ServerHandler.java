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

import com.mojang.authlib.GameProfile;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.libs.gson.JsonElement;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.crypto.AESEncryption;
import net.raphimc.netminecraft.netty.crypto.CryptUtil;
import net.raphimc.netminecraft.packet.IPacket;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.UnknownPacket;
import net.raphimc.netminecraft.packet.impl.login.*;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.storage.ProtocolMetadataStorage;
import net.raphimc.viaprotocolhack.util.VersionEnum;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.proxy.external_interface.ExternalInterface;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.proxy.util.ExceptionUtil;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Proxy2ServerHandler extends SimpleChannelInboundHandler<IPacket> {

    private ProxyConnection proxyConnection;

    private int joinGamePacketId = -1;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);

        this.proxyConnection = ProxyConnection.fromChannel(ctx.channel());

        this.joinGamePacketId = MCPackets.S2C_JOIN_GAME.getId(this.proxyConnection.getClientVersion().getVersion());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        Logger.u_info("disconnect", this.proxyConnection.getC2P().remoteAddress(), this.proxyConnection.getGameProfile(), "Connection closed");
        try {
            this.proxyConnection.getC2P().close();
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IPacket packet) throws Exception {
        if (this.proxyConnection.isClosed()) return;

        switch (this.proxyConnection.getConnectionState()) {
            case LOGIN:
                if (packet instanceof S2CLoginKeyPacket1_7) this.handleLoginKey((S2CLoginKeyPacket1_7) packet);
                else if (packet instanceof S2CLoginSuccessPacket1_7) this.handleLoginSuccess((S2CLoginSuccessPacket1_7) packet);
                else if (packet instanceof S2CLoginCompressionPacket) this.handleLoginCompression((S2CLoginCompressionPacket) packet);
                else break;

                return;
            case PLAY:
                final UnknownPacket unknownPacket = (UnknownPacket) packet;
                if (unknownPacket.packetId == this.joinGamePacketId) {
                    this.proxyConnection.getC2P().writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    this.sendResourcePack();
                    return;
                }
                break;
        }

        this.proxyConnection.getC2P().writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ExceptionUtil.handleNettyException(ctx, cause, this.proxyConnection);
    }

    private void handleLoginKey(final S2CLoginKeyPacket1_7 packet) throws InterruptedException, GeneralSecurityException, ExecutionException {
        final PublicKey publicKey = CryptUtil.decodeRsaPublicKey(packet.publicKey);
        final SecretKey secretKey = CryptUtil.generateSecretKey();
        final String serverHash = new BigInteger(CryptUtil.computeServerIdHash(packet.serverId, publicKey, secretKey)).toString(16);

        boolean auth = true;
        if (this.proxyConnection.getServerVersion().isOlderThanOrEqualTo(VersionEnum.r1_6_4)) {
            auth = this.proxyConnection.getUserConnection().get(ProtocolMetadataStorage.class).authenticate;
        }
        if (auth) {
            ExternalInterface.joinServer(serverHash, this.proxyConnection);
        }

        final byte[] encryptedSecretKey = CryptUtil.encryptData(publicKey, secretKey.getEncoded());
        final byte[] encryptedNonce = CryptUtil.encryptData(publicKey, packet.nonce);

        final C2SLoginKeyPacket1_19_3 loginKey = new C2SLoginKeyPacket1_19_3(encryptedSecretKey, encryptedNonce);
        if (this.proxyConnection.getServerVersion().isNewerThanOrEqualTo(VersionEnum.r1_19) && this.proxyConnection.getLoginHelloPacket() instanceof C2SLoginHelloPacket1_19 && ((C2SLoginHelloPacket1_19) this.proxyConnection.getLoginHelloPacket()).key != null) {
            ExternalInterface.signNonce(packet.nonce, loginKey, this.proxyConnection);
        }
        this.proxyConnection.getChannel().writeAndFlush(loginKey).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

        if (this.proxyConnection.getServerVersion().isNewerThanOrEqualTo(VersionEnum.r1_7_2tor1_7_5)) {
            this.proxyConnection.getChannel().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(new AESEncryption(secretKey));
        } else {
            this.proxyConnection.setKeyForPreNettyEncryption(secretKey);
        }
    }

    private void handleLoginSuccess(final S2CLoginSuccessPacket1_7 packet) {
        if (this.proxyConnection.getClientVersion().isNewerThanOrEqualTo(VersionEnum.r1_8)) {
            if (Options.COMPRESSION_THRESHOLD > -1 && this.proxyConnection.getC2P().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).get() == -1) {
                this.proxyConnection.getChannel().config().setAutoRead(false);
                this.proxyConnection.getC2P().writeAndFlush(new S2CLoginCompressionPacket(Options.COMPRESSION_THRESHOLD)).addListeners(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE, (ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        this.proxyConnection.getC2P().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(Options.COMPRESSION_THRESHOLD);
                        this.proxyConnection.getChannel().config().setAutoRead(true);
                    }
                });
            }
        }

        this.proxyConnection.setGameProfile(new GameProfile(packet.uuid, packet.name));
        Logger.u_info("connect", this.proxyConnection.getC2P().remoteAddress(), this.proxyConnection.getGameProfile(), "Connected successfully! Switching to PLAY state");

        this.proxyConnection.getChannel().config().setAutoRead(false);
        this.proxyConnection.getC2P().writeAndFlush(packet).addListeners(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE, (ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                this.proxyConnection.setConnectionState(ConnectionState.PLAY);
                this.proxyConnection.getChannel().config().setAutoRead(true);
            }
        });
    }

    private void handleLoginCompression(final S2CLoginCompressionPacket packet) {
        this.proxyConnection.getChannel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(packet.compressionThreshold);
    }

    private void sendResourcePack() {
        if (Options.RESOURCE_PACK_URL != null) {
            this.proxyConnection.getChannel().eventLoop().schedule(() -> {
                if (this.proxyConnection.getClientVersion().isNewerThanOrEqualTo(VersionEnum.r1_8)) {
                    final ByteBuf resourcePackPacket = Unpooled.buffer();
                    PacketTypes.writeVarInt(resourcePackPacket, MCPackets.S2C_RESOURCE_PACK.getId(this.proxyConnection.getClientVersion().getVersion()));
                    PacketTypes.writeString(resourcePackPacket, Options.RESOURCE_PACK_URL); // url
                    PacketTypes.writeString(resourcePackPacket, ""); // hash
                    if (this.proxyConnection.getClientVersion().isNewerThanOrEqualTo(VersionEnum.r1_17)) {
                        resourcePackPacket.writeBoolean(Via.getConfig().isForcedUse1_17ResourcePack()); // required
                        final JsonElement promptMessage = Via.getConfig().get1_17ResourcePackPrompt();
                        if (promptMessage != null) {
                            resourcePackPacket.writeBoolean(true); // has message
                            PacketTypes.writeString(resourcePackPacket, promptMessage.toString()); // message
                        } else {
                            resourcePackPacket.writeBoolean(false); // has message
                        }
                    }
                    this.proxyConnection.getC2P().writeAndFlush(resourcePackPacket).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                } else if (this.proxyConnection.getClientVersion().isNewerThanOrEqualTo(VersionEnum.r1_7_2tor1_7_5)) {
                    final byte[] data = Options.RESOURCE_PACK_URL.getBytes(StandardCharsets.UTF_8);

                    final ByteBuf customPayloadPacket = Unpooled.buffer();
                    PacketTypes.writeVarInt(customPayloadPacket, MCPackets.S2C_PLUGIN_MESSAGE.getId(this.proxyConnection.getClientVersion().getVersion()));
                    PacketTypes.writeString(customPayloadPacket, "MC|RPack"); // channel
                    customPayloadPacket.writeShort(data.length); // length
                    customPayloadPacket.writeBytes(data); // data
                    this.proxyConnection.getC2P().writeAndFlush(customPayloadPacket).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                }
            }, 250, TimeUnit.MILLISECONDS);
        }
    }

}
