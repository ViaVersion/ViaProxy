/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2025 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.proxy.external_interface;

import com.google.common.primitives.Longs;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ProfileKey;
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_0;
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_1;
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_3;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.lenni0451.commons.httpclient.proxy.SingleProxyAuthenticator;
import net.raphimc.minecraftauth.bedrock.model.MinecraftCertificateChain;
import net.raphimc.minecraftauth.bedrock.model.MinecraftMultiplayerToken;
import net.raphimc.minecraftauth.java.model.MinecraftPlayerCertificates;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginKeyPacket;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.protocol.storage.AuthData;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.events.FillPlayerDataEvent;
import net.raphimc.viaproxy.plugins.events.JoinServerRequestEvent;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.proxy.util.CloseAndReturn;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount;
import net.raphimc.viaproxy.util.Proxy;
import net.raphimc.viaproxy.util.logging.Logger;

import java.net.Authenticator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ExternalInterface {

    public static void fillPlayerData(final ProxyConnection proxyConnection) {
        Logger.u_info("auth", proxyConnection, "Filling player data");
        try {
            if (proxyConnection.getUserOptions().account() != null) {
                final Account account = proxyConnection.getUserOptions().account();

                proxyConnection.setGameProfile(account.getGameProfile());
                final UserConnection user = proxyConnection.getUserConnection();

                if (ViaProxy.getConfig().shouldSignChat() && proxyConnection.getServerVersion().newerThanOrEqualTo(ProtocolVersion.v1_19) && account instanceof MicrosoftAccount microsoftAccount) {
                    final MinecraftPlayerCertificates playerCertificates = microsoftAccount.getAuthManager().getMinecraftPlayerCertificates().getUpToDate();
                    final Instant expiresAt = Instant.ofEpochMilli(playerCertificates.getExpireTimeMs());
                    final long expiresAtMillis = playerCertificates.getExpireTimeMs();
                    final PublicKey publicKey = playerCertificates.getKeyPair().getPublic();
                    final byte[] publicKeyBytes = publicKey.getEncoded();
                    final byte[] keySignature = playerCertificates.getPublicKeySignature();
                    final PrivateKey privateKey = playerCertificates.getKeyPair().getPrivate();
                    final UUID uuid = proxyConnection.getGameProfile().getId();

                    byte[] loginHelloKeySignature = keySignature;
                    if (proxyConnection.getClientVersion().equals(ProtocolVersion.v1_19)) {
                        loginHelloKeySignature = playerCertificates.getLegacyPublicKeySignature();
                    }
                    proxyConnection.setLoginHelloPacket(new C2SLoginHelloPacket(proxyConnection.getGameProfile().getName(), expiresAt, publicKey, loginHelloKeySignature, proxyConnection.getGameProfile().getId()));

                    user.put(new ChatSession1_19_0(uuid, privateKey, new ProfileKey(expiresAtMillis, publicKeyBytes, playerCertificates.getLegacyPublicKeySignature())));
                    user.put(new ChatSession1_19_1(uuid, privateKey, new ProfileKey(expiresAtMillis, publicKeyBytes, keySignature)));
                    user.put(new ChatSession1_19_3(uuid, privateKey, new ProfileKey(expiresAtMillis, publicKeyBytes, keySignature)));
                } else if (proxyConnection.getServerVersion().equals(BedrockProtocolVersion.bedrockLatest) && account instanceof BedrockAccount bedrockAccount) {
                    final MinecraftMultiplayerToken multiplayerToken = bedrockAccount.getAuthManager().getMinecraftMultiplayerToken().refresh();
                    final MinecraftCertificateChain certificateChain = bedrockAccount.getAuthManager().getMinecraftCertificateChain().refresh();
                    user.put(new AuthData(certificateChain.getMojangJwt(), certificateChain.getIdentityJwt(), multiplayerToken.getToken(), bedrockAccount.getAuthManager().getSessionKeyPair(), bedrockAccount.getAuthManager().getDeviceId()));
                }
            }

            ViaProxy.EVENT_MANAGER.call(new FillPlayerDataEvent(proxyConnection));
        } catch (CloseAndReturn e) {
            throw e;
        } catch (Throwable e) {
            Logger.LOGGER.error("Failed to fill player data", e);
            proxyConnection.kickClient("§cFailed to fill player data. This might be caused by outdated account tokens or rate limits. Wait a couple of seconds and try again. If the problem persists, remove and re-add your account.");
        }

        proxyConnection.getLoginHelloPacket().name = proxyConnection.getGameProfile().getName();
        proxyConnection.getLoginHelloPacket().uuid = proxyConnection.getGameProfile().getId();
    }

    public static void joinServer(final String serverIdHash, final ProxyConnection proxyConnection) {
        Logger.u_info("auth", proxyConnection, "Trying to join online mode server");
        try {
            if (proxyConnection.getUserOptions().account() instanceof MicrosoftAccount microsoftAccount) {
                try {
                    if (ViaProxy.getConfig().getBackendProxy() == null) {
                        AuthLibServices.SESSION_SERVICE.joinServer(microsoftAccount.getGameProfile().getId(), microsoftAccount.getAuthManager().getMinecraftToken().getUpToDate().getToken(), serverIdHash);
                    } else {
                        final Proxy proxy = ViaProxy.getConfig().getBackendProxy();
                        final MinecraftSessionService sessionService = new YggdrasilAuthenticationService(proxy.toJavaProxy()).createMinecraftSessionService();
                        Authenticator prevAuthenticator = Authenticator.getDefault();
                        try {
                            if (proxy.getUsername() != null && proxy.getPassword() != null) {
                                Authenticator.setDefault(new SingleProxyAuthenticator(proxy.getUsername(), proxy.getPassword()));
                            }
                            sessionService.joinServer(microsoftAccount.getGameProfile().getId(), microsoftAccount.getAuthManager().getMinecraftToken().getUpToDate().getToken(), serverIdHash);
                        } finally {
                            Authenticator.setDefault(prevAuthenticator);
                        }
                    }
                } catch (Throwable e) {
                    proxyConnection.kickClient("§cFailed to authenticate with Mojang servers! Please try again in a couple of seconds.");
                }
            } else if (!ViaProxy.EVENT_MANAGER.call(new JoinServerRequestEvent(proxyConnection, serverIdHash)).isCancelled()) {
                proxyConnection.kickClient("§cThe configured target server is in online mode and requires a valid authentication mode.");
            }
        } catch (CloseAndReturn e) {
            throw e;
        } catch (Throwable e) {
            Logger.LOGGER.error("Failed to join online mode server", e);
            proxyConnection.kickClient("§cFailed to join online mode server. See console for more information.");
        }
    }

    public static void signNonce(final byte[] nonce, final C2SLoginKeyPacket packet, final ProxyConnection proxyConnection) throws SignatureException {
        Logger.u_info("auth", proxyConnection, "Requesting nonce signature");
        final UserConnection user = proxyConnection.getUserConnection();

        if (user.has(ChatSession1_19_0.class)) {
            final long salt = ThreadLocalRandom.current().nextLong();
            packet.signature = user.get(ChatSession1_19_0.class).sign(updater -> {
                updater.accept(nonce);
                updater.accept(Longs.toByteArray(salt));
            });
            packet.salt = salt;
        } else {
            proxyConnection.kickClient("§cThe configured target server requires a signed nonce. Please enable chat signing in the config and select a valid authentication mode.");
        }
    }

}
