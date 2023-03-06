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

import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.blockconnections.ConnectionData;
import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;

@CTransformer(ConnectionData.NeighbourUpdater.class)
public class ConnectionData_NeighbourUpdaterTransformer {

    @CInject(method = "updateBlock", target = @CTarget(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", shift = CTarget.Shift.BEFORE), cancellable = true)
    public void preventBlockChangeSpam(InjectionCallback ic, @CLocalVariable(name = "blockState") int blockState, @CLocalVariable(name = "newBlockState") int newBlockState) {
        if (blockState == newBlockState) ic.setCancelled(true);
    }

}
