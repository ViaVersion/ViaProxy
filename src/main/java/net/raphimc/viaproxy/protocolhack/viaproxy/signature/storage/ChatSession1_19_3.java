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

import com.google.common.primitives.Ints;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.PlayerMessageSignature;
import com.viaversion.viaversion.api.minecraft.ProfileKey;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.MessageMetadata;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.chain.v1_19_3.MessageBody;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.chain.v1_19_3.MessageLink;

import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.UUID;

public class ChatSession1_19_3 extends ChatSession {

    private final UUID sessionId = UUID.randomUUID();
    private MessageLink link;

    public ChatSession1_19_3(UserConnection user, UUID uuid, PrivateKey privateKey, ProfileKey profileKey) {
        super(user, uuid, privateKey, profileKey);

        this.link = new MessageLink(uuid, this.sessionId);
    }

    public byte[] signChatMessage(final MessageMetadata metadata, final String content, final PlayerMessageSignature[] lastSeenMessages) throws SignatureException {
        return this.sign(signer -> {
            final MessageLink messageLink = this.nextLink();
            final MessageBody messageBody = new MessageBody(content, metadata.getTimestamp(), metadata.getSalt(), lastSeenMessages);
            signer.accept(Ints.toByteArray(1));
            messageLink.update(signer);
            messageBody.update(signer);
        });
    }

    private MessageLink nextLink() {
        final MessageLink messageLink = this.link;
        if (messageLink != null) {
            this.link = messageLink.next();
        }

        return messageLink;
    }

    public UUID getSessionId() {
        return this.sessionId;
    }

}
