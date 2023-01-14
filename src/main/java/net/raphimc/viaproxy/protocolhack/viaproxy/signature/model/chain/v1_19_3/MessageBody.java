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
package net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.chain.v1_19_3;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.viaversion.viaversion.api.minecraft.PlayerMessageSignature;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.util.DataConsumer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class MessageBody {

    private final String content;
    private final Instant timestamp;
    private final long salt;
    private final PlayerMessageSignature[] lastSeenMessages;

    public MessageBody(final String content, final Instant timestamp, final long salt, final PlayerMessageSignature[] lastSeenMessages) {
        this.content = content;
        this.timestamp = timestamp;
        this.salt = salt;
        this.lastSeenMessages = lastSeenMessages;
    }

    public void update(final DataConsumer dataConsumer) {
        dataConsumer.accept(Longs.toByteArray(this.salt));
        dataConsumer.accept(Longs.toByteArray(this.timestamp.getEpochSecond()));
        final byte[] contentData = this.content.getBytes(StandardCharsets.UTF_8);
        dataConsumer.accept(Ints.toByteArray(contentData.length));
        dataConsumer.accept(contentData);

        dataConsumer.accept(Ints.toByteArray(this.lastSeenMessages.length));
        for (PlayerMessageSignature messageSignatureData : this.lastSeenMessages) {
            dataConsumer.accept(messageSignatureData.signatureBytes());
        }
    }

}
