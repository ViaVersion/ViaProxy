package net.raphimc.viaproxy.injection.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.packets.WorldPackets$2", remap = false)
public abstract class MixinWorldPackets_2 {

    @ModifyConstant(method = "lambda$registerMap$0", constant = @Constant(intValue = 16))
    private static int modifySectionCountToSupportClassicWorldHeight() {
        return 64;
    }

}
