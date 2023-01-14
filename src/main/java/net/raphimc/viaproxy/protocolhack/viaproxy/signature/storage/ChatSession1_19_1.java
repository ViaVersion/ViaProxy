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

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.PlayerMessageSignature;
import com.viaversion.viaversion.api.minecraft.ProfileKey;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.DecoratableMessage;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.MessageMetadata;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.chain.v1_19_1.MessageBody;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.model.chain.v1_19_1.MessageHeader;

import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.UUID;

public class ChatSession1_19_1 extends ChatSession {

    private byte[] precedingSignature;

    public ChatSession1_19_1(UserConnection user, UUID uuid, PrivateKey privateKey, ProfileKey profileKey) {
        super(user, uuid, privateKey, profileKey);
    }

    public byte[] signChatMessage(final MessageMetadata metadata, final DecoratableMessage content, final PlayerMessageSignature[] lastSeenMessages) throws SignatureException {
        final byte[] signature = this.sign(signer -> {
            final MessageHeader messageHeader = new MessageHeader(this.precedingSignature, metadata.getSender());
            final MessageBody messageBody = new MessageBody(content, metadata.getTimestamp(), metadata.getSalt(), lastSeenMessages);
            messageHeader.update(signer);
            messageBody.update(signer);
        });
        this.precedingSignature = signature;
        return signature;
    }

}
