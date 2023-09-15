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
package net.raphimc.viaproxy.util;

import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class URLClassProvider implements IClassProvider {

    private final IClassProvider parent;
    private final List<URL> urls;

    public URLClassProvider(final IClassProvider parent, final URL... urls) {
        this.parent = parent;
        this.urls = Arrays.asList(urls);
    }

    @Override
    public byte[] getClass(String name) throws ClassNotFoundException {
        for (URL url : this.urls) {
            try (InputStream is = new URL("jar:" + url + "!/" + ASMUtils.slash(name) + ".class").openStream()) {
                return IOUtils.toByteArray(is);
            } catch (Throwable ignored) {
            }
        }
        return this.parent.getClass(name);
    }

    @Override
    public Map<String, Supplier<byte[]>> getAllClasses() {
        return this.parent.getAllClasses();
    }

}
