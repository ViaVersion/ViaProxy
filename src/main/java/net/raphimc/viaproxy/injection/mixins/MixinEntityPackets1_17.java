package net.raphimc.viaproxy.injection.mixins;

import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.Protocol1_17To1_16_4;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.packets.EntityPackets;
import net.raphimc.viaproxy.injection.ClassicWorldHeightInjection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityPackets.class, remap = false)
public abstract class MixinEntityPackets1_17 {

    @Redirect(method = "registerPackets", at = @At(value = "INVOKE", target = "Lcom/viaversion/viaversion/protocols/protocol1_17to1_16_4/Protocol1_17To1_16_4;registerClientbound(Lcom/viaversion/viaversion/api/protocol/packet/ClientboundPacketType;Lcom/viaversion/viaversion/api/protocol/remapper/PacketRemapper;)V"))
    private void handleClassicWorldHeight(Protocol1_17To1_16_4 instance, ClientboundPacketType packetType, PacketRemapper packetRemapper) {
        if (packetType == ClientboundPackets1_16_2.JOIN_GAME) packetRemapper = ClassicWorldHeightInjection.handleJoinGame(instance, packetRemapper);
        if (packetType == ClientboundPackets1_16_2.RESPAWN) packetRemapper = ClassicWorldHeightInjection.handleRespawn(instance, packetRemapper);

        ((Protocol) instance).registerClientbound(packetType, packetRemapper);
    }

}
