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

import io.jsonwebtoken.impl.ParameterMap;
import io.jsonwebtoken.lang.Strings;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.COverride;
import net.lenni0451.classtransform.annotations.injection.CRedirect;

@CTransformer(ParameterMap.class)
public abstract class ParameterMapTransformer {

    @CRedirect(method = {"apply", "nullSafePut"}, target = @CTarget(value = "INVOKE", target = "Lio/jsonwebtoken/lang/Objects;isEmpty(Ljava/lang/Object;)Z"))
    private boolean allowEmptyValues(final Object value) {
        return value == null;
    }

    @COverride
    private static Object clean(Object o) {
        if (o instanceof String) {
            o = Strings.trimWhitespace((String) o);
        }
        return o;
    }

}
