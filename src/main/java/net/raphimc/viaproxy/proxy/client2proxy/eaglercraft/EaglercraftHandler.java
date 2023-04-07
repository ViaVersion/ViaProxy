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
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.util.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    private int protocolVersion;
    private String username;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.attr(MCPipeline.COMPRESSION_THRESHOLD_ATTRIBUTE_KEY).set(-2); // Disable automatic compression in Proxy2ServerHandler
        ctx.pipeline().remove(MCPipeline.SIZER_HANDLER_NAME);
        super.channelActive(ctx);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (this.state != State.LOGIN_COMPLETE) {
            throw new IllegalStateException("Cannot send packets before login is completed");
        }

        out.add(new BinaryWebSocketFrame(in.retain()));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame in, List<Object> out) {
        if (in instanceof BinaryWebSocketFrame) {
            final ByteBuf data = in.content();

            switch (this.state) {
                case PRE_HANDSHAKE:
                    if (data.readableBytes() >= 2) { // Check for legacy client
                        if (data.getByte(0) == (byte) 2 && data.getByte(1) == (byte) 69) {
                            throw new IllegalStateException("Your client is not yet supported");
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

                        this.state = State.HANDSHAKE_COMPLETE;
                        this.protocolVersion = minecraftVersion;
                        Logger.LOGGER.info("Eaglercraft client connected: " + clientBrand + " " + clientVersionString);
                        final String serverBrand = "ViaProxy";
                        final String serverVersionString = ViaProxy.VERSION;

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

                        final int handshakeId = MCPackets.C2S_HANDSHAKE.getId(this.protocolVersion);
                        final int loginHelloId = MCPackets.C2S_LOGIN_HELLO.getId(this.protocolVersion);

                        if (handshakeId == -1 || loginHelloId == -1) {
                            Logger.LOGGER.error("Unsupported protocol version: " + this.protocolVersion);
                            ctx.close();
                            return;
                        }

                        final ByteBuf handshake = ctx.alloc().buffer();
                        PacketTypes.writeVarInt(handshake, handshakeId); // packet id
                        PacketTypes.writeVarInt(handshake, this.protocolVersion); // protocol version
                        PacketTypes.writeString(handshake, this.host.getHost()); // address
                        handshake.writeShort(this.host.getPort()); // port
                        PacketTypes.writeVarInt(handshake, ConnectionState.LOGIN.getId()); // next state
                        out.add(handshake);

                        final ByteBuf loginHello = ctx.alloc().buffer();
                        PacketTypes.writeVarInt(loginHello, loginHelloId); // packet id
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
                    out.add(data.retain());
                    break;
                default:
                    throw new IllegalStateException("Unexpected binary frame in state " + this.state);
            }
        } else if (in instanceof TextWebSocketFrame) {
            final String text = ((TextWebSocketFrame) in).text();
            ctx.close();

            if (this.state != State.PRE_HANDSHAKE) {
                throw new IllegalStateException("Unexpected text frame in state " + this.state);
            }
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

    public enum State {
        PRE_HANDSHAKE,
        HANDSHAKE,
        HANDSHAKE_COMPLETE,
        LOGIN,
        LOGIN_COMPLETE,
    }

}
