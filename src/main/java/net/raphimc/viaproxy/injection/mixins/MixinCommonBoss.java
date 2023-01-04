package net.raphimc.viaproxy.injection.mixins;

import com.viaversion.viaversion.legacy.bossbar.CommonBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CommonBoss.class, remap = false)
public abstract class MixinCommonBoss {

    @Redirect(method = {"<init>", "setHealth"}, at = @At(value = "INVOKE", target = "Lcom/google/common/base/Preconditions;checkArgument(ZLjava/lang/Object;)V"))
    private void removeBoundChecks(boolean expression, Object errorMessage) {
    }

}
