/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2024 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.protocoltranslator.viaproxy.loading_terrain_fix;

import com.viaversion.viaversion.api.connection.StoredObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.packet.ClientboundPackets1_18;
import com.viaversion.viaversion.protocols.v1_18to1_18_2.Protocol1_18To1_18_2;

public class SpawnPositionTracker extends StoredObject {

    private BlockPosition spawnPosition = new BlockPosition(8, 64, 8);
    private float angle = 0F;

    public SpawnPositionTracker(final UserConnection user) {
        super(user);
    }

    public void setSpawnPosition(final BlockPosition spawnPosition, final float angle) {
        this.spawnPosition = spawnPosition;
        this.angle = angle;
    }

    public void sendSpawnPosition() {
        final PacketWrapper setDefaultSpawnPosition = PacketWrapper.create(ClientboundPackets1_18.SET_DEFAULT_SPAWN_POSITION, this.user());
        setDefaultSpawnPosition.write(Types.BLOCK_POSITION1_14, this.spawnPosition); // position
        setDefaultSpawnPosition.write(Types.FLOAT, this.angle); // angle
        setDefaultSpawnPosition.send(Protocol1_18To1_18_2.class);
    }

}
