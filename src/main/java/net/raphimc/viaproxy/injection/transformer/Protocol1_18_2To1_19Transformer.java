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

import com.google.common.primitives.Longs;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.Protocol1_18_2To1_19;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.ClientboundPackets1_18;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.storage.NonceStorage;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ClientboundPackets1_19;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ServerboundPackets1_19;
import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.DecoratableMessage;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.MessageMetadata;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.storage.ChatSession1_19_0;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@CTransformer(Protocol1_18_2To1_19.class)
public abstract class Protocol1_18_2To1_19Transformer extends BackwardsProtocol<ClientboundPackets1_19, ClientboundPackets1_18, ServerboundPackets1_19, ServerboundPackets1_17> {

    @CShadow
    private static byte[] EMPTY_BYTES;

    @CInject(method = "registerPackets", target = @CTarget("RETURN"))
    private void allowSignatures(InjectionCallback ic) {
        this.registerServerbound(State.LOGIN, ServerboundLoginPackets.HELLO.getId(), ServerboundLoginPackets.HELLO.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Name
                handler(wrapper -> {
                    final ChatSession1_19_0 chatSession = wrapper.user().get(ChatSession1_19_0.class);
                    wrapper.write(Type.OPTIONAL_PROFILE_KEY, chatSession == null ? null : chatSession.getProfileKey()); // Profile Key
                });
            }
        }, true);
        this.registerClientbound(State.LOGIN, ClientboundLoginPackets.HELLO.getId(), ClientboundLoginPackets.HELLO.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Server id
                map(Type.BYTE_ARRAY_PRIMITIVE); // Public key
                handler(wrapper -> {
                    wrapper.user().put(new NonceStorage(wrapper.passthrough(Type.BYTE_ARRAY_PRIMITIVE))); // Nonce
                });
            }
        }, true);
        this.registerServerbound(State.LOGIN, ServerboundLoginPackets.ENCRYPTION_KEY.getId(), ServerboundLoginPackets.ENCRYPTION_KEY.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE_ARRAY_PRIMITIVE); // Public key
                handler(wrapper -> {
                    final ChatSession1_19_0 chatSession = wrapper.user().get(ChatSession1_19_0.class);

                    final byte[] verifyToken = wrapper.read(Type.BYTE_ARRAY_PRIMITIVE); // Verify token
                    wrapper.write(Type.BOOLEAN, chatSession == null); // is nonce
                    if (chatSession != null) {
                        final long salt = ThreadLocalRandom.current().nextLong();
                        final byte[] signature = chatSession.sign(signer -> {
                            signer.accept(wrapper.user().get(NonceStorage.class).nonce());
                            signer.accept(Longs.toByteArray(salt));
                        });
                        wrapper.write(Type.LONG, salt); // Salt
                        wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, signature); // Signature
                    } else {
                        wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, verifyToken); // Nonce
                    }
                });
            }
        }, true);

        this.registerServerbound(ServerboundPackets1_17.CHAT_MESSAGE, ServerboundPackets1_19.CHAT_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Message
                handler(wrapper -> wrapper.write(Type.LONG, Instant.now().toEpochMilli())); // Timestamp
                create(Type.LONG, 0L); // Salt
                handler(wrapper -> {
                    final ChatSession1_19_0 chatSession = wrapper.user().get(ChatSession1_19_0.class);

                    final String message = wrapper.get(Type.STRING, 0);
                    if (!message.isEmpty() && message.charAt(0) == '/') {
                        wrapper.setPacketType(ServerboundPackets1_19.CHAT_COMMAND);
                        wrapper.set(Type.STRING, 0, message.substring(1));
                        wrapper.write(Type.VAR_INT, 0); // No signatures
                    } else {
                        if (chatSession != null) {
                            final UUID sender = wrapper.user().getProtocolInfo().getUuid();
                            final Instant timestamp = Instant.now();
                            final long salt = ThreadLocalRandom.current().nextLong();

                            final MessageMetadata metadata = new MessageMetadata(sender, timestamp, salt);
                            final DecoratableMessage decoratableMessage = new DecoratableMessage(message);
                            wrapper.set(Type.LONG, 0, timestamp.toEpochMilli()); // Timestamp
                            wrapper.set(Type.LONG, 1, salt); // Salt
                            wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, chatSession.signChatMessage(metadata, decoratableMessage)); // Signature
                        } else {
                            wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, EMPTY_BYTES); // Signature
                        }
                    }

                    wrapper.write(Type.BOOLEAN, false); // No signed preview
                });
            }
        }, true);
    }

}
