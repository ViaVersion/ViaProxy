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

import com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3.Protocol1_19_1To1_19_3;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.ServerboundPackets1_19_3;
import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.storage.ChatSession1_19_3;

@CTransformer(name = "com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3.Protocol1_19_1To1_19_3$1")
public abstract class Protocol1_19_1To1_19_3$1Transformer extends PacketHandlers {

    @CInject(method = "register", target = @CTarget("RETURN"))
    private void allowSignatures(InjectionCallback ic) {
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

}
