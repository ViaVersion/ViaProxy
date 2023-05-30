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

import net.lenni0451.reflect.Classes;
import net.lenni0451.reflect.stream.RStream;
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.File;
import java.net.URL;
import java.util.List;

public class ClassLoaderPriorityUtil {

    public static void loadOverridingJars() {
        final File jarsFolder = new File("jars");
        if (jarsFolder.isDirectory()) {
            for (File file : jarsFolder.listFiles()) {
                try {
                    if (file.getName().endsWith(".jar")) {
                        loadWithHighestPriority(file.toURI().toURL());
                        Logger.LOGGER.info("Loaded overriding jar " + file.getName());
                    }
                } catch (Throwable e) {
                    Logger.LOGGER.error("Failed to load overriding jar " + file.getName(), e);
                }
            }
        }
    }

    private static void loadWithHighestPriority(final URL url) {
        // First add the URl into the classpath
        final Object ucp = RStream.of(Thread.currentThread().getContextClassLoader()).withSuper().fields().by("ucp").get();
        RStream.of(ucp).methods().by("addURL", URL.class).invokeArgs(url);

        // Then move the URL to the front of the classpath, so it gets loaded first
        final List<Object> ucpPath = RStream.of(ucp).fields().by("path").get();
        ucpPath.add(0, ucpPath.remove(ucpPath.size() - 1));

        // Force the ClassLoader to populate the whole list of Loaders (Its lazy loaded by default)
        if (ClassLoaderPriorityUtil.class.getClassLoader().getResourceAsStream("I_HOPE_THIS_FILE_NEVER_EXISTS_" + System.nanoTime()) != null) {
            throw new IllegalStateException("The file that should never exist exists! Please report this to the ViaProxy developers!");
        }

        // Move the loader for that URL to the front of the list
        final Class<?> jarLoaderClazz = Classes.forName(ucp.getClass().getName() + "$JarLoader");
        final List<Object> loaders = RStream.of(ucp).fields().by("loaders").get();
        for (Object loader : loaders) {
            if (jarLoaderClazz.equals(loader.getClass())) {
                final URL loaderUrl = RStream.of(loader).fields().filter(URL.class).by(0).get();
                if (url.equals(loaderUrl)) {
                    loaders.add(0, loaders.remove(loaders.size() - 1));
                    break;
                }
            }
        }
    }

}
