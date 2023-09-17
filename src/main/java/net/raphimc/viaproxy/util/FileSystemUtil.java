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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileSystemUtil {

    public static Map<Path, byte[]> getFilesInDirectory(final String assetPath) throws IOException, URISyntaxException {
        final URI uri = FileSystemUtil.class.getClassLoader().getResource(assetPath).toURI();
        if (uri.getScheme().equals("file")) {
            return getFilesInPath(Paths.get(uri));
        } else if (uri.getScheme().equals("jar")) {
            try (FileSystem fileSystem = getOrCreateFileSystem(uri)) {
                return getFilesInPath(fileSystem.getPath(assetPath));
            }
        } else {
            throw new IllegalArgumentException("Unsupported URI scheme: " + uri.getScheme());
        }
    }

    private static Map<Path, byte[]> getFilesInPath(final Path path) throws IOException {
        try (Stream<Path> stream = Files.walk(path)) {
            return stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .collect(Collectors.toMap(f -> f, f -> {
                        try {
                            return Files.readAllBytes(f);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }, (u, v) -> {
                        throw new IllegalStateException("Duplicate key");
                    }, LinkedHashMap::new));
        }
    }

    private static FileSystem getOrCreateFileSystem(final URI uri) throws IOException {
        FileSystem fileSystem;
        try {
            fileSystem = FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException e) {
            fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
        }
        return fileSystem;
    }

}
