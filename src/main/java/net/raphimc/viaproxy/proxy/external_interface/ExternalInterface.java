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
package net.raphimc.viaproxy.proxy.external_interface;

import com.google.common.primitives.Longs;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ProfileKey;
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_0;
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_1;
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_3;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.raphimc.minecraftauth.step.bedrock.StepMCChain;
import net.raphimc.minecraftauth.step.java.StepPlayerCertificates;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket1_19_3;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket1_20_2;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginKeyPacket1_19;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.protocol.storage.AuthChainData;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.plugins.events.FillPlayerDataEvent;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount;
import net.raphimc.viaproxy.util.logging.Logger;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ExternalInterface {

    public static void fillPlayerData(final ProxyConnection proxyConnection) {
        Logger.u_info("auth", proxyConnection.getC2P().remoteAddress(), proxyConnection.getGameProfile(), "Filling player data");
        try {
            if (proxyConnection.getUserOptions().account() != null) {
                final Account account = proxyConnection.getUserOptions().account();
                ViaProxy.getSaveManager().accountsSave.ensureRefreshed(account);

                proxyConnection.setGameProfile(account.getGameProfile());
                final UserConnection user = proxyConnection.getUserConnection();

                if (Options.CHAT_SIGNING && proxyConnection.getServerVersion().newerThanOrEqualTo(ProtocolVersion.v1_19) && account instanceof MicrosoftAccount microsoftAccount) {
                    final StepPlayerCertificates.PlayerCertificates playerCertificates = microsoftAccount.getPlayerCertificates();
                    final Instant expiresAt = Instant.ofEpochMilli(playerCertificates.getExpireTimeMs());
                    final long expiresAtMillis = playerCertificates.getExpireTimeMs();
                    final PublicKey publicKey = playerCertificates.getPublicKey();
                    final byte[] publicKeyBytes = publicKey.getEncoded();
                    final byte[] keySignature = playerCertificates.getPublicKeySignature();
                    final PrivateKey privateKey = playerCertificates.getPrivateKey();
                    final UUID uuid = proxyConnection.getGameProfile().getId();

                    byte[] loginHelloKeySignature = keySignature;
                    if (proxyConnection.getClientVersion().equals(ProtocolVersion.v1_19)) {
                        loginHelloKeySignature = playerCertificates.getLegacyPublicKeySignature();
                    }
                    proxyConnection.setLoginHelloPacket(new C2SLoginHelloPacket1_20_2(proxyConnection.getGameProfile().getName(), expiresAt, publicKey, loginHelloKeySignature, proxyConnection.getGameProfile().getId()));

                    user.put(new ChatSession1_19_0(uuid, privateKey, new ProfileKey(expiresAtMillis, publicKeyBytes, playerCertificates.getLegacyPublicKeySignature())));
                    user.put(new ChatSession1_19_1(uuid, privateKey, new ProfileKey(expiresAtMillis, publicKeyBytes, keySignature)));
                    user.put(new ChatSession1_19_3(uuid, privateKey, new ProfileKey(expiresAtMillis, publicKeyBytes, keySignature)));
                } else if (proxyConnection.getServerVersion().equals(BedrockProtocolVersion.bedrockLatest) && account instanceof BedrockAccount bedrockAccount) {
                    final StepMCChain.MCChain mcChain = bedrockAccount.getMcChain();

                    final UUID deviceId = mcChain.getXblXsts().getInitialXblSession().getXblDeviceToken().getId();
                    final String playFabId = bedrockAccount.getPlayFabToken().getPlayFabId();
                    user.put(new AuthChainData(mcChain.getMojangJwt(), mcChain.getIdentityJwt(), mcChain.getPublicKey(), mcChain.getPrivateKey(), deviceId, playFabId));
                }
            }

            ViaProxy.EVENT_MANAGER.call(new FillPlayerDataEvent(proxyConnection));
        } catch (Throwable e) {
            Logger.LOGGER.error("Failed to fill player data", e);
            proxyConnection.kickClient("§cFailed to fill player data. This might be caused by outdated account tokens or rate limits. Wait a couple of seconds and try again. If the problem persists, remove and re-add your account.");
        }

        proxyConnection.getLoginHelloPacket().name = proxyConnection.getGameProfile().getName();
        if (proxyConnection.getLoginHelloPacket() instanceof C2SLoginHelloPacket1_19_3) {
            ((C2SLoginHelloPacket1_19_3) proxyConnection.getLoginHelloPacket()).uuid = proxyConnection.getGameProfile().getId();
        }
    }

    public static void joinServer(final String serverIdHash, final ProxyConnection proxyConnection) throws InterruptedException, ExecutionException {
        Logger.u_info("auth", proxyConnection.getC2P().remoteAddress(), proxyConnection.getGameProfile(), "Trying to join online mode server");
        if (Options.OPENAUTHMOD_AUTH) {
            try {
                final ByteBuf response = proxyConnection.sendCustomPayload(OpenAuthModConstants.JOIN_CHANNEL, PacketTypes.writeString(Unpooled.buffer(), serverIdHash)).get(6, TimeUnit.SECONDS);
                if (response == null) throw new TimeoutException();
                if (response.isReadable() && !response.readBoolean()) throw new TimeoutException();
            } catch (TimeoutException e) {
                proxyConnection.kickClient("§cAuthentication cancelled! You need to install OpenAuthMod in order to join this server.");
            }
        } else if (proxyConnection.getUserOptions().account() instanceof MicrosoftAccount microsoftAccount) {
            try {
                AuthLibServices.SESSION_SERVICE.joinServer(microsoftAccount.getGameProfile(), microsoftAccount.getMcProfile().getMcToken().getAccessToken(), serverIdHash);
            } catch (Throwable e) {
                proxyConnection.kickClient("§cFailed to authenticate with Mojang servers! Please try again in a couple of seconds.");
            }
        } else {
            proxyConnection.kickClient("§cThis server is in online mode and requires a valid authentication mode.");
        }
    }

    public static void signNonce(final byte[] nonce, final C2SLoginKeyPacket1_19 packet, final ProxyConnection proxyConnection) throws InterruptedException, ExecutionException, SignatureException {
        Logger.u_info("auth", proxyConnection.getC2P().remoteAddress(), proxyConnection.getGameProfile(), "Requesting nonce signature");
        final UserConnection user = proxyConnection.getUserConnection();

        if (Options.OPENAUTHMOD_AUTH) {
            try {
                final ByteBuf response = proxyConnection.sendCustomPayload(OpenAuthModConstants.SIGN_NONCE_CHANNEL, PacketTypes.writeByteArray(Unpooled.buffer(), nonce)).get(5, TimeUnit.SECONDS);
                if (response == null) throw new TimeoutException();
                if (!response.readBoolean()) throw new TimeoutException();
                packet.salt = response.readLong();
                packet.signature = PacketTypes.readByteArray(response);
            } catch (TimeoutException e) {
                proxyConnection.kickClient("§cAuthentication cancelled! You need to install OpenAuthMod in order to join this server.");
            }
        } else if (user.has(ChatSession1_19_0.class)) {
            final long salt = ThreadLocalRandom.current().nextLong();
            packet.signature = user.get(ChatSession1_19_0.class).sign(updater -> {
                updater.accept(nonce);
                updater.accept(Longs.toByteArray(salt));
            });
            packet.salt = salt;
        } else {
            proxyConnection.kickClient("§cThis server requires a signed nonce. Please enable chat signing in the config and select a valid authentication mode.");
        }
    }

}
