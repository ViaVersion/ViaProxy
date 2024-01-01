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
package net.raphimc.viaproxy.proxy.packethandler;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.PlayerMessageSignature;
import com.viaversion.viaversion.api.minecraft.signature.model.MessageMetadata;
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_3;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.BitSetType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import net.raphimc.netminecraft.constants.ConnectionState;
import net.raphimc.netminecraft.constants.MCPackets;
import net.raphimc.netminecraft.constants.MCPipeline;
import net.raphimc.netminecraft.packet.IPacket;
import net.raphimc.netminecraft.packet.PacketTypes;
import net.raphimc.netminecraft.packet.UnknownPacket;
import net.raphimc.viaproxy.proxy.session.ProxyConnection;

import java.util.BitSet;
import java.util.List;

public class ChatSignaturePacketHandler extends PacketHandler {

    private final int joinGameId;
    private final int chatSessionUpdateId;
    private final int chatMessageId;

    public ChatSignaturePacketHandler(ProxyConnection proxyConnection) {
        super(proxyConnection);

        this.joinGameId = MCPackets.S2C_JOIN_GAME.getId(proxyConnection.getClientVersion().getVersion());
        this.chatSessionUpdateId = MCPackets.C2S_CHAT_SESSION_UPDATE.getId(proxyConnection.getClientVersion().getVersion());
        this.chatMessageId = MCPackets.C2S_CHAT_MESSAGE.getId(proxyConnection.getClientVersion().getVersion());
    }

    @Override
    public boolean handleC2P(IPacket packet, List<ChannelFutureListener> listeners) throws Exception {
        if (packet instanceof UnknownPacket unknownPacket && this.proxyConnection.getC2pConnectionState() == ConnectionState.PLAY) {
            final UserConnection user = this.proxyConnection.getUserConnection();

            if (unknownPacket.packetId == this.chatSessionUpdateId && (!this.isP2sEncrypted() || user.has(ChatSession1_19_3.class))) {
                return false;
            } else if (unknownPacket.packetId == this.chatMessageId && user.has(ChatSession1_19_3.class)) {
                final ChatSession1_19_3 chatSession = user.get(ChatSession1_19_3.class);

                final ByteBuf oldChatMessage = Unpooled.wrappedBuffer(unknownPacket.data);
                final String message = PacketTypes.readString(oldChatMessage, 256); // message
                final long timestamp = oldChatMessage.readLong(); // timestamp
                final long salt = oldChatMessage.readLong(); // salt

                final MessageMetadata metadata = new MessageMetadata(null, timestamp, salt);
                final byte[] signature = chatSession.signChatMessage(metadata, message, new PlayerMessageSignature[0]);

                final ByteBuf newChatMessage = Unpooled.buffer();
                PacketTypes.writeVarInt(newChatMessage, this.chatMessageId);
                PacketTypes.writeString(newChatMessage, message); // message
                newChatMessage.writeLong(timestamp); // timestamp
                newChatMessage.writeLong(salt); // salt
                Type.OPTIONAL_SIGNATURE_BYTES.write(newChatMessage, signature); // signature
                PacketTypes.writeVarInt(newChatMessage, 0); // offset
                new BitSetType(20).write(newChatMessage, new BitSet(20)); // acknowledged
                this.proxyConnection.getChannel().writeAndFlush(newChatMessage).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                return false;
            }
        }

        return true;
    }

    @Override
    public boolean handleP2S(IPacket packet, List<ChannelFutureListener> listeners) {
        if (packet instanceof UnknownPacket unknownPacket && this.proxyConnection.getC2pConnectionState() == ConnectionState.PLAY) {
            final UserConnection user = this.proxyConnection.getUserConnection();

            if (unknownPacket.packetId == this.joinGameId && this.isP2sEncrypted() && user.has(ChatSession1_19_3.class)) {
                final ChatSession1_19_3 chatSession = user.get(ChatSession1_19_3.class);
                listeners.add(f -> {
                    if (f.isSuccess()) {
                        final ByteBuf chatSessionUpdate = Unpooled.buffer();
                        PacketTypes.writeVarInt(chatSessionUpdate, this.chatSessionUpdateId);
                        PacketTypes.writeUuid(chatSessionUpdate, chatSession.getSessionId()); // session id
                        Type.PROFILE_KEY.write(chatSessionUpdate, chatSession.getProfileKey()); // profile key
                        this.proxyConnection.getChannel().writeAndFlush(chatSessionUpdate).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    }
                });
            }
        }

        return true;
    }

    private boolean isP2sEncrypted() {
        return this.proxyConnection.getChannel().attr(MCPipeline.ENCRYPTION_ATTRIBUTE_KEY).get() != null;
    }

}
