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
import com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3.Protocol1_19_1To1_19_3;
import com.viaversion.viaversion.api.minecraft.PlayerMessageSignature;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.BitSetType;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ClientboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ServerboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.ServerboundPackets1_19_3;
import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.MessageMetadata;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.storage.ChatSession1_19_3;

import java.util.BitSet;

@CTransformer(Protocol1_19_1To1_19_3.class)
public abstract class Protocol1_19_1To1_19_3Transformer extends BackwardsProtocol<ClientboundPackets1_19_3, ClientboundPackets1_19_1, ServerboundPackets1_19_3, ServerboundPackets1_19_1> {

    @CInject(method = "registerPackets", target = @CTarget("RETURN"))
    private void allowSignatures(InjectionCallback ic) {
        this.registerClientbound(State.LOGIN, ClientboundLoginPackets.GAME_PROFILE.getId(), ClientboundLoginPackets.GAME_PROFILE.getId(), new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    final ChatSession1_19_3 chatSession = wrapper.user().get(ChatSession1_19_3.class);

                    if (chatSession != null) {
                        final PacketWrapper chatSessionUpdate = wrapper.create(ServerboundPackets1_19_3.CHAT_SESSION_UPDATE);
                        chatSessionUpdate.write(Type.UUID, chatSession.getSessionId());
                        chatSessionUpdate.write(Type.PROFILE_KEY, chatSession.getProfileKey());
                        chatSessionUpdate.sendToServer(Protocol1_19_1To1_19_3.class);
                    }
                });
            }
        }, true);

        this.registerServerbound(ServerboundPackets1_19_1.CHAT_MESSAGE, ServerboundPackets1_19_3.CHAT_MESSAGE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Message
                map(Type.LONG); // Timestamp
                map(Type.LONG); // Salt
                read(Type.BYTE_ARRAY_PRIMITIVE); // Signature
                read(Type.BOOLEAN); // Signed preview
                read(Type.PLAYER_MESSAGE_SIGNATURE_ARRAY); // Last seen messages
                read(Type.OPTIONAL_PLAYER_MESSAGE_SIGNATURE); // Last received message
                handler(wrapper -> {
                    final ChatSession1_19_3 chatSession = wrapper.user().get(ChatSession1_19_3.class);

                    if (chatSession != null) {
                        final String message = wrapper.get(Type.STRING, 0);
                        final long timestamp = wrapper.get(Type.LONG, 0);
                        final long salt = wrapper.get(Type.LONG, 1);

                        final MessageMetadata metadata = new MessageMetadata(null, timestamp, salt);
                        wrapper.write(Protocol1_19_1To1_19_3.OPTIONAL_SIGNATURE_BYTES_TYPE, chatSession.signChatMessage(metadata, message, new PlayerMessageSignature[0])); // Signature
                    } else {
                        wrapper.write(Protocol1_19_1To1_19_3.OPTIONAL_SIGNATURE_BYTES_TYPE, null); // Signature
                    }

                    wrapper.write(Type.VAR_INT, 0); // Offset
                    wrapper.write(new BitSetType(20), new BitSet(20)); // Acknowledged
                });
            }
        }, true);
    }

}
