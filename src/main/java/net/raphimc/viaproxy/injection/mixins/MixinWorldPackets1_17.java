package net.raphimc.viaproxy.injection.mixins;

import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.Protocol1_17To1_16_4;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.packets.WorldPackets;
import net.raphimc.viaproxy.injection.ClassicWorldHeightInjection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WorldPackets.class, remap = false)
public abstract class MixinWorldPackets1_17 {

    @Redirect(method = "register", at = @At(value = "INVOKE", target = "Lcom/viaversion/viaversion/protocols/protocol1_17to1_16_4/Protocol1_17To1_16_4;registerClientbound(Lcom/viaversion/viaversion/api/protocol/packet/ClientboundPacketType;Lcom/viaversion/viaversion/api/protocol/remapper/PacketRemapper;)V"))
    private static void handleClassicWorldHeight(Protocol1_17To1_16_4 instance, ClientboundPacketType packetType, PacketRemapper packetRemapper) {
        if (packetType == ClientboundPackets1_16_2.CHUNK_DATA) packetRemapper = ClassicWorldHeightInjection.handleChunkData(instance, packetRemapper);
        if (packetType == ClientboundPackets1_16_2.UPDATE_LIGHT) packetRemapper = ClassicWorldHeightInjection.handleUpdateLight(instance, packetRemapper);

        ((Protocol) instance).registerClientbound(packetType, packetRemapper);
    }

}
