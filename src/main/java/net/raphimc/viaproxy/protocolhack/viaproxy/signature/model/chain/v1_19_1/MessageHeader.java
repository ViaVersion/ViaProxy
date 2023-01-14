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

import net.raphimc.viaproxy.protocolhack.viaproxy.signature.util.DataConsumer;

import java.util.UUID;

public class MessageHeader {

    private final byte[] precedingSignature;
    private final UUID sender;

    public MessageHeader(final byte[] precedingSignature, final UUID sender) {
        this.precedingSignature = precedingSignature;
        this.sender = sender;
    }

    public void update(final DataConsumer dataConsumer) {
        if (this.precedingSignature != null) {
            dataConsumer.accept(this.precedingSignature);
        }

        dataConsumer.accept(this.sender);
    }

}
