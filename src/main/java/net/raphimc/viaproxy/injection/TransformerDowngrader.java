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
package net.raphimc.viaproxy.injection;

import net.lenni0451.classtransform.transformer.IAnnotationHandlerPreprocessor;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Sneaky;
import org.objectweb.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.runtime.ClassDowngradingAgent;

import java.lang.instrument.IllegalClassFormatException;

public class TransformerDowngrader implements IAnnotationHandlerPreprocessor {

    private final ClassLoader classLoader;
    private final ClassDowngradingAgent downgrader = new ClassDowngradingAgent();

    public TransformerDowngrader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void process(final ClassNode node) {
    }

    @Override
    public ClassNode replace(final ClassNode node) {
        final byte[] bytes = ASMUtils.toStacklessBytes(node);
        try {
            final byte[] transformed = this.downgrader.transform(this.classLoader, node.name, null, null, bytes);
            if (transformed != null) {
                return ASMUtils.fromBytes(transformed);
            } else {
                return node;
            }
        } catch (IllegalClassFormatException e) {
            Sneaky.sneakyThrow(e);
            return null;
        }
    }

}
