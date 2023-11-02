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

import com.google.common.collect.Lists;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.packet.IPacket;
import net.raphimc.netminecraft.packet.impl.handshake.C2SHandshakePacket;
import net.raphimc.netminecraft.util.ServerAddress;
import net.raphimc.vialoader.util.VersionEnum;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.plugins.events.ConnectEvent;
import net.raphimc.viaproxy.plugins.events.PreConnectEvent;
import net.raphimc.viaproxy.plugins.events.Proxy2ServerHandlerCreationEvent;
import net.raphimc.viaproxy.plugins.events.ResolveSrvEvent;
import net.raphimc.viaproxy.protocolhack.viaproxy.ViaBedrockTransferHolder;
import net.raphimc.viaproxy.proxy.packethandler.*;
import net.raphimc.viaproxy.proxy.proxy2server.Proxy2ServerChannelInitializer;
import net.raphimc.viaproxy.proxy.proxy2server.Proxy2ServerHandler;
import net.raphimc.viaproxy.proxy.session.BedrockProxyConnection;
import net.raphimc.viaproxy.proxy.session.DummyProxyConnection;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;
import net.raphimc.viaproxy.proxy.session.UserOptions;
import net.raphimc.viaproxy.proxy.util.*;
import net.raphimc.viaproxy.util.ArrayHelper;
import net.raphimc.viaproxy.util.logging.Logger;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.UnresolvedAddressException;
import java.util.List;
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
        final VersionEnum clientVersion = VersionEnum.fromProtocolVersion(ProtocolVersion.getProtocol(packet.protocolVersion));

        if (packet.intendedState == null) {
            throw CloseAndReturn.INSTANCE;
        }

        this.proxyConnection.setClientVersion(clientVersion);
        this.proxyConnection.setC2pConnectionState(packet.intendedState);

        if (clientVersion == VersionEnum.UNKNOWN || !VersionEnum.OFFICIAL_SUPPORTED_PROTOCOLS.contains(clientVersion)) {
            this.proxyConnection.kickClient("§cYour client version is not supported by ViaProxy!");
        }

        final String[] handshakeParts;
        if (Options.PLAYER_INFO_FORWARDING) {
            handshakeParts = new String[3];
            final String[] receivedParts = packet.address.split("\0");
            for (int i = 0; i < receivedParts.length && i < handshakeParts.length; i++) {
                handshakeParts[i] = receivedParts[i];
            }
            if (handshakeParts[0] == null) {
                this.proxyConnection.kickClient("§cMissing hostname in handshake. Ensure that your proxy has player info forwarding enabled.");
            }
            if (handshakeParts[1] == null) {
                this.proxyConnection.kickClient("§cMissing player IP in handshake. Ensure that your proxy has player info forwarding enabled.");
            }
            if (handshakeParts[2] == null) {
                this.proxyConnection.kickClient("§cMissing player UUID in handshake. Ensure that your proxy has player info forwarding enabled.");
            }
        } else {
            handshakeParts = new String[]{packet.address};
        }

        String connectIP = Options.CONNECT_ADDRESS;
        int connectPort = Options.CONNECT_PORT;
        VersionEnum serverVersion = Options.PROTOCOL_VERSION;
        String classicMpPass = Options.CLASSIC_MP_PASS;

        if (Options.INTERNAL_SRV_MODE) {
            final ArrayHelper arrayHelper = ArrayHelper.instanceOf(handshakeParts[0].split("\7"));
            connectIP = arrayHelper.get(0);
            connectPort = arrayHelper.getInteger(1);
            final String versionString = arrayHelper.get(2);
            if (arrayHelper.isIndexValid(3)) {
                classicMpPass = arrayHelper.getString(3);
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
                if (handshakeParts[0].toLowerCase().contains(".viaproxy.")) {
                    handshakeParts[0] = handshakeParts[0].substring(0, handshakeParts[0].toLowerCase().lastIndexOf(".viaproxy."));
                } else {
                    throw CloseAndReturn.INSTANCE;
                }
                final ArrayHelper arrayHelper = ArrayHelper.instanceOf(handshakeParts[0].split(Pattern.quote("_")));
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

        if (serverVersion.equals(VersionEnum.bedrockLatest) && packet.intendedState == ConnectionState.LOGIN && ViaBedrockTransferHolder.hasTempRedirect(this.proxyConnection.getC2P())) {
            final InetSocketAddress newAddress = ViaBedrockTransferHolder.removeTempRedirect(this.proxyConnection.getC2P());
            connectIP = newAddress.getHostString();
            connectPort = newAddress.getPort();
        }

        final ResolveSrvEvent resolveSrvEvent = ViaProxy.EVENT_MANAGER.call(new ResolveSrvEvent(serverVersion, connectIP, connectPort));
        connectIP = resolveSrvEvent.getHost();
        connectPort = resolveSrvEvent.getPort();

        final ServerAddress serverAddress;
        if (resolveSrvEvent.isCancelled() || serverVersion.isOlderThan(VersionEnum.r1_3_1tor1_3_2) || serverVersion.equals(VersionEnum.bedrockLatest)) {
            serverAddress = new ServerAddress(connectIP, connectPort);
        } else {
            serverAddress = ServerAddress.fromSRV(connectIP + ":" + connectPort);
        }

        final PreConnectEvent preConnectEvent = new PreConnectEvent(serverAddress, serverVersion, clientVersion, this.proxyConnection.getC2P());
        if (ViaProxy.EVENT_MANAGER.call(preConnectEvent).isCancelled()) {
            this.proxyConnection.kickClient(preConnectEvent.getCancelMessage());
        }

        final Supplier<ChannelHandler> handlerSupplier = () -> ViaProxy.EVENT_MANAGER.call(new Proxy2ServerHandlerCreationEvent(new Proxy2ServerHandler(), false)).getHandler();
        if (serverVersion.equals(VersionEnum.bedrockLatest)) {
            this.proxyConnection = new BedrockProxyConnection(handlerSupplier, Proxy2ServerChannelInitializer::new, this.proxyConnection.getC2P());
        } else {
            this.proxyConnection = new ProxyConnection(handlerSupplier, Proxy2ServerChannelInitializer::new, this.proxyConnection.getC2P());
        }
        this.proxyConnection.getC2P().attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY).set(this.proxyConnection);
        this.proxyConnection.setClientVersion(clientVersion);
        this.proxyConnection.setC2pConnectionState(packet.intendedState);
        this.proxyConnection.setUserOptions(new UserOptions(classicMpPass, Options.MC_ACCOUNT));
        this.proxyConnection.getPacketHandlers().add(new StatusPacketHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new CustomPayloadPacketHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new CompressionPacketHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new LoginPacketHandler(this.proxyConnection));
        if (clientVersion.isNewerThanOrEqualTo(VersionEnum.r1_20_2) || serverVersion.isNewerThanOrEqualTo(VersionEnum.r1_20_2)) {
            this.proxyConnection.getPacketHandlers().add(new ConfigurationPacketHandler(this.proxyConnection));
        }
        this.proxyConnection.getPacketHandlers().add(new ResourcePackPacketHandler(this.proxyConnection));
        this.proxyConnection.getPacketHandlers().add(new UnexpectedPacketHandler(this.proxyConnection));

        ChannelUtil.disableAutoRead(this.proxyConnection.getC2P());
        Logger.u_info("connect", this.proxyConnection.getC2P().remoteAddress(), this.proxyConnection.getGameProfile(), "[" + clientVersion.getName() + " <-> " + serverVersion.getName() + "] Connecting to " + serverAddress.getAddress() + ":" + serverAddress.getPort());
        ViaProxy.EVENT_MANAGER.call(new ConnectEvent(this.proxyConnection));

        this.proxyConnection.connectToServer(serverAddress, serverVersion).addListeners((ThrowingChannelFutureListener) f -> {
            if (f.isSuccess()) {
                f.channel().eventLoop().submit(() -> { // Reschedule so the packets get sent after the channel is fully initialized and active
                    if (Options.SERVER_HAPROXY_PROTOCOL) {
                        this.proxyConnection.getChannel().writeAndFlush(HAProxyUtil.createMessage(this.proxyConnection.getC2P(), this.proxyConnection.getChannel(), clientVersion)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    }

                    handshakeParts[0] = serverAddress.getAddress();
                    final C2SHandshakePacket newHandshakePacket = new C2SHandshakePacket(clientVersion.getOriginalVersion(), String.join("\0", handshakeParts), serverAddress.getPort(), packet.intendedState);
                    this.proxyConnection.getChannel().writeAndFlush(newHandshakePacket).addListeners(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE, (ChannelFutureListener) f2 -> {
                        if (f2.isSuccess()) {
                            this.proxyConnection.setP2sConnectionState(packet.intendedState);
                        }
                    });

                    ChannelUtil.restoreAutoRead(this.proxyConnection.getC2P());
                });
            }
        }, (ThrowingChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                if (f.cause() instanceof ConnectException || f.cause() instanceof UnresolvedAddressException) {
                    this.proxyConnection.kickClient("§cCould not connect to the backend server!\n§cTry again in a few seconds.");
                } else {
                    Logger.LOGGER.error("Error while connecting to the backend server", f.cause());
                    this.proxyConnection.kickClient("§cAn error occurred while connecting to the backend server: " + f.cause().getMessage() + "\n§cCheck the console for more information.");
                }
            }
        });
    }

}
