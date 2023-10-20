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

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.minecraft.ProfileKey;
import net.raphimc.viaproxy.protocolhack.viaproxy.signature.util.DataConsumer;

import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatSession implements StorableObject {

    private final UUID uuid;
    private final PrivateKey privateKey;
    private final ProfileKey profileKey;
    private final Signature signer;

    public ChatSession(final UUID uuid, final PrivateKey privateKey, final ProfileKey profileKey) {
        this.uuid = uuid;
        this.privateKey = privateKey;
        this.profileKey = profileKey;

        try {
            this.signer = Signature.getInstance("SHA256withRSA");
            this.signer.initSign(this.privateKey);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize signature", e);
        }
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public ProfileKey getProfileKey() {
        return this.profileKey;
    }

    public byte[] sign(final Consumer<DataConsumer> dataConsumer) throws SignatureException {
        dataConsumer.accept(bytes -> {
            try {
                this.signer.update(bytes);
            } catch (SignatureException e) {
                throw new RuntimeException(e);
            }
        });
        return this.signer.sign();
    }

}
