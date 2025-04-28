/**
 * Photohub ---- To View Some S3xy Photos
 * Copyright (C) 2025 Loraine, Yui
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

// import javax.imageio.ImageIO;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.Iterator;
import java.util.Objects;

import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class Photo {
    private static final Set<String> SUPPORTED_TYPES;
    private static final DateTimeFormatter BASIC_TIME_FORMAT;

    private String name;
    private String type;

    private long storageSize;
    private long width = 0;
    private long height = 0;

    private Path photoPath;
    private Path parent;

    private LocalDateTime lastModifiedTime;

    static {
        String[] suffixes = ImageIO.getReaderFileSuffixes();
        SUPPORTED_TYPES = Arrays
                .stream(suffixes).map(String::toLowerCase).collect(Collectors.toCollection(HashSet::new));

        System.out.println("Supported types " + SUPPORTED_TYPES);
        BASIC_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss, ccc");
    }

    public Photo(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + path);
        }

        if (Files.isDirectory(path)) {
            throw new IOException("Path is a directory: " + path);
        }

        if (!Files.isReadable(path)) {
            throw new IOException("File is not readable: " + path);
        }

        name = path.getFileName().toString();
        int dotIndex = name.lastIndexOf('.');

        type = (dotIndex == -1 || dotIndex == name.length() - 1)
                ? "NO EXTENSION" : name.substring(dotIndex + 1).toLowerCase();

        if (type.equals("NO EXTENSION")) {
            throw new IOException("File has no extension: " + path);
        }

        if (!SUPPORTED_TYPES.contains(type)) {
            throw new IOException("Unsupported file type: " + type);
        }

        photoPath = path;
        parent = path.getParent();

        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        storageSize = attributes.size();
        lastModifiedTime = LocalDateTime.ofInstant(
                attributes.lastModifiedTime().toInstant(), ZoneId.systemDefault());

        loadImageDimensions();
    }

    public Photo(String pathLiteral) throws IOException {
        this(Paths.get(pathLiteral));
    }

    // Only read the metadata instead of decoding the image
    private void loadImageDimensions() throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(Files.newInputStream(photoPath))) {
            if (in == null) {
                throw new IOException("Failed to read the bytes in: " + photoPath);
            }

            // ImageInputStream 用于顺序地（或随机地）存取图像文件中的原始字节数据，
            // 让后续的ImageReader可以正确地定位、解析图像文件头等相关元数据信息。

            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IOException("Unable to decode: " + photoPath);
            }

            ImageReader reader = readers.next();
            try {
                // 将 ImageInputStream 传给 ImageReader，
                // 让它从该数据流中读取图片的必要元数据。
                reader.setInput(in);

                // 只读取元数据，不解码像素
                this.width = reader.getWidth(0); // 第0帧 / 图层
                this.height = reader.getHeight(0);
            } finally {
                reader.dispose(); // 释放资源
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public long getStorageSize() {
        return storageSize;
    }

    public double getStorageSizeKiB() {
        return storageSize / 1024.0;
    }

    public double getStorageSizeMiB() {
        return storageSize / 1024.0 / 1024.0;
    }

    public String getStorageSizeLiteral() {
        if (storageSize < 1024) {
            return storageSize + " B";
        } else if (storageSize < 1024 * 1024) {
            return String.format("%.2f KiB", getStorageSizeKiB());
        } else {
            return String.format("%.2f MiB", getStorageSizeMiB());
        }
    }

    public long getWidth() {
        return width;
    }

    public long getHeight() {
        return height;
    }

    public String getDimensionsLiteral() {
        return width + " × " + height;
    }

    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public String getLastModifiedTimeLiteral() {
        return lastModifiedTime.format(BASIC_TIME_FORMAT);
    }

    public Path getPath() {
        return photoPath;
    }

    public Path getParent() {
        return parent;
    }

    public static void main(String[] args) {
        try {
            var path = Objects.requireNonNull(
                    Photo.class.getResource("/io/loraine/photohub/Default Resources/Escalator.jpg")).toURI();

            Photo photoInfo = new Photo(Paths.get(path));

            System.out.println(photoInfo.getType());
            System.out.println(photoInfo.getName());
            System.out.println(photoInfo.getPath());
            System.out.println(photoInfo.getParent());
            System.out.println(photoInfo.getStorageSizeLiteral());
            System.out.println(photoInfo.getLastModifiedTimeLiteral());
            System.out.println(photoInfo.getDimensionsLiteral());
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + " -> " + e.getMessage());
        }
    }
}
