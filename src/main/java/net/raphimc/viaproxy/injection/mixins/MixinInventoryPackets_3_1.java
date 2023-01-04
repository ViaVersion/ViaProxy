package net.raphimc.viaproxy.injection.mixins;

import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.viaversion.viaversion.protocols.protocol1_14to1_13_2.packets.InventoryPackets$3$1", remap = false)
public abstract class MixinInventoryPackets_3_1 {

    @SuppressWarnings({"UnresolvedMixinReference", "MixinAnnotationTarget"})
    @Inject(method = "handle", at = @At(value = "FIELD", target = "Lcom/viaversion/viaversion/api/type/Type;BOOLEAN:Lcom/viaversion/viaversion/api/type/types/BooleanType;", ordinal = 2, shift = At.Shift.BEFORE))
    private void removeExtraData(PacketWrapper wrapper, CallbackInfo ci) {
        wrapper.clearInputBuffer();
    }

}
