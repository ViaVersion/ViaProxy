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
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.tree.ClassTree;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import net.raphimc.javadowngrader.JavaDowngrader;
import org.objectweb.asm.tree.ClassNode;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Java17ToJava8 implements IBytecodeTransformer {

    private final ClassTree classTree;
    private final IClassProvider classProvider;
    private final int nativeClassVersion;
    private final List<String> whitelistedPackages = new ArrayList<>();

    public Java17ToJava8(final ClassTree classTree, final IClassProvider classProvider) {
        this.classTree = classTree;
        this.classProvider = classProvider;

        final String classVersion = System.getProperty("java.class.version");
        final String[] versions = classVersion.split("\\.");
        final int majorVersion = Integer.parseInt(versions[0]);
        final int minorVersion = Integer.parseInt(versions[1]);
        this.nativeClassVersion = minorVersion << 16 | majorVersion;
    }

    public Java17ToJava8(final TransformerManager transformerManager) {
        this(transformerManager.getClassTree(), transformerManager.getClassProvider());
    }

    public Java17ToJava8 addWhitelistedPackage(final String packageName) {
        this.whitelistedPackages.add(packageName);
        return this;
    }

    @Override
    public byte[] transform(final String className, final byte[] bytecode, final boolean calculateStackMapFrames) {
        if (ByteBuffer.wrap(bytecode, 4, 4).getInt() <= this.nativeClassVersion) {
            return null;
        }

        if (!this.whitelistedPackages.isEmpty()) {
            int dotIndex = className.lastIndexOf('.');
            if (dotIndex == -1 && !this.whitelistedPackages.contains("")) return null;
            String pkg = className.substring(0, dotIndex);
            while (!this.whitelistedPackages.contains(pkg)) {
                dotIndex = pkg.lastIndexOf('.');
                if (dotIndex == -1) return null;
                pkg = pkg.substring(0, dotIndex);
            }
        }

        final ClassNode classNode = ASMUtils.fromBytes(bytecode);
        JavaDowngrader.downgrade(classNode, this.nativeClassVersion);

        if (calculateStackMapFrames) {
            return ASMUtils.toBytes(classNode, this.classTree, this.classProvider);
        } else {
            return ASMUtils.toStacklessBytes(classNode);
        }
    }

}
