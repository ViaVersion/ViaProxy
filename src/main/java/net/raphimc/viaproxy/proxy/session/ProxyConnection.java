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
package net.raphimc.viaproxy.proxy.session;

import com.google.common.net.HostAndPort;
import com.mojang.authlib.GameProfile;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import net.lenni0451.mcstructs.nbt.INbtTag;
import net.lenni0451.mcstructs.nbt.tags.CompoundTag;
import net.lenni0451.mcstructs.nbt.tags.StringTag;
import net.lenni0451.mcstructs.text.components.StringComponent;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.NetClient;
import net.raphimc.netminecraft.netty.crypto.AESEncryption;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket1_7;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginDisconnectPacket1_20_3;
import net.raphimc.netminecraft.packet.impl.status.S2CStatusResponsePacket;
import net.raphimc.netminecraft.packet.registry.PacketRegistryUtil;
import net.raphimc.netminecraft.util.ChannelType;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.ConsoleFormatter;
import net.raphimc.viaproxy.proxy.packethandler.PacketHandler;
import net.raphimc.viaproxy.proxy.util.CloseAndReturn;
import net.raphimc.viaproxy.util.logging.Logger;

import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProxyConnection extends NetClient {

    public static final AttributeKey<ProxyConnection> PROXY_CONNECTION_ATTRIBUTE_KEY = AttributeKey.valueOf("proxy_connection");

    private final Channel c2p;
    private final List<PacketHandler> packetHandlers = new ArrayList<>();

    private SocketAddress serverAddress;

    private ProtocolVersion serverVersion;
    private ProtocolVersion clientVersion;

    private HostAndPort clientHandshakeAddress;
    private GameProfile gameProfile;
    private C2SLoginHelloPacket1_7 loginHelloPacket;
    private Key storedSecretKey;

    private UserConnection userConnection;
    private UserOptions userOptions;

    private ConnectionState c2pConnectionState = ConnectionState.HANDSHAKING;
    private ConnectionState p2sConnectionState = ConnectionState.HANDSHAKING;

    public ProxyConnection(final Supplier<ChannelHandler> handlerSupplier, final Function<Supplier<ChannelHandler>, ChannelInitializer<Channel>> channelInitializerSupplier, final Channel c2p) {
        super(handlerSupplier, channelInitializerSupplier);
        this.c2p = c2p;
    }

    public static ProxyConnection fromChannel(final Channel channel) {
        return channel.attr(PROXY_CONNECTION_ATTRIBUTE_KEY).get();
    }

    public static ProxyConnection fromUserConnection(final UserConnection userConnection) {
        return fromChannel(userConnection.getChannel());
    }

    @Override
    @Deprecated
    public ChannelFuture connect(final SocketAddress address) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initialize(final ChannelType channelType, final Bootstrap bootstrap) {
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ViaProxy.getConfig().getConnectTimeout());
        bootstrap.attr(PROXY_CONNECTION_ATTRIBUTE_KEY, this);
        super.initialize(channelType, bootstrap);
    }

    public ChannelFuture connectToServer(final SocketAddress serverAddress, final ProtocolVersion targetVersion) {
        this.serverAddress = serverAddress;
        this.serverVersion = targetVersion;
        return super.connect(serverAddress);
    }

    public Channel getC2P() {
        return this.c2p;
    }

    public List<PacketHandler> getPacketHandlers() {
        return this.packetHandlers;
    }

    public <T> T getPacketHandler(final Class<T> packetHandlerType) {
        for (final PacketHandler packetHandler : this.packetHandlers) {
            if (packetHandlerType.isInstance(packetHandler)) {
                return packetHandlerType.cast(packetHandler);
            }
        }
        return null;
    }

    public SocketAddress getServerAddress() {
        return this.serverAddress;
    }

    public ProtocolVersion getServerVersion() {
        return this.serverVersion;
    }

    public ProtocolVersion getClientVersion() {
        return this.clientVersion;
    }

    public void setClientVersion(final ProtocolVersion clientVersion) {
        this.clientVersion = clientVersion;
    }

    public HostAndPort getClientHandshakeAddress() {
        return this.clientHandshakeAddress;
    }

    public void setClientHandshakeAddress(final HostAndPort clientHandshakeAddress) {
        this.clientHandshakeAddress = clientHandshakeAddress;
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    public void setGameProfile(final GameProfile gameProfile) {
        this.gameProfile = gameProfile;
    }

    public C2SLoginHelloPacket1_7 getLoginHelloPacket() {
        return this.loginHelloPacket;
    }

    public void setLoginHelloPacket(final C2SLoginHelloPacket1_7 loginHelloPacket) {
        this.loginHelloPacket = loginHelloPacket;
    }

    public void setKeyForPreNettyEncryption(final Key key) {
        this.storedSecretKey = key;
    }

    public void enablePreNettyEncryption() throws GeneralSecurityException {
        this.getChannel().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(new AESEncryption(this.storedSecretKey));
    }

    public UserConnection getUserConnection() {
        return this.userConnection;
    }

    public void setUserConnection(final UserConnection userConnection) {
        this.userConnection = userConnection;
    }

    public UserOptions getUserOptions() {
        return this.userOptions;
    }

    public void setUserOptions(final UserOptions userOptions) {
        this.userOptions = userOptions;
    }

    public ConnectionState getC2pConnectionState() {
        return this.c2pConnectionState;
    }

    public ConnectionState getP2sConnectionState() {
        return this.p2sConnectionState;
    }

    public void setC2pConnectionState(final ConnectionState connectionState) {
        this.c2pConnectionState = connectionState;
        switch (connectionState) {
            case HANDSHAKING:
                this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getHandshakingRegistry(false));
                break;
            case STATUS:
                this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getStatusRegistry(false));
                break;
            case LOGIN:
                this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getLoginRegistry(false, this.clientVersion.getVersion()));
                break;
            case CONFIGURATION:
                this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getConfigurationRegistry(false, this.clientVersion.getVersion()));
                break;
            case PLAY:
                this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getPlayRegistry(false, this.clientVersion.getVersion()));
                break;
        }
    }

    public void setP2sConnectionState(final ConnectionState connectionState) {
        this.p2sConnectionState = connectionState;
        switch (connectionState) {
            case HANDSHAKING:
                if (this.getChannel() != null)
                    this.getChannel().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getHandshakingRegistry(true));
                break;
            case STATUS:
                if (this.getChannel() != null)
                    this.getChannel().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getStatusRegistry(true));
                break;
            case LOGIN:
                if (this.getChannel() != null)
                    this.getChannel().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getLoginRegistry(true, this.clientVersion.getVersion()));
                break;
            case CONFIGURATION:
                if (this.getChannel() != null)
                    this.getChannel().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getConfigurationRegistry(true, this.clientVersion.getVersion()));
                break;
            case PLAY:
                if (this.getChannel() != null)
                    this.getChannel().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getPlayRegistry(true, this.clientVersion.getVersion()));
                break;
        }
    }

    public void kickClient(final String message) throws CloseAndReturn {
        Logger.u_err("proxy kick", this, ConsoleFormatter.convert(message));

        final ChannelFuture future;
        if (this.c2pConnectionState == ConnectionState.STATUS) {
            future = this.c2p.writeAndFlush(new S2CStatusResponsePacket("{\"players\":{\"max\":0,\"online\":0},\"description\":" + new JsonPrimitive(message) + ",\"version\":{\"protocol\":-1,\"name\":\"ViaProxy\"}}"));
        } else if (this.c2pConnectionState == ConnectionState.LOGIN) {
            future = this.c2p.writeAndFlush(new S2CLoginDisconnectPacket1_20_3(new StringComponent(message)));
        } else if (this.c2pConnectionState == ConnectionState.CONFIGURATION) {
            final ByteBuf disconnectPacket = Unpooled.buffer();
            PacketTypes.writeVarInt(disconnectPacket, MCPackets.S2C_CONFIG_DISCONNECT.getId(this.clientVersion.getVersion()));
            if (this.clientVersion.olderThanOrEqualTo(ProtocolVersion.v1_20_2)) {
                PacketTypes.writeString(disconnectPacket, messageToJson(message));
            } else {
                PacketTypes.writeUnnamedTag(disconnectPacket, messageToNbt(message));
            }
            future = this.c2p.writeAndFlush(disconnectPacket);
        } else if (this.c2pConnectionState == ConnectionState.PLAY) {
            final ByteBuf disconnectPacket = Unpooled.buffer();
            PacketTypes.writeVarInt(disconnectPacket, MCPackets.S2C_DISCONNECT.getId(this.clientVersion.getVersion()));
            if (this.clientVersion.olderThanOrEqualTo(ProtocolVersion.v1_20_2)) {
                PacketTypes.writeString(disconnectPacket, messageToJson(message));
            } else {
                PacketTypes.writeUnnamedTag(disconnectPacket, messageToNbt(message));
            }
            future = this.c2p.writeAndFlush(disconnectPacket);
        } else {
            future = this.c2p.newSucceededFuture();
        }

        future.addListener(ChannelFutureListener.CLOSE);
        throw CloseAndReturn.INSTANCE;
    }

    public boolean isClosed() {
        return !this.c2p.isOpen() || (this.getChannel() != null && !this.getChannel().isOpen());
    }

    private static String messageToJson(final String message) {
        final JsonObject obj = new JsonObject();
        obj.addProperty("text", message);
        return obj.toString();
    }

    private static INbtTag messageToNbt(final String message) {
        final CompoundTag tag = new CompoundTag();
        tag.add("text", new StringTag(message));
        return tag;
    }

}
