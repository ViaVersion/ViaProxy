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
package net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.chain.v1_19_1;

import com.viaversion.viaversion.api.minecraft.PlayerMessageSignature;
import net.lenni0451.mcstructs.text.serializer.TextComponentSerializer;
import net.lenni0451.mcstructs.text.utils.JsonUtils;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.DecoratableMessage;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.util.DataConsumer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

public class MessageBody {

    private final DecoratableMessage content;
    private final Instant timestamp;
    private final long salt;
    private final PlayerMessageSignature[] lastSeenMessages;

    public MessageBody(final DecoratableMessage content, final Instant timestamp, final long salt, final PlayerMessageSignature[] lastSeenMessages) {
        this.content = content;
        this.timestamp = timestamp;
        this.salt = salt;
        this.lastSeenMessages = lastSeenMessages;
    }

    public void update(final DataConsumer dataConsumer) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            dataOutputStream.writeLong(this.salt);
            dataOutputStream.writeLong(this.timestamp.getEpochSecond());

            dataOutputStream.write(this.content.getPlain().getBytes(StandardCharsets.UTF_8));
            dataOutputStream.write(70);
            if (this.content.isDecorated()) {
                dataOutputStream.write(JsonUtils.toSortedString(TextComponentSerializer.V1_18.serializeJson(this.content.getDecorated()), null).getBytes(StandardCharsets.UTF_8));
            }

            for (PlayerMessageSignature lastSeenMessage : this.lastSeenMessages) {
                dataOutputStream.writeByte(70);
                dataOutputStream.writeLong(lastSeenMessage.uuid().getMostSignificantBits());
                dataOutputStream.writeLong(lastSeenMessage.uuid().getLeastSignificantBits());
                dataOutputStream.write(lastSeenMessage.signatureBytes());
            }

            digest.update(outputStream.toByteArray());
            dataConsumer.accept(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
