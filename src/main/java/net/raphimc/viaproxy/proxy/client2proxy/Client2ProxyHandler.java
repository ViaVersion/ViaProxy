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
package net.raphimc.viaproxy.proxy.client2proxy;

import com.google.common.collect.Lists;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.CookieStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.IntendedState;
import net.raphimc.netminecraft.packet.IPacket;
import net.raphimc.netminecraft.packet.impl.handshake.C2SHandshakePacket;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.events.ConnectEvent;
import net.raphimc.viaproxy.plugins.events.PreConnectEvent;
import net.raphimc.viaproxy.plugins.events.Proxy2ServerHandlerCreationEvent;
import net.raphimc.viaproxy.plugins.events.ProxySessionCreationEvent;
import net.raphimc.viaproxy.protocoltranslator.ProtocolTranslator;
import net.raphimc.viaproxy.protocoltranslator.viaproxy.ViaProxyConfig;
import net.raphimc.viaproxy.proxy.packethandler.*;
import net.raphimc.viaproxy.proxy.proxy2server.Proxy2ServerChannelInitializer;
import net.raphimc.viaproxy.proxy.proxy2server.Proxy2ServerHandler;
import net.raphimc.viaproxy.proxy.session.BedrockProxyConnection;
import net.raphimc.viaproxy.proxy.session.DummyProxyConnection;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.proxy.session.UserOptions;
import net.raphimc.viaproxy.proxy.util.*;
import net.raphimc.viaproxy.saves.impl.accounts.ClassicAccount;
import net.raphimc.viaproxy.util.AddressUtil;
import net.raphimc.viaproxy.util.ArrayHelper;
import net.raphimc.viaproxy.util.ProtocolVersionDetector;
import net.raphimc.viaproxy.util.ProtocolVersionUtil;
import net.raphimc.viaproxy.util.logging.Logger;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.UnresolvedAddressException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Client2ProxyHandler extends SimpleChannelInboundHandler<IPacket> {

    private ProxyConnection proxyConnection;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        this.proxyConnection = new DummyProxyConnection(ctx.channel());
        ViaProxy.getConnectedClients().add(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (this.proxyConnection instanceof DummyProxyConnection) return;

        try {
            this.proxyConnection.getChannel().close();
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IPacket packet) throws Exception {
        if (this.proxyConnection.isClosed()) return;

        if (this.proxyConnection.getC2pConnectionState() == ConnectionState.HANDSHAKING) {
            if (packet instanceof C2SHandshakePacket) this.handleHandshake((C2SHandshakePacket) packet);
            else throw new IllegalStateException("Unexpected packet in HANDSHAKING state");
            return;
        }

        final List<ChannelFutureListener> listeners = Lists.newArrayList(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        for (PacketHandler packetHandler : this.proxyConnection.getPacketHandlers()) {
            if (!packetHandler.handleC2P(packet, listeners)) {
                return;
            }
        }
        this.proxyConnection.getChannel().writeAndFlush(packet).addListeners(listeners.toArray(new ChannelFutureListener[0]));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ExceptionUtil.handleNettyException(ctx, cause, this.proxyConnection);
    }

    private void handleHandshake(final C2SHandshakePacket packet) {
        final ProtocolVersion clientVersion = ProtocolVersion.getProtocol(packet.protocolVersion);

        if (packet.intendedState == null) {
            throw CloseAndReturn.INSTANCE;
        }

        this.proxyConnection.setClientVersion(clientVersion);
        this.proxyConnection.setC2pConnectionState(packet.intendedState.getConnectionState());

        if (!clientVersion.isKnown()) {
            this.proxyConnection.kickClient("§cYour client version is not supported by ViaProxy!");
        }

        final String[] handshakeParts = packet.address.split("\0");

        SocketAddress serverAddress = ViaProxy.getConfig().getTargetAddress();
        ProtocolVersion serverVersion = ViaProxy.getConfig().getTargetVersion();
        String classicMpPass = ViaProxy.getConfig().getAccount() instanceof ClassicAccount classicAccount ? classicAccount.getMppass() : null;

        if (ViaProxy.getConfig().getWildcardDomainHandling() == ViaProxyConfig.WildcardDomainHandling.PUBLIC) {
            try {
                if (handshakeParts[0].toLowerCase().contains(".viaproxy.")) {
                    handshakeParts[0] = handshakeParts[0].substring(0, handshakeParts[0].toLowerCase().lastIndexOf(".viaproxy."));
                } else {
                    throw new IllegalArgumentException();
                }
                final ArrayHelper arrayHelper = ArrayHelper.instanceOf(handshakeParts[0].split(Pattern.quote("_")));
                if (arrayHelper.getLength() < 3) {
                    throw new IllegalArgumentException();
                }
                final String versionString = arrayHelper.get(arrayHelper.getLength() - 1);
                serverVersion = ProtocolVersionUtil.fromNameLenient(versionString);
                if (serverVersion == null) {
                    this.proxyConnection.kickClient("§cWrong domain syntax!\n§cUnknown server version.");
                }
                final String connectIP = arrayHelper.getAsString(0, arrayHelper.getLength() - 3, "_");
                final int connectPort = arrayHelper.getInteger(arrayHelper.getLength() - 2);
                serverAddress = AddressUtil.parse(connectIP + ":" + connectPort, serverVersion);
            } catch (IllegalArgumentException e) {
                this.proxyConnection.kickClient("§cWrong domain syntax! §6Please use:\n§7address_port_version.viaproxy.hostname");
            }
        } else if (ViaProxy.getConfig().getWildcardDomainHandling() == ViaProxyConfig.WildcardDomainHandling.INTERNAL) {
            final ArrayHelper arrayHelper = ArrayHelper.instanceOf(handshakeParts[0].split("\7"));
            final String versionString = arrayHelper.get(1);
            serverVersion = ProtocolVersionUtil.fromNameLenient(versionString);
            if (serverVersion == null) throw CloseAndReturn.INSTANCE;
            serverAddress = AddressUtil.parse(arrayHelper.get(0), serverVersion);
            if (arrayHelper.isIndexValid(2)) {
                classicMpPass = arrayHelper.getString(2);
            }
        }

        if (packet.intendedState.getConnectionState() == ConnectionState.STATUS && !ViaProxy.getConfig().shouldAllowBetaPinging() && serverVersion.olderThanOrEqualTo(LegacyProtocolVersion.b1_7tob1_7_3)) {
            if (!ViaProxy.getConfig().getCustomMotd().isBlank()) {
                this.proxyConnection.kickClient(ViaProxy.getConfig().getCustomMotd());
            }
            this.proxyConnection.kickClient("§7ViaProxy is working!\n§7Connect to join the configured server");
        }

        if (packet.intendedState.getConnectionState() == ConnectionState.LOGIN && TransferDataHolder.hasTempRedirect(this.proxyConnection.getC2P())) {
            serverAddress = TransferDataHolder.removeTempRedirect(this.proxyConnection.getC2P());
        }

        final PreConnectEvent preConnectEvent = new PreConnectEvent(serverAddress, serverVersion, clientVersion, this.proxyConnection.getC2P());
        if (ViaProxy.EVENT_MANAGER.call(preConnectEvent).isCancelled()) {
            this.proxyConnection.kickClient(preConnectEvent.getCancelMessage());
        }
        serverAddress = preConnectEvent.getServerAddress();
        serverVersion = preConnectEvent.getServerVersion();

        final UserOptions userOptions = new UserOptions(classicMpPass, ViaProxy.getConfig().getAccount());
        ChannelUtil.disableAutoRead(this.proxyConnection.getC2P());

        if (packet.intendedState.getConnectionState() == ConnectionState.LOGIN && serverVersion.equals(ProtocolTranslator.AUTO_DETECT_PROTOCOL)) {
            SocketAddress finalServerAddress = serverAddress;
            CompletableFuture.runAsync(() -> {
                final ProtocolVersion detectedVersion = ProtocolVersionDetector.get(finalServerAddress, clientVersion);
                this.connect(finalServerAddress, detectedVersion, clientVersion, packet.intendedState, userOptions, handshakeParts);
            }).exceptionally(t -> {
                if (t instanceof ConnectException || t instanceof UnresolvedAddressException) {
                    this.proxyConnection.kickClient("§cCould not connect to the backend server!");
                } else {
                    this.proxyConnection.kickClient("§cAutomatic protocol detection failed!\n§c" + t.getMessage());
                }
                return null;
            });
        } else {
            this.connect(serverAddress, serverVersion, clientVersion, packet.intendedState, userOptions, handshakeParts);
        }
    }

    private void connect(final SocketAddress serverAddress, final ProtocolVersion serverVersion, final ProtocolVersion clientVersion, final IntendedState intendedState, final UserOptions userOptions, final String[] handshakeParts) {
        final Supplier<ChannelHandler> handlerSupplier = () -> ViaProxy.EVENT_MANAGER.call(new Proxy2ServerHandlerCreationEvent(new Proxy2ServerHandler(), false)).getHandler();
        final ProxyConnection proxyConnection;
        if (serverVersion.equals(BedrockProtocolVersion.bedrockLatest)) {
            proxyConnection = new BedrockProxyConnection(handlerSupplier, Proxy2ServerChannelInitializer::new, this.proxyConnection.getC2P());
        } else {
            proxyConnection = new ProxyConnection(handlerSupplier, Proxy2ServerChannelInitializer::new, this.proxyConnection.getC2P());
        }
        this.proxyConnection = ViaProxy.EVENT_MANAGER.call(new ProxySessionCreationEvent<>(proxyConnection, false)).getProxySession();
        this.proxyConnection.getC2P().attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY).set(this.proxyConnection);
        this.proxyConnection.setClientVersion(clientVersion);
        this.proxyConnection.setC2pConnectionState(intendedState.getConnectionState());
        this.proxyConnection.setUserOptions(userOptions);
        this.proxyConnection.getPacketHandlers().add(new StatusPacketHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new OpenAuthModPacketHandler(this.proxyConnection));
        if (ViaProxy.getConfig().shouldSupportSimpleVoiceChat() && serverVersion.newerThan(ProtocolVersion.v1_14) && clientVersion.newerThan(ProtocolVersion.v1_14)) {
            this.proxyConnection.getPacketHandlers().add(new SimpleVoiceChatPacketHandler(this.proxyConnection));
        }
        if (clientVersion.newerThanOrEqualTo(ProtocolVersion.v1_8)) {
            this.proxyConnection.getPacketHandlers().add(new BrandCustomPayloadPacketHandler(this.proxyConnection));
        }
        this.proxyConnection.getPacketHandlers().add(new CompressionPacketHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new LoginPacketHandler(this.proxyConnection));
        if (clientVersion.newerThanOrEqualTo(ProtocolVersion.v1_20_5)) {
            this.proxyConnection.getPacketHandlers().add(new TransferPacketHandler(this.proxyConnection));
        }
        if (clientVersion.newerThanOrEqualTo(ProtocolVersion.v1_20_2) || serverVersion.newerThanOrEqualTo(ProtocolVersion.v1_20_2)) {
            this.proxyConnection.getPacketHandlers().add(new ConfigurationPacketHandler(this.proxyConnection));
        }
        if (clientVersion.newerThanOrEqualTo(ProtocolVersion.v1_19_3) && serverVersion.newerThanOrEqualTo(ProtocolVersion.v1_19_3)) {
            this.proxyConnection.getPacketHandlers().add(new ChatSignaturePacketHandler(this.proxyConnection));
        }
        this.proxyConnection.getPacketHandlers().add(new ResourcePackPacketHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new UnexpectedPacketHandler(this.proxyConnection));

        Logger.u_info("connect", this.proxyConnection.getC2P().remoteAddress(), this.proxyConnection.getGameProfile(), "[" + clientVersion.getName() + " <-> " + serverVersion.getName() + "] Connecting to " + AddressUtil.toString(serverAddress));
        ViaProxy.EVENT_MANAGER.call(new ConnectEvent(this.proxyConnection));

        this.proxyConnection.connectToServer(serverAddress, serverVersion).addListeners((ThrowingChannelFutureListener) f -> {
            if (f.isSuccess()) {
                f.channel().eventLoop().submit(() -> { // Reschedule so the packets get sent after the channel is fully initialized and active
                    if (ViaProxy.getConfig().useBackendHaProxy()) {
                        this.proxyConnection.getChannel().writeAndFlush(HAProxyUtil.createMessage(this.proxyConnection.getC2P(), this.proxyConnection.getChannel(), clientVersion)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    }

                    final String address;
                    final int port;
                    if (serverAddress instanceof InetSocketAddress inetSocketAddress) {
                        address = inetSocketAddress.getHostString();
                        port = inetSocketAddress.getPort();
                    } else {
                        address = AddressUtil.toString(serverAddress);
                        port = 25565;
                    }

                    handshakeParts[0] = address;
                    final C2SHandshakePacket newHandshakePacket = new C2SHandshakePacket(clientVersion.getOriginalVersion(), String.join("\0", handshakeParts), port, intendedState);
                    this.proxyConnection.getChannel().writeAndFlush(newHandshakePacket).addListeners(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE, (ChannelFutureListener) f2 -> {
                        if (f2.isSuccess()) {
                            this.proxyConnection.setP2sConnectionState(intendedState.getConnectionState());
                            final UserConnection userConnection = this.proxyConnection.getUserConnection();
                            if (userConnection.has(CookieStorage.class) && TransferDataHolder.hasCookieStorage(this.proxyConnection.getC2P())) {
                                userConnection.get(CookieStorage.class).cookies().putAll(TransferDataHolder.removeCookieStorage(this.proxyConnection.getC2P()).cookies());
                            }
                        }
                    });

                    ChannelUtil.restoreAutoRead(this.proxyConnection.getC2P());
                });
            }
        }, (ThrowingChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                if (f.cause() instanceof ConnectException || f.cause() instanceof UnresolvedAddressException) {
                    this.proxyConnection.kickClient("§cCould not connect to the backend server!");
                } else {
                    Logger.LOGGER.error("Error while connecting to the backend server", f.cause());
                    this.proxyConnection.kickClient("§cAn error occurred while connecting to the backend server: " + f.cause().getMessage() + "\n§cCheck the console for more information.");
                }
            }
        });
    }

}
