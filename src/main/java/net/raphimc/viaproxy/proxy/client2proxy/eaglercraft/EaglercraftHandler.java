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
package net.raphimc.viaproxy.proxy.client2proxy.eaglercraft;

import com.google.common.net.HostAndPort;
import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonParser;
import com.viaversion.viaversion.protocols.base.ClientboundStatusPackets;
import com.viaversion.viaversion.protocols.base.ServerboundHandshakePackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundStatusPackets;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import net.lenni0451.mcstructs.text.serializer.TextComponentSerializer;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.vialegacy.protocols.release.protocol1_6_1to1_5_2.ServerboundPackets1_5_2;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.types.Types1_6_4;
import net.raphimc.viaprotocolhack.util.VersionEnum;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.proxy.client2proxy.Client2ProxyChannelInitializer;
import net.raphimc.viaproxy.util.logging.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

// Thanks to ayunami2000 for helping with the eaglercraft protocol
public class EaglercraftHandler extends MessageToMessageCodec<WebSocketFrame, ByteBuf> {

    private static final int CLIENT_VERSION = 0x01;
    private static final int SERVER_VERSION = 0x02;
    private static final int VERSION_MISMATCH = 0x03;
    private static final int CLIENT_REQUEST_LOGIN = 0x04;
    private static final int SERVER_ALLOW_LOGIN = 0x05;
    private static final int SERVER_DENY_LOGIN = 0x06;
    private static final int CLIENT_PROFILE_DATA = 0x07;
    private static final int CLIENT_FINISH_LOGIN = 0x08;
    private static final int SERVER_FINISH_LOGIN = 0x09;
    private static final int SERVER_ERROR = 0xFF;

    private HostAndPort host;
    private State state = State.PRE_HANDSHAKE;
    private VersionEnum version;
    private int pluginMessageId;
    private String username;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(-2); // Disable automatic compression in Proxy2ServerHandler
        ctx.pipeline().remove(MCPipeline.SIZER_HANDLER_NAME);
        super.channelActive(ctx);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws IOException {
        if (this.state == State.STATUS) {
            final int packetId = PacketTypes.readVarInt(in);
            if (packetId != ClientboundStatusPackets.STATUS_RESPONSE.getId()) {
                throw new IllegalStateException("Unexpected packet id " + packetId);
            }
            final JsonObject root = JsonParser.parseString(PacketTypes.readString(in, Short.MAX_VALUE)).getAsJsonObject();

            final JsonObject response = new JsonObject();
            response.addProperty("name", "ViaProxy");
            response.addProperty("brand", "ViaProxy");
            if (root.has("version")) {
                response.add("vers", root.getAsJsonObject("version").get("name"));
            } else {
                response.addProperty("vers", "Unknown");
            }
            response.addProperty("cracked", true);
            response.addProperty("secure", false);
            response.addProperty("time", System.currentTimeMillis());
            response.addProperty("uuid", UUID.randomUUID().toString());
            response.addProperty("type", "motd");

            final JsonObject data = new JsonObject();
            data.addProperty("cache", false);
            final JsonArray motd = new JsonArray();
            if (root.has("description")) {
                final String[] motdLines = TextComponentSerializer.V1_8.deserialize(root.get("description").toString()).asLegacyFormatString().split("\n");
                for (String motdLine : motdLines) {
                    motd.add(motdLine);
                }
            }
            data.add("motd", motd);
            data.addProperty("icon", root.has("favicon"));
            if (root.has("players")) {
                final JsonObject javaPlayers = root.getAsJsonObject("players");
                data.add("online", javaPlayers.get("online"));
                data.add("max", javaPlayers.get("max"));
                final JsonArray players = new JsonArray();
                if (javaPlayers.has("sample")) {
                    javaPlayers.getAsJsonArray("sample").forEach(player -> players.add(TextComponentSerializer.V1_8.deserialize(player.getAsJsonObject().get("name").getAsString()).asLegacyFormatString()));
                }
                data.add("players", players);
            }
            response.add("data", data);
            out.add(new TextWebSocketFrame(response.toString()));

            if (root.has("favicon")) {
                final BufferedImage icon = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(root.get("favicon").getAsString().substring(22).replace("\n", "").getBytes(StandardCharsets.UTF_8))));
                final int[] pixels = icon.getRGB(0, 0, 64, 64, null, 0, 64);
                final byte[] iconPixels = new byte[64 * 64 * 4];
                for (int i = 0; i < 64 * 64; ++i) {
                    iconPixels[i * 4] = (byte) ((pixels[i] >> 16) & 0xFF);
                    iconPixels[i * 4 + 1] = (byte) ((pixels[i] >> 8) & 0xFF);
                    iconPixels[i * 4 + 2] = (byte) (pixels[i] & 0xFF);
                    iconPixels[i * 4 + 3] = (byte) ((pixels[i] >> 24) & 0xFF);
                }
                out.add(new BinaryWebSocketFrame(ctx.alloc().buffer().writeBytes(iconPixels)));
            }
        } else if (this.state == State.LOGIN_COMPLETE) {
            out.add(new BinaryWebSocketFrame(in.retain()));
        } else {
            throw new IllegalStateException("Cannot send packets before login is completed");
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame in, List<Object> out) throws Exception {
        if (in instanceof BinaryWebSocketFrame) {
            final ByteBuf data = in.content();

            switch (this.state) {
                case PRE_HANDSHAKE:
                    if (data.readableBytes() >= 2) { // Check for legacy client
                        if (data.getByte(0) == (byte) 2 && data.getByte(1) == (byte) 69) {
                            data.setByte(1, 61); // 1.5.2 protocol id
                            this.state = State.LOGIN_COMPLETE;
                            this.version = VersionEnum.r1_5_2;
                            out.add(data.retain());
                            break;
                        }
                    }
                    this.state = State.HANDSHAKE;
                case HANDSHAKE:
                    int packetId = data.readUnsignedByte(); // packet id
                    if (packetId == CLIENT_VERSION) {
                        int eaglercraftVersion = data.readUnsignedByte(); // eaglercraft version
                        final int minecraftVersion;
                        if (eaglercraftVersion == 1) {
                            minecraftVersion = data.readUnsignedByte(); // minecraft version
                        } else if (eaglercraftVersion == 2) {
                            int count = data.readUnsignedShort(); // eaglercraft versions
                            final List<Integer> eaglercraftVersions = new ArrayList<>(count);
                            for (int i = 0; i < count; i++) {
                                eaglercraftVersions.add(data.readUnsignedShort()); // eaglercraft version id
                            }
                            if (!eaglercraftVersions.contains(2) && !eaglercraftVersions.contains(3)) {
                                Logger.LOGGER.error("No supported eaglercraft versions found");
                                ctx.close();
                                return;
                            }

                            if (eaglercraftVersions.contains(3)) {
                                eaglercraftVersion = 3;
                            }

                            count = data.readUnsignedShort(); // minecraft versions
                            final List<Integer> minecraftVersions = new ArrayList<>(count);
                            for (int i = 0; i < count; i++) {
                                minecraftVersions.add(data.readUnsignedShort()); // minecraft version id
                            }
                            if (minecraftVersions.size() != 1) {
                                Logger.LOGGER.error("No supported minecraft versions found");
                                ctx.close();
                            }
                            minecraftVersion = minecraftVersions.get(0);
                        } else {
                            throw new IllegalArgumentException("Unknown Eaglercraft version: " + eaglercraftVersion);
                        }
                        final String clientBrand = data.readCharSequence(data.readUnsignedByte(), StandardCharsets.US_ASCII).toString(); // client brand
                        final String clientVersionString = data.readCharSequence(data.readUnsignedByte(), StandardCharsets.US_ASCII).toString(); // client version

                        if (eaglercraftVersion >= 2) {
                            data.skipBytes(1); // auth enabled
                            data.skipBytes(data.readUnsignedByte()); // auth username
                        }
                        if (data.isReadable()) {
                            throw new IllegalArgumentException("Too much data in packet: " + data.readableBytes() + " bytes");
                        }

                        Logger.LOGGER.info("Eaglercraft client connected: " + clientBrand + " " + clientVersionString);
                        final String serverBrand = "ViaProxy";
                        final String serverVersionString = ViaProxy.VERSION;
                        this.state = State.HANDSHAKE_COMPLETE;
                        this.version = VersionEnum.fromProtocolId(minecraftVersion);
                        if (this.version.equals(VersionEnum.UNKNOWN)) {
                            Logger.LOGGER.error("Unsupported protocol version: " + minecraftVersion);
                            ctx.close();
                            return;
                        }

                        final ByteBuf response = ctx.alloc().buffer();
                        response.writeByte(SERVER_VERSION); // packet id
                        if (eaglercraftVersion == 1) {
                            response.writeByte(1); // eaglercraft version
                        } else {
                            response.writeShort(eaglercraftVersion); // eaglercraft version
                            response.writeShort(minecraftVersion); // minecraft version
                        }
                        response.writeByte(serverBrand.length()).writeCharSequence(serverBrand, StandardCharsets.US_ASCII); // server brand
                        response.writeByte(serverVersionString.length()).writeCharSequence(serverVersionString, StandardCharsets.US_ASCII); // server version
                        response.writeByte(0); // auth method
                        response.writeShort(0); // salt length
                        ctx.writeAndFlush(new BinaryWebSocketFrame(response));
                    } else {
                        throw new IllegalStateException("Unexpected packet id " + packetId + " in state " + this.state);
                    }
                    break;
                case HANDSHAKE_COMPLETE:
                    packetId = data.readUnsignedByte(); // packet id
                    if (packetId == CLIENT_REQUEST_LOGIN) {
                        final String username = data.readCharSequence(data.readUnsignedByte(), StandardCharsets.US_ASCII).toString(); // username
                        data.readCharSequence(data.readUnsignedByte(), StandardCharsets.US_ASCII).toString(); // requested server
                        data.skipBytes(data.readUnsignedByte()); // auth password
                        if (data.isReadable()) {
                            throw new IllegalArgumentException("Too much data in packet: " + data.readableBytes() + " bytes");
                        }

                        this.state = State.LOGIN;
                        this.username = username;
                        final UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));

                        final ByteBuf response = ctx.alloc().buffer();
                        response.writeByte(SERVER_ALLOW_LOGIN); // packet id
                        response.writeByte(username.length()).writeCharSequence(username, StandardCharsets.US_ASCII); // username
                        response.writeLong(uuid.getMostSignificantBits()).writeLong(uuid.getLeastSignificantBits()); // uuid
                        ctx.writeAndFlush(new BinaryWebSocketFrame(response));
                    } else {
                        throw new IllegalStateException("Unexpected packet id " + packetId + " in state " + this.state);
                    }
                    break;
                case LOGIN:
                    packetId = data.readUnsignedByte(); // packet id
                    if (packetId == CLIENT_PROFILE_DATA) {
                        final String type = data.readCharSequence(data.readUnsignedByte(), StandardCharsets.US_ASCII).toString();
                        final byte[] dataBytes = new byte[data.readUnsignedShort()];
                        data.readBytes(dataBytes);
                        if (data.isReadable()) {
                            throw new IllegalArgumentException("Too much data in packet: " + data.readableBytes() + " bytes");
                        }
                    } else if (packetId == CLIENT_FINISH_LOGIN) {
                        if (data.isReadable()) {
                            throw new IllegalArgumentException("Too much data in packet: " + data.readableBytes() + " bytes");
                        }
                        this.state = State.LOGIN_COMPLETE;

                        this.pluginMessageId = MCPackets.C2S_PLUGIN_MESSAGE.getId(this.version.getVersion());

                        if (this.pluginMessageId == -1) {
                            Logger.LOGGER.error("Unsupported protocol version: " + this.version.getVersion());
                            ctx.close();
                            return;
                        }

                        if (ctx.pipeline().get(Client2ProxyChannelInitializer.LEGACY_PASSTHROUGH_HANDLER_NAME) != null) {
                            ctx.pipeline().remove(Client2ProxyChannelInitializer.LEGACY_PASSTHROUGH_HANDLER_NAME);
                        }

                        out.add(this.writeHandshake(ctx.alloc().buffer(), ConnectionState.LOGIN));

                        final ByteBuf loginHello = ctx.alloc().buffer();
                        PacketTypes.writeVarInt(loginHello, ServerboundLoginPackets.HELLO.getId()); // packet id
                        PacketTypes.writeString(loginHello, this.username); // username
                        out.add(loginHello);

                        final ByteBuf response = ctx.alloc().buffer();
                        response.writeByte(SERVER_FINISH_LOGIN); // packet id
                        ctx.writeAndFlush(new BinaryWebSocketFrame(response));
                    } else {
                        throw new IllegalStateException("Unexpected packet id " + packetId + " in state " + this.state);
                    }
                    break;
                case LOGIN_COMPLETE:
                    if (this.version.equals(VersionEnum.r1_5_2)) {
                        packetId = data.readUnsignedByte();
                        if (packetId == ServerboundPackets1_5_2.SHARED_KEY.getId()) {
                            ctx.channel().writeAndFlush(new BinaryWebSocketFrame(data.readerIndex(0).retain()));
                            break;
                        } else if (packetId == ServerboundPackets1_5_2.PLUGIN_MESSAGE.getId()) {
                            if (Types1_6_4.STRING.read(data).startsWith("EAG|")) {
                                break;
                            }
                        }
                    } else if (this.version.isNewerThanOrEqualTo(VersionEnum.r1_7_2tor1_7_5)) {
                        packetId = PacketTypes.readVarInt(data);
                        if (packetId == this.pluginMessageId) {
                            if (PacketTypes.readString(data, Short.MAX_VALUE).startsWith("EAG|")) {
                                break;
                            }
                        }
                    }
                    out.add(data.readerIndex(0).retain());
                    break;
                default:
                    throw new IllegalStateException("Unexpected binary frame in state " + this.state);
            }
        } else if (in instanceof TextWebSocketFrame) {
            final String text = ((TextWebSocketFrame) in).text();
            if (this.state != State.PRE_HANDSHAKE) {
                throw new IllegalStateException("Unexpected text frame in state " + this.state);
            }
            if (!text.equalsIgnoreCase("accept: motd")) {
                ctx.close();
                return;
            }
            this.state = State.STATUS;
            this.version = VersionEnum.r1_8;

            if (ctx.pipeline().get(Client2ProxyChannelInitializer.LEGACY_PASSTHROUGH_HANDLER_NAME) != null) {
                ctx.pipeline().remove(Client2ProxyChannelInitializer.LEGACY_PASSTHROUGH_HANDLER_NAME);
            }

            out.add(this.writeHandshake(ctx.alloc().buffer(), ConnectionState.STATUS));

            final ByteBuf statusRequest = ctx.alloc().buffer();
            PacketTypes.writeVarInt(statusRequest, ServerboundStatusPackets.STATUS_REQUEST.getId()); // packet id
            out.add(statusRequest);
        } else {
            throw new UnsupportedOperationException("Unsupported frame type: " + in.getClass().getName());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            final WebSocketServerProtocolHandler.HandshakeComplete handshake = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            if (!handshake.requestHeaders().contains("Host")) {
                ctx.close();
                return;
            }

            this.host = HostAndPort.fromString(handshake.requestHeaders().get("Host")).withDefaultPort(80);
        }

        super.userEventTriggered(ctx, evt);
    }

    private ByteBuf writeHandshake(final ByteBuf byteBuf, final ConnectionState state) {
        PacketTypes.writeVarInt(byteBuf, ServerboundHandshakePackets.CLIENT_INTENTION.getId()); // packet id
        PacketTypes.writeVarInt(byteBuf, this.version.getVersion()); // protocol version
        PacketTypes.writeString(byteBuf, this.host.getHost()); // address
        byteBuf.writeShort(this.host.getPort()); // port
        PacketTypes.writeVarInt(byteBuf, state.getId()); // next state
        return byteBuf;
    }

    public enum State {
        STATUS,
        PRE_HANDSHAKE,
        HANDSHAKE,
        HANDSHAKE_COMPLETE,
        LOGIN,
        LOGIN_COMPLETE,
    }

}
