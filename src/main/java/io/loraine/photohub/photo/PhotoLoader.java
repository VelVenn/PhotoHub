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

import javafx.scene.image.Image;

import java.nio.file.*;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.util.List;

import java.io.IOException;

public class PhotoLoader {
    private final Cache<Photo, Image> cache;

    private final ExecutorService executor;

    private CompletableFuture<Void> dirTask = null;
    private Map<Photo, CompletableFuture<Image>> photoTasks = new ConcurrentHashMap<>();

    private volatile Path dirPath;
    private volatile List<Photo> photoPaths;
    private Map<Photo, Integer> photoIndex = new ConcurrentHashMap<>();

    private volatile boolean isScanDone = false;
    private final Object scanLock = new Object();

    /**
     * Default constructor, setting the image cache to max 150MiB
     * and use the cached thread pool
     */
    public PhotoLoader() {
        this(157_286_400, true); // 150MiB
    }

    public PhotoLoader(int cacheSize) {
        cache = Caffeine.newBuilder()
                .initialCapacity(10)
                .maximumSize(cacheSize)
                .expireAfterAccess(60, TimeUnit.SECONDS)
                .build();
        executor = Executors.newCachedThreadPool();
    }

    public PhotoLoader(int cacheSize, int executorSize) {
        cache = Caffeine.newBuilder()
                .initialCapacity(10)
                .maximumSize(cacheSize)
                .expireAfterAccess(60, TimeUnit.SECONDS)
                .build();
        executor = Executors.newFixedThreadPool(executorSize);
    }

    // Any boolean value will do.
    public PhotoLoader(int cacheWeight, boolean isWeight) {
        cache = Caffeine.newBuilder()
                .initialCapacity(10)
                .maximumWeight(cacheWeight)
                .expireAfterAccess(60, TimeUnit.SECONDS)
                .weigher((Photo p, Image i) -> (int) (i.getHeight() * i.getWidth() * 4))
                .build();
        executor = Executors.newCachedThreadPool();
    }

    public PhotoLoader(int cacheWeight, int executorSize, boolean isWeight) {
        cache = Caffeine.newBuilder()
                .initialCapacity(10)
                .maximumWeight(cacheWeight)
                .expireAfterAccess(60, TimeUnit.SECONDS)
                .weigher((Photo p, Image i) -> (int) (i.getHeight() * i.getWidth() * 4))
                .build();
        executor = Executors.newFixedThreadPool(executorSize);
    }

    public void scanPath(Path path) throws IOException {
        validateDirectory(path);

        if (isScanDone) {
            return;
        }

        if (dirTask != null) {
            dirTask.join(); 
            return;
        }

        synchronized (scanLock) {
            if (isScanDone) {
                return;
            } 

            if (dirTask != null) {
                dirTask.join();
                return;
            }

            try (Stream<Path> pathStream = Files.list(path)) {
                List<Photo> tmpPhotoPaths = pathStream
                        .filter(Photos::isValidPhoto)
                        .map(p -> new Photo(p, true))
                        .toList();
                int[] idx = {0};
                Map<Photo, Integer> tmpPhotoIndex = tmpPhotoPaths.stream()
                        .collect(Collectors.toMap(p -> p, p -> idx[0]++));
                photoPaths = tmpPhotoPaths;
                photoIndex = tmpPhotoIndex;
                isScanDone = true;
            }

            dirTask = CompletableFuture.completedFuture(null);
        }
    }

    public CompletableFuture<Void> scanPathAsync(Path path) throws IOException {
        validateDirectory(path);

        if (isScanDone) {
            return CompletableFuture.completedFuture(null);
        }

        if (dirTask != null) {
            return dirTask;
        }

        synchronized (scanLock) {
            if (isScanDone) {
                return CompletableFuture.completedFuture(null);
            }

            if (dirTask != null) {
                return dirTask;
            }

            dirTask = CompletableFuture.runAsync(() -> {
                try (Stream<Path> pathStream = Files.list(path)) {
                    photoPaths = pathStream
                            .filter(Photos::isValidPhoto)
                            .map(p -> new Photo(p, true))
                            .toList();

                    int[] idx = {0};
                    photoIndex = photoPaths.stream().collect(Collectors.toMap(p -> p, p -> idx[0]++));

                    isScanDone = true;
                } catch (IOException e) {
                    throw new RuntimeException("Error scanning path: " + path, e);
                }
            }, executor).exceptionally(ex -> {
                isScanDone = false;
                System.err.println(ex.getMessage());
                return null;
            });
        }

        return dirTask;
    }

    public CompletableFuture<Photo> loadPhotoMetadataAsync(Photo photo) {
        if (photo == null) {
            throw new NullPointerException("Photo cannot be null.");
        }


        if (photo.isAttributesLoaded() && photo.isDimensionsLoaded()) {
            return CompletableFuture.completedFuture(photo);
        }

        return CompletableFuture.supplyAsync(() -> {
            synchronized (photo.getLock()) {
                try {
                    if (!photo.isAttributesLoaded()) {
                        photo.loadImageAttributes();
                        photo.setAttributesLoaded(true);
                    }
                    if (!photo.isDimensionsLoaded()) {
                        photo.loadImageDimensions();
                        photo.setDimensionsLoaded(true);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error loading photo metadata: " + photo, e);
                }
                return photo;
            }
        }, executor).exceptionally(ex -> {
            photo.setAttributesLoaded(false);
            photo.setDimensionsLoaded(false);
            System.err.println(ex.getMessage());
            return null;
        });
    }

    public void cancelTask() {
        if (dirTask != null && !dirTask.isDone()) {
            dirTask.cancel(true);
        }

        for (Map.Entry<Photo, CompletableFuture<Image>> entry : photoTasks.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isDone()) {
                entry.getValue().cancel(true);
            }
        }

        photoTasks.clear();
        executor.shutdownNow();
    }

    public int getPhotoCount() {
        if (!isScanDone) {
            return -1;
        }

        return photoPaths == null ? 0 : photoPaths.size();
    }

    public List<Photo> getPhotoPaths() {
        if (!isScanDone) {
            return null;
        }

        return List.copyOf(photoPaths);
    }

    public Path getDirPath() {
        return dirPath;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    private void validateDirectory(Path path) throws NoSuchFileException, AccessDeniedException {
        if (path == null) {
            throw new NullPointerException("Path cannot be null.");
        }
        if (!Files.exists(path)) {
            throw new NoSuchFileException("Directory does not exist: " + path);
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path is not a directory: " + path);
        }
        if (!Files.isReadable(path)) {
            throw new AccessDeniedException("Path is not readable: " + path);
        }

        dirPath = path;
    }

    public static void main(String[] args) {
        LoaderTester.metadataLaterTest();
    }
}

class LoaderTester {
    static void scanDirTest() {
        try {
            PhotoLoader loader = new PhotoLoader();

            Path path = Paths.get(
                    "D:\\KUN\\desktop\\Tools\\hitomi_downloader_GUI\\hitomi_ExHentai\\[POISON MOTION] 悲鳴 -地下室に囚われた１１人-");

            var future = loader.scanPathAsync(path);
            if (future != null) {
                future.thenRun(() -> {
                    System.out.println("Scan done.");
                    System.out.println("Photo count: " + loader.getPhotoCount());
                    System.out.println("Photo paths: " + loader.getDirPath());
                }).join();
            } else {
                System.out.println("Scan have already been done.");
            }

            loader.scanPath(path);
            System.out.println("Scan done.");

            loader.cancelTask();
        } catch (IOException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    static void metadataLaterTest() {
        try {
            PhotoLoader loader = new PhotoLoader();

            var path = Paths.get("D:\\KUN\\picture\\wallpaper\\escalator.jpg");

            Photo photo = new Photo(path, true);

            var future = loader.loadPhotoMetadataAsync(photo).thenAccept(p -> {
                System.out.println("Size: " + p.getStorageSizeLiteral());
                System.out.println("Last modified: " + p.getLastModifiedTimeLiteral());
                System.out.println("Dimensions: " + p.getDimensionsLiteral());
            });

            System.out.println("Photo: " + photo.getName());
            System.out.println("Path: " + photo.getPath());
            System.out.println("Type: " + photo.getType());

            future.join();

            loader.cancelTask();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
