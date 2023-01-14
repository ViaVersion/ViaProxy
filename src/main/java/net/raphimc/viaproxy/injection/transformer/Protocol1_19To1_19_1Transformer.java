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
package net.raphimc.viaproxy.injection.transformer;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.protocol.protocol1_19to1_19_1.Protocol1_19To1_19_1;
import com.viaversion.viabackwards.protocol.protocol1_19to1_19_1.storage.ReceivedMessagesStorage;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ClientboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ServerboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ClientboundPackets1_19;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ServerboundPackets1_19;
import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.DecoratableMessage;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.MessageMetadata;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.storage.ChatSession1_19_1;

import java.util.UUID;

@CTransformer(Protocol1_19To1_19_1.class)
public abstract class Protocol1_19To1_19_1Transformer extends BackwardsProtocol<ClientboundPackets1_19_1, ClientboundPackets1_19, ServerboundPackets1_19_1, ServerboundPackets1_19> {

    @CShadow
    private static byte[] EMPTY_BYTES;

    @CInject(method = "registerPackets", target = @CTarget("RETURN"))
    private void allowSignatures(InjectionCallback ic) {
        this.registerServerbound(State.LOGIN, ServerboundLoginPackets.HELLO.getId(), ServerboundLoginPackets.HELLO.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Name
                read(Type.OPTIONAL_PROFILE_KEY); // Profile Key
                handler(wrapper -> {
                    final ChatSession1_19_1 chatSession = wrapper.user().get(ChatSession1_19_1.class);
                    wrapper.write(Type.OPTIONAL_PROFILE_KEY, chatSession == null ? null : chatSession.getProfileKey()); // Profile Key
                    wrapper.write(Type.OPTIONAL_UUID, chatSession == null ? null : chatSession.getUuid()); // Profile uuid
                });
            }
        }, true);
        this.registerClientbound(State.LOGIN, ClientboundLoginPackets.HELLO.getId(), ClientboundLoginPackets.HELLO.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
            }
        }, true);
        this.registerServerbound(State.LOGIN, ServerboundLoginPackets.ENCRYPTION_KEY.getId(), ServerboundLoginPackets.ENCRYPTION_KEY.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
            }
        }, true);

        this.registerServerbound(ServerboundPackets1_19.CHAT_MESSAGE, ServerboundPackets1_19_1.CHAT_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Message
                map(Type.LONG); // Timestamp
                map(Type.LONG); // Salt
                read(Type.BYTE_ARRAY_PRIMITIVE); // Signature
                read(Type.BOOLEAN); // Signed preview
                handler(wrapper -> {
                    final ChatSession1_19_1 chatSession = wrapper.user().get(ChatSession1_19_1.class);
                    final ReceivedMessagesStorage messagesStorage = wrapper.user().get(ReceivedMessagesStorage.class);

                    if (chatSession != null) {
                        final UUID sender = wrapper.user().getProtocolInfo().getUuid();
                        final String message = wrapper.get(Type.STRING, 0);
                        final long timestamp = wrapper.get(Type.LONG, 0);
                        final long salt = wrapper.get(Type.LONG, 1);

                        final MessageMetadata metadata = new MessageMetadata(sender, timestamp, salt);
                        final DecoratableMessage decoratableMessage = new DecoratableMessage(message);

                        wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, chatSession.signChatMessage(metadata, decoratableMessage, messagesStorage.lastSignatures())); // Signature
                        wrapper.write(Type.BOOLEAN, decoratableMessage.isDecorated()); // Signed preview
                    } else {
                        wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, EMPTY_BYTES); // Signature
                        wrapper.write(Type.BOOLEAN, false); // Signed preview
                    }

                    messagesStorage.resetUnacknowledgedCount();
                    wrapper.write(Type.PLAYER_MESSAGE_SIGNATURE_ARRAY, messagesStorage.lastSignatures());
                    wrapper.write(Type.OPTIONAL_PLAYER_MESSAGE_SIGNATURE, null); // No last unacknowledged
                });
            }
        }, true);
    }

}
