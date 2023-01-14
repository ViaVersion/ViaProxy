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
package net.raphimc.viaproxy.injection.mixins;

import com.google.common.primitives.Longs;
import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.BitSetType;
import com.viaversion.viaversion.api.type.types.ByteArrayType;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ClientboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ServerboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.storage.NonceStorage;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.Protocol1_19_3To1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.ServerboundPackets1_19_3;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.storage.ReceivedMessagesStorage;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.DecoratableMessage;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.MessageMetadata;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.storage.ChatSession1_19_1;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(value = Protocol1_19_3To1_19_1.class, remap = false)
public abstract class MixinProtocol1_19_3To1_19_1 extends AbstractProtocol<ClientboundPackets1_19_1, ClientboundPackets1_19_3, ServerboundPackets1_19_1, ServerboundPackets1_19_3> {

    @Final
    @Shadow
    private static ByteArrayType.OptionalByteArrayType OPTIONAL_MESSAGE_SIGNATURE_BYTES_TYPE;

    @Final
    @Shadow
    private static BitSetType ACKNOWLEDGED_BIT_SET_TYPE;

    @Final
    @Shadow
    private static byte[] EMPTY_BYTES;

    @Inject(method = "registerPackets", at = @At("RETURN"))
    private void allowSignatures(CallbackInfo ci) {
        this.registerServerbound(State.LOGIN, ServerboundLoginPackets.HELLO.getId(), ServerboundLoginPackets.HELLO.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Name
                handler(wrapper -> {
                    final ChatSession1_19_1 chatSession = wrapper.user().get(ChatSession1_19_1.class);
                    wrapper.write(Type.OPTIONAL_PROFILE_KEY, chatSession == null ? null : chatSession.getProfileKey()); // Profile Key
                });
                map(Type.OPTIONAL_UUID); // Profile uuid
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
        });
        this.registerServerbound(State.LOGIN, ServerboundLoginPackets.ENCRYPTION_KEY.getId(), ServerboundLoginPackets.ENCRYPTION_KEY.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE_ARRAY_PRIMITIVE); // Public key
                handler(wrapper -> {
                    final ChatSession1_19_1 chatSession = wrapper.user().get(ChatSession1_19_1.class);

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

        this.registerServerbound(ServerboundPackets1_19_3.CHAT_MESSAGE, ServerboundPackets1_19_1.CHAT_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Message
                map(Type.LONG); // Timestamp
                map(Type.LONG); // Salt
                read(OPTIONAL_MESSAGE_SIGNATURE_BYTES_TYPE); // Signature
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
                read(Type.VAR_INT); // Offset
                read(ACKNOWLEDGED_BIT_SET_TYPE); // Acknowledged
            }
        }, true);
    }

}
