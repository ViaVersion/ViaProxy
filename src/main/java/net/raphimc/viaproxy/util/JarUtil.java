/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2025 RK_01/RaphiMC and contributors
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

import net.raphimc.viaproxy.ViaProxy;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class JarUtil {

    public static Optional<File> getJarFile() {
        try {
            return Optional.of(new File(ViaProxy.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    public static void launch(final File jarFile) throws IOException {
        new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-jar", jarFile.getAbsolutePath()).directory(ViaProxy.getCwd()).start();
    }

}
