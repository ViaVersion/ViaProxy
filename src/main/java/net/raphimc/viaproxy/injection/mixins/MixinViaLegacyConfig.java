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

import net.raphimc.vialegacy.ViaLegacyConfig;
import net.raphimc.viaproxy.ViaProxy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ViaLegacyConfig.class)
public abstract class MixinViaLegacyConfig {

    @Inject(method = "isLegacySkinLoading", at = @At("HEAD"), cancellable = true)
    private void makeGUIConfigurable1(final CallbackInfoReturnable<Boolean> cir) {
        if (ViaProxy.getViaProxyWindow() != null) {
            cir.setReturnValue(ViaProxy.getViaProxyWindow().advancedTab.legacySkinLoading.isSelected());
        }
    }

    @Inject(method = "isLegacySkullLoading", at = @At("HEAD"), cancellable = true)
    private void makeGUIConfigurable2(final CallbackInfoReturnable<Boolean> cir) {
        if (ViaProxy.getViaProxyWindow() != null) {
            cir.setReturnValue(ViaProxy.getViaProxyWindow().advancedTab.legacySkinLoading.isSelected());
        }
    }

}
