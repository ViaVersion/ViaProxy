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
package net.raphimc.viaproxy.proxy.external_interface;

import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket1_19_3;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket1_7;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginKeyPacket1_19;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.proxy.ProxyConnection;
import net.raphimc.viaproxy.util.LocalSocketClient;
import net.raphimc.viaproxy.util.logging.Logger;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ExternalInterface {

    public static void fillPlayerData(final C2SLoginHelloPacket1_7 loginHello, final ProxyConnection proxyConnection) throws NoSuchAlgorithmException, InvalidKeySpecException {
        proxyConnection.setLoginHelloPacket(loginHello);
        if (proxyConnection.getLoginHelloPacket() instanceof C2SLoginHelloPacket1_19_3) {
            proxyConnection.setGameProfile(new GameProfile(((C2SLoginHelloPacket1_19_3) proxyConnection.getLoginHelloPacket()).uuid, proxyConnection.getLoginHelloPacket().name));
        } else {
            proxyConnection.setGameProfile(new GameProfile(null, loginHello.name));
        }

        if (Options.LOCAL_SOCKET_AUTH) {
            String[] response = new LocalSocketClient(48941).request("getusername");
            if (response != null && response[0].equals("success")) {
                proxyConnection.setGameProfile(new GameProfile(null, response[1]));
            }

            response = new LocalSocketClient(48941).request("get_public_key_data");
            if (response != null && response[0].equals("success")) {
                final String name = proxyConnection.getGameProfile().getName();
                final UUID uuid = UUIDTypeAdapter.fromString(response[1]);
                final PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(response[2])));
                final byte[] keySignature = Base64.getDecoder().decode(response[3]);
                final Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(response[4]));

                proxyConnection.setGameProfile(new GameProfile(uuid, name));
                proxyConnection.setLoginHelloPacket(new C2SLoginHelloPacket1_19_3(name, expiresAt, publicKey, keySignature, uuid));
            }
        } else if (Options.MC_ACCOUNT != null) {
            proxyConnection.setGameProfile(new GameProfile(Options.MC_ACCOUNT.id(), Options.MC_ACCOUNT.name()));
            // TODO: Key
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
        } else if (Options.LOCAL_SOCKET_AUTH) {
            new LocalSocketClient(48941).request("authenticate", serverIdHash);
        } else if (Options.MC_ACCOUNT != null && !Options.MC_ACCOUNT.prevResult().items().isEmpty()) {
            try {
                AuthLibServices.sessionService.joinServer(new GameProfile(Options.MC_ACCOUNT.id(), Options.MC_ACCOUNT.name()), Options.MC_ACCOUNT.prevResult().prevResult().access_token(), serverIdHash);
            } catch (Throwable e) {
                proxyConnection.kickClient("§cFailed to authenticate with Mojang servers! Please try again later.");
            }
        } else {
            proxyConnection.kickClient("§cThis server is in online mode and requires a valid authentication mode.");
        }
    }

    public static void signNonce(final byte[] nonce, final C2SLoginKeyPacket1_19 packet, final ProxyConnection proxyConnection) throws InterruptedException, ExecutionException {
        Logger.u_info("auth", proxyConnection.getC2P().remoteAddress(), proxyConnection.getGameProfile(), "Requesting nonce signature");
        if (Options.OPENAUTHMOD_AUTH) {
            try {
                final ByteBuf response = proxyConnection.sendCustomPayload(OpenAuthModConstants.SIGN_NONCE_CHANNEL, PacketTypes.writeByteArray(Unpooled.buffer(), nonce)).get(5, TimeUnit.SECONDS);
                if (response == null) throw new TimeoutException();
                if (!response.readBoolean()) throw new TimeoutException();
                packet.salt = response.readLong();
                packet.signature = PacketTypes.readByteArray(response);
            } catch (TimeoutException ignored) {
            }
        } else if (Options.LOCAL_SOCKET_AUTH) {
            final String[] response = new LocalSocketClient(48941).request("sign_nonce", Base64.getEncoder().encodeToString(nonce));
            if (response != null && response[0].equals("success")) {
                packet.salt = Long.valueOf(response[1]);
                packet.signature = Base64.getDecoder().decode(response[2]);
            }
        } else if (Options.MC_ACCOUNT != null) { // TODO: Key
        }
    }

}
