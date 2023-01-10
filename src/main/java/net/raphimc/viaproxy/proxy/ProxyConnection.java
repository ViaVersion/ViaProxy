package net.raphimc.viaproxy.proxy;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.netty.connection.NetClient;
import net.raphimc.netminecraft.netty.crypto.AESEncryption;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.impl.login.C2SLoginHelloPacket1_7;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginCustomPayloadPacket;
import net.raphimc.netminecraft.packet.impl.login.S2CLoginDisconnectPacket;
import net.raphimc.netminecraft.packet.impl.status.S2CStatusResponsePacket;
import net.raphimc.netminecraft.packet.registry.PacketRegistryUtil;
import net.raphimc.netminecraft.util.ServerAddress;
import net.raphimc.viaprotocolhack.util.VersionEnum;
import net.raphimc.viaproxy.proxy.util.CloseAndReturn;
import net.raphimc.viaproxy.util.logging.Logger;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProxyConnection extends NetClient {

    public static final AttributeKey<ProxyConnection> PROXY_CONNECTION_ATTRIBUTE_KEY = AttributeKey.valueOf("proxy_connection");

    private final SocketChannel c2p;
    private final AtomicInteger customPayloadId = new AtomicInteger(0);
    private final Map<Integer, CompletableFuture<ByteBuf>> customPayloadListener = new ConcurrentHashMap<>();

    private ServerAddress serverAddress;
    private VersionEnum serverVersion;

    private VersionEnum clientVersion;
    private GameProfile gameProfile;
    private C2SLoginHelloPacket1_7 loginHelloPacket;
    private UserConnection userConnection;
    private ConnectionState connectionState = ConnectionState.HANDSHAKING;

    private Key storedSecretKey;
    private String classicMpPass;

    public ProxyConnection(final Supplier<ChannelHandler> handlerSupplier, final Function<Supplier<ChannelHandler>, ChannelInitializer<SocketChannel>> channelInitializerSupplier, final SocketChannel c2p) {
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
    public void connect(final ServerAddress serverAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initialize(final Bootstrap bootstrap) {
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 4_000);
        bootstrap.attr(ProxyConnection.PROXY_CONNECTION_ATTRIBUTE_KEY, this);
        super.initialize(bootstrap);
    }

    public void connectToServer(final ServerAddress serverAddress, final VersionEnum targetVersion) {
        this.serverAddress = serverAddress;
        this.serverVersion = targetVersion;
        super.connect(serverAddress);
    }

    public SocketChannel getC2P() {
        return this.c2p;
    }

    public ServerAddress getServerAddress() {
        return this.serverAddress;
    }

    public VersionEnum getServerVersion() {
        return this.serverVersion;
    }

    public void setKeyForPreNettyEncryption(Key key) {
        this.storedSecretKey = key;
    }

    public void enablePreNettyEncryption() throws GeneralSecurityException {
        this.getChannel().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).set(new AESEncryption(this.storedSecretKey));
    }

    public void setClientVersion(VersionEnum clientVersion) {
        this.clientVersion = clientVersion;
    }

    public VersionEnum getClientVersion() {
        return this.clientVersion;
    }

    public void setGameProfile(GameProfile gameProfile) {
        this.gameProfile = gameProfile;
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    public void setLoginHelloPacket(final C2SLoginHelloPacket1_7 loginHelloPacket) {
        this.loginHelloPacket = loginHelloPacket;
    }

    public C2SLoginHelloPacket1_7 getLoginHelloPacket() {
        return this.loginHelloPacket;
    }

    public void setUserConnection(UserConnection userConnection) {
        this.userConnection = userConnection;
    }

    public UserConnection getUserConnection() {
        return this.userConnection;
    }

    public void setConnectionState(ConnectionState connectionState) {
        this.connectionState = connectionState;
        switch (this.connectionState) {
            case HANDSHAKING:
                if (this.getChannel() != null)
                    this.getChannel().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getHandshakeRegistry(true));
                this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getHandshakeRegistry(false));
                break;
            case STATUS:
                if (this.getChannel() != null)
                    this.getChannel().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getStatusRegistry(true));
                this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getStatusRegistry(false));
                break;
            case LOGIN:
                if (this.getChannel() != null)
                    this.getChannel().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getLoginRegistry(true, this.clientVersion.getVersion()));
                this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getLoginRegistry(false, this.clientVersion.getVersion()));
                break;
            case PLAY:
                if (this.getChannel() != null)
                    this.getChannel().attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getPlayRegistry(true, this.clientVersion.getVersion()));
                this.c2p.attr(MCPipeline.PACKET_REGISTRY_ATTRIBUTE_KEY).set(PacketRegistryUtil.getPlayRegistry(false, this.clientVersion.getVersion()));
                break;
        }
    }

    public ConnectionState getConnectionState() {
        return this.connectionState;
    }

    public CompletableFuture<ByteBuf> sendCustomPayload(final String channel, final ByteBuf data) {
        if (channel.length() > 20) throw new IllegalStateException("Channel name can't be longer than 20 characters");
        final CompletableFuture<ByteBuf> future = new CompletableFuture<>();
        final int id = this.customPayloadId.getAndIncrement();

        switch (this.connectionState) {
            case LOGIN:
                if (this.clientVersion.isNewerThanOrEqualTo(VersionEnum.r1_13)) {
                    this.c2p.writeAndFlush(new S2CLoginCustomPayloadPacket(id, channel, PacketTypes.readReadableBytes(data))).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                } else {
                    final ByteBuf disconnectPacketData = Unpooled.buffer();
                    PacketTypes.writeString(disconnectPacketData, channel);
                    PacketTypes.writeVarInt(disconnectPacketData, id);
                    disconnectPacketData.writeBytes(data);
                    this.c2p.writeAndFlush(new S2CLoginDisconnectPacket(messageToJson("§cYou need to install OpenAuthMod in order to join this server.§k\n" + Base64.getEncoder().encodeToString(ByteBufUtil.getBytes(disconnectPacketData)) + "\n" + ExternalInterface.OPENAUTHMOD_LEGACY_MAGIC_STRING))).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                }
                break;
            case PLAY:
                final ByteBuf customPayloadPacket = Unpooled.buffer();
                PacketTypes.writeVarInt(customPayloadPacket, MCPackets.S2C_PLUGIN_MESSAGE.getId(this.clientVersion.getVersion()));
                PacketTypes.writeString(customPayloadPacket, channel); // channel
                PacketTypes.writeVarInt(customPayloadPacket, id);
                customPayloadPacket.writeBytes(data);
                this.c2p.writeAndFlush(customPayloadPacket).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                break;
            default:
                throw new IllegalStateException("Can't send a custom payload packet during " + this.connectionState);
        }

        this.customPayloadListener.put(id, future);
        return future;
    }

    public boolean handleCustomPayload(final int id, final ByteBuf data) {
        if (this.customPayloadListener.containsKey(id)) {
            this.customPayloadListener.remove(id).complete(data);
            return true;
        }
        return false;
    }

    public void setClassicMpPass(final String classicMpPass) {
        this.classicMpPass = classicMpPass;
    }

    public String getClassicMpPass() {
        return this.classicMpPass;
    }

    public void kickClient(final String message) throws InterruptedException, CloseAndReturn {
        Logger.u_err("kick", this.c2p.remoteAddress(), this.getGameProfile(), message.replaceAll("§.", ""));

        final ChannelFuture future;
        if (this.connectionState == ConnectionState.LOGIN) {
            future = this.c2p.writeAndFlush(new S2CLoginDisconnectPacket(messageToJson(message)));
        } else if (this.connectionState == ConnectionState.PLAY) {
            final ByteBuf disconnectPacket = Unpooled.buffer();
            PacketTypes.writeVarInt(disconnectPacket, MCPackets.S2C_DISCONNECT.getId(this.clientVersion.getVersion()));
            PacketTypes.writeString(disconnectPacket, messageToJson(message));
            future = this.c2p.writeAndFlush(disconnectPacket);
        } else if (this.connectionState == ConnectionState.STATUS) {
            future = this.c2p.writeAndFlush(new S2CStatusResponsePacket("{\"players\":{\"max\":0,\"online\":0},\"description\":" + new JsonPrimitive(message) + ",\"version\":{\"protocol\":-1,\"name\":\"ViaProxy\"}}"));
        } else {
            future = this.c2p.newSucceededFuture();
        }

        future.await().channel().close();
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

}
