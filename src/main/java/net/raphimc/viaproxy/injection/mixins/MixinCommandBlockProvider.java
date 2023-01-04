package net.raphimc.viaproxy.injection.mixins;

import com.viaversion.viaversion.protocols.protocol1_9to1_8.providers.CommandBlockProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = CommandBlockProvider.class, remap = false)
public abstract class MixinCommandBlockProvider {

    @ModifyConstant(method = "sendPermission", constant = @Constant(intValue = 26))
    private int modifyOpLevel() {
        return 28; // Op Level 4
    }

}
