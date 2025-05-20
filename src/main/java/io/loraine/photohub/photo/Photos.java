/**
 * Photohub ---- To View Some S3xy Photos
 * Copyright (C) 2025 Loraine K. Cheung
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.loraine.photohub.photo;

import io.loraine.photohub.util.Logger;

import javax.imageio.ImageIO;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.util.Set;
import java.util.Arrays;

import java.util.stream.Collectors;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.AccessDeniedException;
import java.nio.file.InvalidPathException;

public class Photos {
    private Photos() {
    }

    private static final Set<String> SUPPORTED_TYPES;
    private static final boolean DEBUG = false;

    static {
        String[] suffixes = ImageIO.getReaderFileSuffixes();

        SUPPORTED_TYPES = Arrays
                .stream(suffixes).map(String::toLowerCase).collect(Collectors.toUnmodifiableSet());

        if (DEBUG) Logger.log("Supported types: " + SUPPORTED_TYPES);
    }

    public static void validatePath(Path path) throws IOException {
        if (path == null) {
            throw new NullPointerException("Path cannot be null.");
        }

        path = path.normalize().toAbsolutePath();

        if (!Files.exists(path)) {
            throw new NoSuchFileException("File does not exist: " + path);
        }

        if (!Files.isReadable(path)) {
            throw new AccessDeniedException("File is not readable: " + path);
        }

        if (Files.isDirectory(path)) {
            throw new IOException("Path is a directory: " + path);
        }
    }

    public static void validatePath(String pathLiteral) throws IOException {
        try {
            Path path = Paths.get(pathLiteral);
            validatePath(path);
        } catch (InvalidPathException e) {
            throw new IOException("Invalid path: " + pathLiteral, e.getCause());
        }
    }

    public static boolean isValidPhoto(Path path) {
        if (path == null) return false;

        path = path.normalize().toAbsolutePath();

        if (!Files.exists(path)) return false;
        if (Files.isDirectory(path)) return false;
        if (!Files.isReadable(path)) return false;
        String extension = getFileExtension(path);
        return isSupportedExtension(extension);
    }

    /**
     * Get file's extension
     *
     * @param path Path of the file
     * @return extension name in lower case or null if there is no extension
     */

    public static String getFileExtension(Path path) {
        if (path == null) {
            throw new NullPointerException("Path cannot be null.");
        }

        path = path.normalize().toAbsolutePath();

        String name = path.getFileName().toString();
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex == -1 || dotIndex == name.length() - 1)
                ? null : name.substring(dotIndex + 1).toLowerCase();
    }

    static String getFileExtension(String name) {
        if (name == null) {
            throw new NullPointerException("Name cannot be null.");
        }

        name = name.trim(); // remove leading and trailing spaces

        int dotIndex = name.lastIndexOf('.');
        return (dotIndex == -1 || dotIndex == name.length() - 1)
                ? null : name.substring(dotIndex + 1).toLowerCase();
    }

    static void validateFileExtension(String extension) throws IOException {
        if (extension == null) {
            throw new IOException("No file extension found.");
        }

        extension = extension.toLowerCase().trim();

        if (!SUPPORTED_TYPES.contains(extension)) {
            throw new IOException("Unsupported file extension: " + extension);
        }
    }

    /**
     * Check if the given extension is supported
     *
     * @param extension Extension name in lower case
     */
    public static boolean isSupportedExtension(String extension) {
        if (extension == null) {
            return false;
        }

        extension = extension.toLowerCase().trim();

        return SUPPORTED_TYPES.contains(extension);
    }

    public static Set<String> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }
}
