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
package net.raphimc.viaproxy.proxy.packethandler;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.crypto.AESEncryption;
import net.raphimc.netminecraft.netty.crypto.CryptUtil;
import net.raphimc.netminecraft.packet.Packet;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginKeyPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginGameProfilePacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginHelloPacket;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.api.util.UuidUtil;
import net.raphimc.vialegacy.protocol.release.r1_6_4tor1_7_2_5.storage.ProtocolMetadataStorage;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.events.ClientLoggedInEvent;
import net.raphimc.viaproxy.plugins.events.ShouldVerifyOnlineModeEvent;
import net.raphimc.viaproxy.proxy.LoginState;
import net.raphimc.viaproxy.proxy.external_interface.AuthLibServices;
import net.raphimc.viaproxy.proxy.external_interface.ExternalInterface;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.proxy.util.ChannelUtil;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginPacketHandler extends PacketHandler {

    private static final KeyPair KEY_PAIR = CryptUtil.generateKeyPair();
    private static final Random RANDOM = new Random();
    private static final ExecutorService HTTP_EXECUTOR = Executors.newWorkStealingPool(4);

    private final byte[] verifyToken = new byte[4];
    private LoginState loginState = LoginState.FIRST_PACKET;

    public LoginPacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);

        RANDOM.nextBytes(this.verifyToken);
    }

    @Override
    public boolean handleC2P(Packet packet, List<ChannelFutureListener> listeners) throws GeneralSecurityException {
        if (packet instanceof C2SLoginHelloPacket loginHelloPacket) {
            if (this.loginState != LoginState.FIRST_PACKET) throw CloseAndReturn.INSTANCE;
            this.loginState = LoginState.SENT_HELLO;

            if (loginHelloPacket.expiresAt != null && loginHelloPacket.expiresAt.isBefore(Instant.now())) {
                throw new IllegalStateException("Expired public key");
            }

            proxyConnection.setLoginHelloPacket(loginHelloPacket);
            if (loginHelloPacket.uuid != null) {
                proxyConnection.setGameProfile(new GameProfile(loginHelloPacket.uuid, loginHelloPacket.name));
            } else {
                proxyConnection.setGameProfile(new GameProfile(UuidUtil.createOfflinePlayerUuid(loginHelloPacket.name), loginHelloPacket.name));
            }

            if (ViaProxy.getConfig().isProxyOnlineMode() && !ViaProxy.EVENT_MANAGER.call(new ShouldVerifyOnlineModeEvent(this.proxyConnection)).isCancelled()) {
                this.proxyConnection.getC2P().writeAndFlush(new S2CLoginHelloPacket("", KEY_PAIR.getPublic().getEncoded(), this.verifyToken, true)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            } else {
                ViaProxy.EVENT_MANAGER.call(new ClientLoggedInEvent(proxyConnection));
                ExternalInterface.fillPlayerData(this.proxyConnection);
                this.proxyConnection.getChannel().writeAndFlush(this.proxyConnection.getLoginHelloPacket()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            }

            return false;
        } else if (packet instanceof C2SLoginKeyPacket loginKeyPacket) {
            if (this.loginState != LoginState.SENT_HELLO) throw CloseAndReturn.INSTANCE;
            this.loginState = LoginState.SENT_KEY;

            if (loginKeyPacket.encryptedNonce != null) {
                if (!Arrays.equals(this.verifyToken, CryptUtil.decryptData(KEY_PAIR.getPrivate(), loginKeyPacket.encryptedNonce))) {
                    Logger.u_err("auth", this.proxyConnection, "Invalid verify token");
                    this.proxyConnection.kickClient("§cInvalid verify token!");
                }
            } else {
                final C2SLoginHelloPacket loginHelloPacket = this.proxyConnection.getLoginHelloPacket();
                if (loginHelloPacket.key == null || !CryptUtil.verifySignedNonce(loginHelloPacket.key, this.verifyToken, loginKeyPacket.salt, loginKeyPacket.signature)) {
                    Logger.u_err("auth", this.proxyConnection, "Invalid verify token");
                    this.proxyConnection.kickClient("§cInvalid verify token!");
                }
            }

            final SecretKey secretKey = CryptUtil.decryptSecretKey(KEY_PAIR.getPrivate(), loginKeyPacket.encryptedSecretKey);
            this.proxyConnection.getC2P().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(new AESEncryption(secretKey));

            HTTP_EXECUTOR.submit(() -> {
                final String userName = this.proxyConnection.getGameProfile().getName();
                try {
                    final String serverHash = new BigInteger(CryptUtil.computeServerIdHash("", KEY_PAIR.getPublic(), secretKey)).toString(16);
                    final GameProfile mojangProfile = AuthLibServices.SESSION_SERVICE.hasJoinedServer(this.proxyConnection.getGameProfile(), serverHash, null);
                    if (mojangProfile == null) {
                        Logger.u_err("auth", this.proxyConnection, "Invalid session");
                        this.proxyConnection.kickClient("§cInvalid session! Please restart minecraft (and the launcher) and try again.");
                    } else {
                        this.proxyConnection.setGameProfile(mojangProfile);
                    }
                    Logger.u_info("auth", this.proxyConnection, "Authenticated as " + this.proxyConnection.getGameProfile().getId().toString());
                } catch (CloseAndReturn ignored) {
                } catch (Throwable e) {
                    Logger.LOGGER.error("Failed to make session request for user '" + userName + "'!", e);
                    try {
                        this.proxyConnection.kickClient("§cFailed to authenticate with Mojang servers! Please try again later.");
                    } catch (Throwable ignored) {
                    }
                }

                this.proxyConnection.getC2P().eventLoop().execute(() -> {
                    ViaProxy.EVENT_MANAGER.call(new ClientLoggedInEvent(proxyConnection));
                    ExternalInterface.fillPlayerData(this.proxyConnection);
                    this.proxyConnection.getChannel().writeAndFlush(this.proxyConnection.getLoginHelloPacket()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                });
            });
            return false;
        }

        return true;
    }

    @Override
    public boolean handleP2S(Packet packet, List<ChannelFutureListener> listeners) throws GeneralSecurityException, ExecutionException, InterruptedException {
        if (packet instanceof S2CLoginHelloPacket loginHelloPacket) {
            final PublicKey publicKey = CryptUtil.decodeRsaPublicKey(loginHelloPacket.publicKey);
            final SecretKey secretKey = CryptUtil.generateSecretKey();
            final String serverHash = new BigInteger(CryptUtil.computeServerIdHash(loginHelloPacket.serverId, publicKey, secretKey)).toString(16);

            boolean auth = this.proxyConnection.getClientVersion().olderThan(ProtocolVersion.v1_20_5) || loginHelloPacket.authenticate;
            if (auth && this.proxyConnection.getServerVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_6_4)) {
                auth = this.proxyConnection.getUserConnection().get(ProtocolMetadataStorage.class).authenticate;
            }
            if (auth) {
                ExternalInterface.joinServer(serverHash, this.proxyConnection);
            }

            final byte[] encryptedSecretKey = CryptUtil.encryptData(publicKey, secretKey.getEncoded());
            final byte[] encryptedNonce = CryptUtil.encryptData(publicKey, loginHelloPacket.nonce);

            final C2SLoginKeyPacket loginKey = new C2SLoginKeyPacket(encryptedSecretKey, encryptedNonce);
            if (this.proxyConnection.getServerVersion().betweenInclusive(ProtocolVersion.v1_19, ProtocolVersion.v1_19_1) && this.proxyConnection.getLoginHelloPacket().key != null) {
                ExternalInterface.signNonce(loginHelloPacket.nonce, loginKey, this.proxyConnection);
            }
            this.proxyConnection.getChannel().writeAndFlush(loginKey).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            if (this.proxyConnection.getServerVersion().newerThanOrEqualTo(ProtocolVersion.v1_7_2)) {
                this.proxyConnection.getChannel().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(new AESEncryption(secretKey));
            } else {
                this.proxyConnection.setKeyForPreNettyEncryption(secretKey);
            }

            return false;
        } else if (packet instanceof S2CLoginGameProfilePacket loginGameProfilePacket) {
            final ConnectionState nextState = this.proxyConnection.getClientVersion().newerThanOrEqualTo(ProtocolVersion.v1_20_2) ? ConnectionState.CONFIGURATION : ConnectionState.PLAY;

            this.proxyConnection.setGameProfile(new GameProfile(loginGameProfilePacket.uuid, loginGameProfilePacket.name));
            Logger.u_info("session", this.proxyConnection, "Connected successfully! Switching to " + nextState + " state");

            ChannelUtil.disableAutoRead(this.proxyConnection.getChannel());
            listeners.add(f -> {
                if (f.isSuccess() && nextState != ConnectionState.CONFIGURATION) {
                    this.proxyConnection.setC2pConnectionState(nextState);
                    this.proxyConnection.setP2sConnectionState(nextState);
                    ChannelUtil.restoreAutoRead(this.proxyConnection.getChannel());
                }
            });
        }

        return true;
    }

}
