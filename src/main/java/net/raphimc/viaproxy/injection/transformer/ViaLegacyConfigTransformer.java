/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.injection.transformer;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.raphimc.vialegacy.ViaLegacyConfig;
import net.raphimc.viaproxy.cli.options.Options;

@CTransformer(ViaLegacyConfig.class)
public abstract class ViaLegacyConfigTransformer {

    @CInject(method = "isLegacySkinLoading", target = @CTarget("HEAD"), cancellable = true)
    private void makeGUIConfigurable(final InjectionCallback ic) {
        if (Options.LEGACY_SKIN_LOADING != null) {
            ic.setReturnValue(Options.LEGACY_SKIN_LOADING);
        }
    }

}
