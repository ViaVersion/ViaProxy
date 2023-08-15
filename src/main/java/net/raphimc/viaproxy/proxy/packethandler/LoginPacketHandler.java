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
package net.raphimc.viaproxy.proxy.packethandler;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.crypto.AESEncryption;
import net.raphimc.netminecraft.netty.crypto.CryptUtil;
import net.raphimc.netminecraft.packet.IPacket;
import net.raphimc.netminecraft.packet.UnknownPacket;
import net.raphimc.netminecraft.packet.impl.login.*;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.storage.ProtocolMetadataStorage;
import net.raphimc.vialoader.util.VersionEnum;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.proxy.LoginState;
import net.raphimc.viaproxy.proxy.external_interface.AuthLibServices;
import net.raphimc.viaproxy.proxy.external_interface.ExternalInterface;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.proxy.util.CloseAndReturn;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class LoginPacketHandler extends PacketHandler {

    private static final KeyPair KEY_PAIR = CryptUtil.generateKeyPair();
    private static final Random RANDOM = new Random();

    private final byte[] verifyToken = new byte[4];
    private LoginState loginState = LoginState.FIRST_PACKET;

    private final int chatSessionUpdateId;

    public LoginPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);

        RANDOM.nextBytes(this.verifyToken);
        this.chatSessionUpdateId = MCPackets.C2S_CHAT_SESSION_UPDATE.getId(proxyConnection.getClientVersion().getVersion());
    }

    @Override
    public boolean handleC2P(IPacket packet, List<ChannelFutureListener> listeners) throws GeneralSecurityException {
        if (packet instanceof UnknownPacket && this.proxyConnection.getConnectionState() == ConnectionState.PLAY) {
            final UnknownPacket unknownPacket = (UnknownPacket) packet;
            if (unknownPacket.packetId == this.chatSessionUpdateId && this.proxyConnection.getChannel().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).get() == null) {
                return false;
            }
        } else if (packet instanceof C2SLoginHelloPacket1_7) {
            if (this.loginState != LoginState.FIRST_PACKET) throw CloseAndReturn.INSTANCE;
            this.loginState = LoginState.SENT_HELLO;
            final C2SLoginHelloPacket1_7 loginHelloPacket = (C2SLoginHelloPacket1_7) packet;

            if (packet instanceof C2SLoginHelloPacket1_19) {
                final C2SLoginHelloPacket1_19 packet1_19 = (C2SLoginHelloPacket1_19) packet;
                if (packet1_19.expiresAt != null && packet1_19.expiresAt.isBefore(Instant.now())) {
                    throw new IllegalStateException("Expired public key");
                }
            }

            proxyConnection.setLoginHelloPacket(loginHelloPacket);
            if (packet instanceof C2SLoginHelloPacket1_19_3) {
                proxyConnection.setGameProfile(new GameProfile(((C2SLoginHelloPacket1_19_3) packet).uuid, loginHelloPacket.name));
            } else {
                proxyConnection.setGameProfile(new GameProfile(null, loginHelloPacket.name));
            }

            if (Options.ONLINE_MODE) {
                this.proxyConnection.getC2P().writeAndFlush(new S2CLoginKeyPacket1_8("", KEY_PAIR.getPublic().getEncoded(), this.verifyToken)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            } else {
                ExternalInterface.fillPlayerData(this.proxyConnection);
                this.proxyConnection.getChannel().writeAndFlush(this.proxyConnection.getLoginHelloPacket()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            }

            return false;
        } else if (packet instanceof C2SLoginKeyPacket1_7) {
            if (this.loginState != LoginState.SENT_HELLO) throw CloseAndReturn.INSTANCE;
            this.loginState = LoginState.SENT_KEY;
            final C2SLoginKeyPacket1_7 loginKeyPacket = (C2SLoginKeyPacket1_7) packet;

            if (loginKeyPacket.encryptedNonce != null) {
                if (!Arrays.equals(this.verifyToken, CryptUtil.decryptData(KEY_PAIR.getPrivate(), loginKeyPacket.encryptedNonce))) {
                    Logger.u_err("auth", this.proxyConnection.getC2P().remoteAddress(), this.proxyConnection.getGameProfile(), "Invalid verify token");
                    this.proxyConnection.kickClient("§cInvalid verify token!");
                }
            } else {
                final C2SLoginKeyPacket1_19 keyPacket = (C2SLoginKeyPacket1_19) packet;
                final C2SLoginHelloPacket1_19 helloPacket = (C2SLoginHelloPacket1_19) this.proxyConnection.getLoginHelloPacket();
                if (helloPacket.key == null || !CryptUtil.verifySignedNonce(helloPacket.key, this.verifyToken, keyPacket.salt, keyPacket.signature)) {
                    Logger.u_err("auth", this.proxyConnection.getC2P().remoteAddress(), this.proxyConnection.getGameProfile(), "Invalid verify token");
                    this.proxyConnection.kickClient("§cInvalid verify token!");
                }
            }

            final SecretKey secretKey = CryptUtil.decryptSecretKey(KEY_PAIR.getPrivate(), loginKeyPacket.encryptedSecretKey);
            this.proxyConnection.getC2P().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(new AESEncryption(secretKey));

            final String userName = this.proxyConnection.getGameProfile().getName();

            try {
                final String serverHash = new BigInteger(CryptUtil.computeServerIdHash("", KEY_PAIR.getPublic(), secretKey)).toString(16);
                final GameProfile mojangProfile = AuthLibServices.SESSION_SERVICE.hasJoinedServer(this.proxyConnection.getGameProfile(), serverHash, null);
                if (mojangProfile == null) {
                    Logger.u_err("auth", this.proxyConnection.getC2P().remoteAddress(), this.proxyConnection.getGameProfile(), "Invalid session");
                    this.proxyConnection.kickClient("§cInvalid session! Please restart minecraft (and the launcher) and try again.");
                } else {
                    this.proxyConnection.setGameProfile(mojangProfile);
                }
                Logger.u_info("auth", this.proxyConnection.getC2P().remoteAddress(), this.proxyConnection.getGameProfile(), "Authenticated as " + this.proxyConnection.getGameProfile().getId().toString());
            } catch (Throwable e) {
                throw new RuntimeException("Failed to make session request for user '" + userName + "'!", e);
            }

            ExternalInterface.fillPlayerData(this.proxyConnection);
            this.proxyConnection.getChannel().writeAndFlush(this.proxyConnection.getLoginHelloPacket()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            return false;
        }

        return true;
    }

    @Override
    public boolean handleP2S(IPacket packet, List<ChannelFutureListener> listeners) throws GeneralSecurityException, ExecutionException, InterruptedException {
        if (packet instanceof S2CLoginKeyPacket1_7) {
            final S2CLoginKeyPacket1_7 loginKeyPacket = (S2CLoginKeyPacket1_7) packet;

            final PublicKey publicKey = CryptUtil.decodeRsaPublicKey(loginKeyPacket.publicKey);
            final SecretKey secretKey = CryptUtil.generateSecretKey();
            final String serverHash = new BigInteger(CryptUtil.computeServerIdHash(loginKeyPacket.serverId, publicKey, secretKey)).toString(16);

            boolean auth = true;
            if (this.proxyConnection.getServerVersion().isOlderThanOrEqualTo(VersionEnum.r1_6_4)) {
                auth = this.proxyConnection.getUserConnection().get(ProtocolMetadataStorage.class).authenticate;
            }
            if (auth) {
                ExternalInterface.joinServer(serverHash, this.proxyConnection);
            }

            final byte[] encryptedSecretKey = CryptUtil.encryptData(publicKey, secretKey.getEncoded());
            final byte[] encryptedNonce = CryptUtil.encryptData(publicKey, loginKeyPacket.nonce);

            final C2SLoginKeyPacket1_19_3 loginKey = new C2SLoginKeyPacket1_19_3(encryptedSecretKey, encryptedNonce);
            if (this.proxyConnection.getServerVersion().isNewerThanOrEqualTo(VersionEnum.r1_19) && this.proxyConnection.getLoginHelloPacket() instanceof C2SLoginHelloPacket1_19 && ((C2SLoginHelloPacket1_19) this.proxyConnection.getLoginHelloPacket()).key != null) {
                ExternalInterface.signNonce(loginKeyPacket.nonce, loginKey, this.proxyConnection);
            }
            this.proxyConnection.getChannel().writeAndFlush(loginKey).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            if (this.proxyConnection.getServerVersion().isNewerThanOrEqualTo(VersionEnum.r1_7_2tor1_7_5)) {
                this.proxyConnection.getChannel().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(new AESEncryption(secretKey));
            } else {
                this.proxyConnection.setKeyForPreNettyEncryption(secretKey);
            }

            return false;
        } else if (packet instanceof S2CLoginSuccessPacket1_7) {
            final S2CLoginSuccessPacket1_7 loginSuccessPacket = (S2CLoginSuccessPacket1_7) packet;

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

            final ConnectionState nextState = this.proxyConnection.getClientVersion().isNewerThanOrEqualTo(VersionEnum.r1_20_2) ? ConnectionState.CONFIGURATION : ConnectionState.PLAY;

            this.proxyConnection.setGameProfile(new GameProfile(loginSuccessPacket.uuid, loginSuccessPacket.name));
            Logger.u_info("session", this.proxyConnection.getC2P().remoteAddress(), this.proxyConnection.getGameProfile(), "Connected successfully! Switching to " + nextState + " state");

            this.proxyConnection.getChannel().config().setAutoRead(false);
            listeners.add(f -> {
                if (f.isSuccess() && nextState != ConnectionState.CONFIGURATION) {
                    this.proxyConnection.setConnectionState(nextState);
                    this.proxyConnection.getChannel().config().setAutoRead(true);
                }
            });
        } else if (packet instanceof S2CLoginCompressionPacket) {
            final S2CLoginCompressionPacket loginCompressionPacket = (S2CLoginCompressionPacket) packet;

            this.proxyConnection.getChannel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(loginCompressionPacket.compressionThreshold);
            return false;
        }

        return true;
    }

}
