/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2025 RK_01/RaphiMC and contributors
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

import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.Protocol1_16_4To1_17;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.rewriter.EntityPacketRewriter1_17;
import net.raphimc.viaproxy.injection.ClassicWorldHeightInjection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityPacketRewriter1_17.class, remap = false)
public abstract class MixinEntityPacketRewriter1_17 {

    @Redirect(method = "registerPackets", at = @At(value = "INVOKE", target = "Lcom/viaversion/viaversion/protocols/v1_16_4to1_17/Protocol1_16_4To1_17;registerClientbound(Lcom/viaversion/viaversion/api/protocol/packet/ClientboundPacketType;Lcom/viaversion/viaversion/api/protocol/remapper/PacketHandler;)V"))
    private void handleClassicWorldHeight(Protocol1_16_4To1_17 instance, ClientboundPacketType packetType, PacketHandler packetHandler) {
        if (packetType == ClientboundPackets1_16_2.LOGIN) packetHandler = ClassicWorldHeightInjection.handleJoinGame(packetHandler);
        if (packetType == ClientboundPackets1_16_2.RESPAWN) packetHandler = ClassicWorldHeightInjection.handleRespawn(packetHandler);

        ((Protocol) instance).registerClientbound(packetType, packetHandler);
    }

}
