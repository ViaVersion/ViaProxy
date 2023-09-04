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
package net.raphimc.viaproxy.injection;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.transformer.IBytecodeTransformer;

/**
 * This class should not be used anymore. ViaProxy does now automatically downgrade all plugins to the current java version.
 */
@Deprecated
public class Java17ToJava8 implements IBytecodeTransformer {

    @Deprecated
    public Java17ToJava8(final TransformerManager transformerManager) {
    }

    @Deprecated
    @Override
    public byte[] transform(String className, byte[] bytecode, boolean calculateStackMapFrames) {
        return null;
    }

}
