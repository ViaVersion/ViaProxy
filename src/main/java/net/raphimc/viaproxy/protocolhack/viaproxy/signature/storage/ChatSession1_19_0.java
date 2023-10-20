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
package net.raphimc.viaproxy.protocolhack.viaproxy.signature.storage;

import com.viaversion.viaversion.api.minecraft.ProfileKey;
import net.lenni0451.mcstructs.text.serializer.TextComponentSerializer;
import net.lenni0451.mcstructs.text.utils.JsonUtils;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.DecoratableMessage;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.MessageMetadata;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.UUID;

public class ChatSession1_19_0 extends ChatSession {

    public ChatSession1_19_0(UUID uuid, PrivateKey privateKey, ProfileKey profileKey) {
        super(uuid, privateKey, profileKey);
    }

    public byte[] signChatMessage(final MessageMetadata metadata, final DecoratableMessage content) throws SignatureException {
        return this.sign(signer -> {
            final byte[] data = new byte[32];
            final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
            buffer.putLong(metadata.getSalt());
            buffer.putLong(metadata.getSender().getMostSignificantBits()).putLong(metadata.getSender().getLeastSignificantBits());
            buffer.putLong(metadata.getTimestamp().getEpochSecond());
            signer.accept(data);
            signer.accept(JsonUtils.toSortedString(TextComponentSerializer.V1_18.serializeJson(content.getDecorated()), null).getBytes(StandardCharsets.UTF_8));
        });
    }

}
