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
package net.raphimc.viaproxy.proxy.client2proxy;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
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
import net.raphimc.netminecraft.packet.impl.handshake.C2SHandshakePacket;
import net.raphimc.netminecraft.packet.impl.login.*;
import net.raphimc.netminecraft.util.ServerAddress;
import net.raphimc.viaprotocolhack.util.VersionEnum;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.plugins.PluginManager;
import net.raphimc.viaproxy.plugins.events.PreConnectEvent;
import net.raphimc.viaproxy.plugins.events.Proxy2ServerHandlerCreationEvent;
import net.raphimc.viaproxy.plugins.events.ResolveSrvEvent;
import net.raphimc.viaproxy.proxy.LoginState;
import net.raphimc.viaproxy.proxy.ProxyConnection;
import net.raphimc.viaproxy.proxy.external_interface.AuthLibServices;
import net.raphimc.viaproxy.proxy.external_interface.ExternalInterface;
import net.raphimc.viaproxy.proxy.external_interface.OpenAuthModConstants;
import net.raphimc.viaproxy.proxy.proxy2server.Proxy2ServerChannelInitializer;
import net.raphimc.viaproxy.proxy.proxy2server.Proxy2ServerHandler;
import net.raphimc.viaproxy.proxy.util.CloseAndReturn;
import net.raphimc.viaproxy.proxy.util.ExceptionUtil;
import net.raphimc.viaproxy.util.ArrayHelper;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;

public class Client2ProxyHandler extends SimpleChannelInboundHandler<IPacket> {

    private static final KeyPair KEY_PAIR;
    private static final Random RANDOM = new Random();

    static {
        KEY_PAIR = CryptUtil.generateKeyPair();
    }

    private ProxyConnection proxyConnection;
    private LoginState loginState = LoginState.FIRST_PACKET;

    private final byte[] verifyToken = new byte[4];
    private int customPayloadPacketId = -1;
    private int chatSessionUpdatePacketId = -1;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        RANDOM.nextBytes(this.verifyToken);
        this.proxyConnection = new ProxyConnection(() -> PluginManager.EVENT_MANAGER.call(new Proxy2ServerHandlerCreationEvent(new Proxy2ServerHandler())).getHandler(), Proxy2ServerChannelInitializer::new, ctx.channel());
        ctx.channel().attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY).set(this.proxyConnection);

        ViaProxy.c2pChannels.add(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        try {
            this.proxyConnection.getChannel().close();
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IPacket packet) throws Exception {
        if (this.proxyConnection.isClosed()) return;

        switch (this.proxyConnection.getConnectionState()) {
            case HANDSHAKING:
                if (packet instanceof C2SHandshakePacket) this.handleHandshake((C2SHandshakePacket) packet);
                else break;

                return;
            case LOGIN:
                if (packet instanceof C2SLoginHelloPacket1_7) this.handleLoginHello((C2SLoginHelloPacket1_7) packet);
                else if (packet instanceof C2SLoginKeyPacket1_7) this.handleLoginKey((C2SLoginKeyPacket1_7) packet);
                else if (packet instanceof C2SLoginCustomPayloadPacket) this.handleLoginCustomPayload((C2SLoginCustomPayloadPacket) packet);
                else break;

                return;
            case PLAY:
                final UnknownPacket unknownPacket = (UnknownPacket) packet;
                if (unknownPacket.packetId == this.customPayloadPacketId) {
                    if (this.handlePlayCustomPayload(Unpooled.wrappedBuffer(unknownPacket.data))) return;
                } else if (unknownPacket.packetId == this.chatSessionUpdatePacketId && this.proxyConnection.getChannel().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).get() == null) {
                    return;
                }
                break;
        }

        this.proxyConnection.getChannel().writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ExceptionUtil.handleNettyException(ctx, cause, this.proxyConnection);
    }

    private void handleHandshake(final C2SHandshakePacket packet) throws InterruptedException {
        String address = packet.address.split("\0")[0];
        final VersionEnum clientVersion = VersionEnum.fromProtocolVersion(ProtocolVersion.getProtocol(packet.protocolVersion));

        if (packet.intendedState != ConnectionState.STATUS && packet.intendedState != ConnectionState.LOGIN) {
            throw CloseAndReturn.INSTANCE;
        }

        this.proxyConnection.setClientVersion(clientVersion);
        this.proxyConnection.setConnectionState(packet.intendedState);

        if (clientVersion == VersionEnum.UNKNOWN || !VersionEnum.OFFICIAL_SUPPORTED_PROTOCOLS.contains(clientVersion)) {
            this.proxyConnection.kickClient("§cYour client version is not supported by ViaProxy!");
        }

        this.customPayloadPacketId = MCPackets.C2S_PLUGIN_MESSAGE.getId(clientVersion.getVersion());
        this.chatSessionUpdatePacketId = MCPackets.C2S_CHAT_SESSION_UPDATE.getId(clientVersion.getVersion());

        String connectIP = Options.CONNECT_ADDRESS;
        int connectPort = Options.CONNECT_PORT;
        VersionEnum serverVersion = Options.PROTOCOL_VERSION;

        if (Options.INTERNAL_SRV_MODE) {
            final ArrayHelper arrayHelper = ArrayHelper.instanceOf(address.split("\7"));
            connectIP = arrayHelper.get(0);
            connectPort = arrayHelper.getInteger(1);
            final String versionString = arrayHelper.get(2);
            if (arrayHelper.isIndexValid(3)) {
                this.proxyConnection.setClassicMpPass(arrayHelper.getString(3));
            }
            for (VersionEnum v : VersionEnum.getAllVersions()) {
                if (v.getName().equalsIgnoreCase(versionString)) {
                    serverVersion = v;
                    break;
                }
            }
            if (serverVersion == null) throw CloseAndReturn.INSTANCE;
        } else if (Options.SRV_MODE) {
            try {
                if (address.toLowerCase().contains(".viaproxy.")) {
                    address = address.substring(0, address.toLowerCase().lastIndexOf(".viaproxy."));
                } else {
                    throw CloseAndReturn.INSTANCE;
                }
                final ArrayHelper arrayHelper = ArrayHelper.instanceOf(address.split(Pattern.quote("_")));
                if (arrayHelper.getLength() < 3) {
                    throw CloseAndReturn.INSTANCE;
                }
                connectIP = arrayHelper.getAsString(0, arrayHelper.getLength() - 3, "_");
                connectPort = arrayHelper.getInteger(arrayHelper.getLength() - 2);
                final String versionString = arrayHelper.get(arrayHelper.getLength() - 1);
                for (VersionEnum v : VersionEnum.getAllVersions()) {
                    if (v.getName().replace(" ", "-").equalsIgnoreCase(versionString)) {
                        serverVersion = v;
                        break;
                    }
                }
                if (serverVersion == null) throw CloseAndReturn.INSTANCE;
            } catch (CloseAndReturn e) {
                this.proxyConnection.kickClient("§cWrong SRV syntax! §6Please use:\n§7ip_port_version.viaproxy.hostname");
            }
        }

        final ResolveSrvEvent resolveSrvEvent = PluginManager.EVENT_MANAGER.call(new ResolveSrvEvent(serverVersion, connectIP, connectPort));
        connectIP = resolveSrvEvent.getHost();
        connectPort = resolveSrvEvent.getPort();

        final ServerAddress serverAddress;
        if (resolveSrvEvent.isCancelled() || serverVersion.isOlderThan(VersionEnum.r1_3_1tor1_3_2)) {
            serverAddress = new ServerAddress(connectIP, connectPort);
        } else {
            serverAddress = ServerAddress.fromSRV(connectIP + ":" + connectPort);
        }

        final PreConnectEvent preConnectEvent = new PreConnectEvent(serverAddress, serverVersion, clientVersion, this.proxyConnection.getC2P());
        if (PluginManager.EVENT_MANAGER.call(preConnectEvent).isCancelled()) {
            this.proxyConnection.kickClient(preConnectEvent.getCancelMessage());
        }

        Logger.u_info("connect", this.proxyConnection.getC2P().remoteAddress(), this.proxyConnection.getGameProfile(), "[" + clientVersion.getName() + " <-> " + serverVersion.getName() + "] Connecting to " + serverAddress.getAddress() + ":" + serverAddress.getPort());
        try {
            this.proxyConnection.connectToServer(serverAddress, serverVersion);
            this.proxyConnection.getChannel().writeAndFlush(new C2SHandshakePacket(clientVersion.getOriginalVersion(), serverAddress.getAddress(), serverAddress.getPort(), packet.intendedState)).await().addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            this.proxyConnection.setConnectionState(packet.intendedState);
        } catch (Throwable e) {
            if (e instanceof ConnectException) { // Trust me, this is not always false
                this.proxyConnection.kickClient("§cCould not connect to the backend server!\n§cTry again in a few seconds.");
            } else {
                Logger.LOGGER.error("Error while connecting to the backend server", e);
                this.proxyConnection.kickClient("§cAn error occurred while connecting to the backend server: " + e.getMessage() + "\n§cCheck the console for more information.");
            }
        }
    }

    private void handleLoginHello(C2SLoginHelloPacket1_7 packet) throws NoSuchAlgorithmException, InvalidKeySpecException, AuthenticationException {
        if (this.loginState != LoginState.FIRST_PACKET) throw CloseAndReturn.INSTANCE;
        this.loginState = LoginState.SENT_HELLO;

        if (packet instanceof C2SLoginHelloPacket1_19) {
            final C2SLoginHelloPacket1_19 packet1_19 = (C2SLoginHelloPacket1_19) packet;
            if (packet1_19.expiresAt != null && packet1_19.expiresAt.isBefore(Instant.now())) {
                throw new IllegalStateException("Expired public key");
            }
        }

        ExternalInterface.fillPlayerData(packet, this.proxyConnection);

        if (Options.ONLINE_MODE) {
            this.proxyConnection.getC2P().writeAndFlush(new S2CLoginKeyPacket1_8("", KEY_PAIR.getPublic().getEncoded(), this.verifyToken)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } else {
            this.proxyConnection.getChannel().writeAndFlush(this.proxyConnection.getLoginHelloPacket()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
    }

    private void handleLoginKey(final C2SLoginKeyPacket1_7 packet) throws GeneralSecurityException, InterruptedException {
        if (this.proxyConnection.getClientVersion().isOlderThanOrEqualTo(VersionEnum.r1_12_2) && new String(packet.encryptedNonce, StandardCharsets.UTF_8).equals(OpenAuthModConstants.DATA_CHANNEL)) { // 1.8-1.12.2 OpenAuthMod response handling
            final ByteBuf byteBuf = Unpooled.wrappedBuffer(packet.encryptedSecretKey);
            this.proxyConnection.handleCustomPayload(PacketTypes.readVarInt(byteBuf), byteBuf);
            return;
        }

        if (this.loginState != LoginState.SENT_HELLO) throw CloseAndReturn.INSTANCE;
        this.loginState = LoginState.SENT_KEY;

        if (packet.encryptedNonce != null) {
            if (!Arrays.equals(this.verifyToken, CryptUtil.decryptData(KEY_PAIR.getPrivate(), packet.encryptedNonce))) {
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

        final SecretKey secretKey = CryptUtil.decryptSecretKey(KEY_PAIR.getPrivate(), packet.encryptedSecretKey);
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

        this.proxyConnection.getChannel().writeAndFlush(this.proxyConnection.getLoginHelloPacket()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    private void handleLoginCustomPayload(final C2SLoginCustomPayloadPacket packet) {
        if (packet.response == null || !this.proxyConnection.handleCustomPayload(packet.queryId, Unpooled.wrappedBuffer(packet.response))) {
            this.proxyConnection.getChannel().writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
    }

    private boolean handlePlayCustomPayload(final ByteBuf packet) {
        final String channel = PacketTypes.readString(packet, Short.MAX_VALUE); // channel
        if (channel.equals(OpenAuthModConstants.DATA_CHANNEL)) {
            return this.proxyConnection.handleCustomPayload(PacketTypes.readVarInt(packet), packet);
        }
        return false;
    }

}
