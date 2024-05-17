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
package net.raphimc.viaproxy.injection.mixins;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.packet.ClientboundPackets1_18;
import com.viaversion.viaversion.protocols.v1_18to1_18_2.Protocol1_18To1_18_2;
import net.raphimc.viaproxy.protocoltranslator.viaproxy.loading_terrain_fix.SpawnPositionTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(Protocol1_18To1_18_2.class)
public abstract class MixinProtocol1_18To1_18_2 extends AbstractProtocol<ClientboundPackets1_18, ClientboundPackets1_18, ServerboundPackets1_17, ServerboundPackets1_17> {

    @Inject(method = "registerPackets", at = @At("RETURN"))
    private void fixDownloadingTerrainScreenNotClosing() {
        this.registerClientbound(ClientboundPackets1_18.PLAYER_POSITION, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> wrapper.user().get(SpawnPositionTracker.class).sendSpawnPosition());
            }
        });
        this.registerClientbound(ClientboundPackets1_18.SET_DEFAULT_SPAWN_POSITION, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_14); // position
                map(Types.FLOAT); // angle
                handler(wrapper -> wrapper.user().get(SpawnPositionTracker.class).setSpawnPosition(wrapper.get(Types.BLOCK_POSITION1_14, 0), wrapper.get(Types.FLOAT, 0)));
            }
        });
    }

    @Override
    public void init(UserConnection user) {
        user.put(new SpawnPositionTracker(user));
    }

}
