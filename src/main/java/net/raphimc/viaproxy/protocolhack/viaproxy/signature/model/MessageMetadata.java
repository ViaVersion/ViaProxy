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
package net.raphimc.viaproxy.protocolhack.viaproxy.signature.model;

import java.time.Instant;
import java.util.UUID;

public class MessageMetadata {

    private final UUID sender;
    private final Instant timestamp;
    private final long salt;

    public MessageMetadata(final UUID sender, final Instant timestamp, final long salt) {
        this.sender = sender;
        this.timestamp = timestamp;
        this.salt = salt;
    }

    public MessageMetadata(final UUID sender, final long timestamp, final long salt) {
        this(sender, Instant.ofEpochMilli(timestamp), salt);
    }

    public UUID getSender() {
        return this.sender;
    }

    public Instant getTimestamp() {
        return this.timestamp;
    }

    public long getSalt() {
        return this.salt;
    }

}
