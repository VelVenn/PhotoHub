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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.Iterator;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import java.io.IOException;

public class Photo {
    private static final DateTimeFormatter BASIC_TIME_FORMAT;

    private final String name;
    private final String type;

    private final Path photoPath;
    private final Path parent;

    private volatile long width = -1;
    private volatile long height = -1;

    private volatile long storageSize = -1;
    private volatile LocalDateTime lastModifiedTime = null;

    private volatile boolean isAttributesLoaded = false;
    private volatile boolean isDimensionsLoaded = false;

    private final Object lock = new Object();

    static {
        BASIC_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss, ccc");
    }

    public Photo(Path path) throws IOException {
        Photos.validatePath(path);

        photoPath = path;
        parent = path.getParent();

        name = path.getFileName().toString();
        type = Photos.getFileExtension(name);

        Photos.validateFileExtension(type);

        if (!isAttributesLoaded) {
            synchronized (lock) {
                if (!isAttributesLoaded) {
                    try {
                        loadImageAttributes();
                        isAttributesLoaded = true;
                    } catch (IOException e) {
                        isAttributesLoaded = false;
                        throw e;
                    }
                }
            }
        }

        if (!isDimensionsLoaded) {
            synchronized (lock) {
                if (!isDimensionsLoaded) {
                    try {
                        loadImageDimensions();
                        isDimensionsLoaded = true;
                    } catch (IOException e) {
                        isDimensionsLoaded = false;
                        throw e;
                    }
                }
            }
        }
    }

    public Photo(String pathLiteral) throws IOException {
        Photos.validatePath(pathLiteral);

        photoPath = Paths.get(pathLiteral);
        parent = photoPath.getParent();

        name = photoPath.getFileName().toString();
        type = Photos.getFileExtension(name);

        Photos.validateFileExtension(type);
        
        if (!isAttributesLoaded) {
            synchronized (lock) {
                if (!isAttributesLoaded) {
                    try {
                        loadImageAttributes();
                        isAttributesLoaded = true;
                    } catch (IOException e) {
                        isAttributesLoaded = false;
                        throw e;
                    }
                }
            }
        }

        if (!isDimensionsLoaded) {
            synchronized (lock) {
                if (!isDimensionsLoaded) {
                    try {
                        loadImageDimensions();
                        isDimensionsLoaded = true;
                    } catch (IOException e) {
                        isDimensionsLoaded = false;
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * Construct a Photo object WITHOUT any I/O pre-validation.
     * @param path The path to the photo file.
     * @param noValidation Any value will do.
     */
    public Photo(Path path, boolean noValidation) {
        photoPath = path;
        parent = path.getParent();

        name = photoPath.getFileName().toString();
        type = Photos.getFileExtension(name);
    }

    // Only read the metadata instead of decoding the image
    void loadImageDimensions() throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(photoPath.toFile())) {
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

    void loadImageAttributes() throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(photoPath, BasicFileAttributes.class);
        storageSize = attributes.size();
        lastModifiedTime = LocalDateTime.ofInstant(
                attributes.lastModifiedTime().toInstant(), ZoneId.systemDefault());
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
        if (storageSize < 0 || !isAttributesLoaded) {
            return "N/A";
        }

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
        if (width < 0 || height < 0 || !isDimensionsLoaded) {
            return "N/A";
        }

        return width + " x " + height;
    }

    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public String getLastModifiedTimeLiteral() {
        if (lastModifiedTime == null || !isAttributesLoaded) {
            return "N/A";
        }

        return lastModifiedTime.format(BASIC_TIME_FORMAT);
    }

    public Path getPath() {
        return Paths.get(photoPath.toString());
    }

    public Path getParent() {
        return Paths.get(parent.toString());
    }

    public static DateTimeFormatter getTimeFormat() {
        return BASIC_TIME_FORMAT;
    }

    /** This should only be called by the PhotoLoader class */
    void setAttributesLoaded(boolean value) {
        isAttributesLoaded = value;
    }

    /** This should only be called by the PhotoLoader class */
    void setDimensionsLoaded(boolean value) {
        isDimensionsLoaded = value;
    }

    /** This should only be called by the PhotoLoader class */
    Object getLock() {
        return lock;
    }

    public boolean isAttributesLoaded() {
        return isAttributesLoaded;
    }

    public boolean isDimensionsLoaded() {
        return isDimensionsLoaded;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }

        Photo other = (Photo) obj;

        return this.photoPath.equals(other.photoPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(photoPath, this.getClass());
    }

    public static void main(String[] args) {
        try {
            var path = Objects.requireNonNull(
                    Photo.class.getResource("/io/loraine/photohub/Default_Resources/Escalator.jpg")).toURI();

            Photo photoInfo = new Photo(Paths.get(path));

            System.out.println(Files.probeContentType(photoInfo.getPath()));

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
