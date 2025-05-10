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
import javafx.embed.swing.SwingFXUtils;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

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
    private final Map<Photo, CompletableFuture<Image>> photoTasks = new ConcurrentHashMap<>();

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
                .weigher((Photo p, Image i) -> {
                    double weight = i.getHeight() * i.getWidth() * 4; // Estimate as ARGB, assume 1 byte per channel
                    if (weight < 0) return 0;
                    return weight > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) weight;
                })
                .build();
        executor = Executors.newCachedThreadPool();
    }

    public PhotoLoader(int cacheWeight, int executorSize, boolean isWeight) {
        cache = Caffeine.newBuilder()
                .initialCapacity(10)
                .maximumWeight(cacheWeight)
                .expireAfterAccess(60, TimeUnit.SECONDS)
                .weigher((Photo p, Image i) -> {
                    double weight = i.getHeight() * i.getWidth() * 4;
                    if (weight < 0) return 0;
                    return weight > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) weight;
                })
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

    public CompletableFuture<Image> loadPhotoAsync(Photo photo) {
        if (photo == null) {
            throw new NullPointerException("Photo cannot be null");
        }

        // Check if photo hit the cache
        Image cached = cache.getIfPresent(photo);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // Check if the photo is already in loading
        CompletableFuture<Image> future = photoTasks.get(photo);
        if (future != null && !future.isDone()) {
            return future;
        }

        CompletableFuture<Image> loadTask = CompletableFuture.supplyAsync(() -> {
            try {
                Image image = render(photo);
                cache.put(photo, image);
                return image;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, executor);

        // Ensure that only the first started task is put in the map
        CompletableFuture<Image> existingTask = photoTasks.putIfAbsent(photo, loadTask);
        if (existingTask != null) {
            return existingTask;
        }

        loadTask.whenComplete((image, ex) -> photoTasks.remove(photo));

        return loadTask;
    }

    public CompletableFuture<Photo> loadPhotoMetadataAsync(Photo photo) {
        if (photo == null) {
            throw new NullPointerException("Photo cannot be null.");
        }

        Photo realPhoto =
                (isScanDone && photoIndex.containsKey(photo)) ? photoPaths.get(photoIndex.get(photo)) : photo;

        if (realPhoto.isAttributesLoaded() && realPhoto.isDimensionsLoaded()) {
            return CompletableFuture.completedFuture(realPhoto);
        }

        return CompletableFuture.supplyAsync(() -> {
            synchronized (realPhoto.getLock()) {
                try {
                    if (!realPhoto.isAttributesLoaded()) {
                        realPhoto.loadImageAttributes();
                        realPhoto.setAttributesLoaded(true);
                    }
                    if (!realPhoto.isDimensionsLoaded()) {
                        realPhoto.loadImageDimensions();
                        realPhoto.setDimensionsLoaded(true);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error loading photo metadata: " + realPhoto, e);
                }
                return realPhoto;
            }
        }, executor).exceptionally(ex -> {
            realPhoto.setAttributesLoaded(false);
            realPhoto.setDimensionsLoaded(false);
            System.err.println(ex.getMessage());
            return null;
        });
    }

    private Image render(Photo photo) throws IOException {
        if (photo == null) {
            throw new NullPointerException("Photo cannot be null.");
        }

        if (photo.getType().equals("gif")) {
            return new Image(photo.getPath().toUri().toString());
        }

        Image result;
        try {
            BufferedImage bufferedImage = ImageIO.read(photo.getPath().toFile());
            
            if (bufferedImage == null) {
                throw new IOException("Failed to load image: " + photo.getPath());
            }

            result = SwingFXUtils.toFXImage(bufferedImage, null);

            if (result.isError()) {
                throw new IOException("Failed to load image: " + photo.getPath());
            }

            return result;
        } catch (IOException e) {
            throw new IOException("Failed to load image: " + photo.getPath(), e);
        }
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
        LoaderTester.loadPhotoTest();

        System.out.println("All tasks completed.");
        System.exit(0);
    }
}

class LoaderTester {
    static void scanDirTest() {
        try {
            PhotoLoader loader = new PhotoLoader();

            Path path = Paths.get(
                    "your/test/path/here");

            var future = loader.scanPathAsync(path);
            loader.scanPath(path);
            if (future != null) {
                future.thenRun(() -> {
                    System.out.println("Scan done.");
                    System.out.println("Photo count: " + loader.getPhotoCount());
                    System.out.println("Photo paths: " + loader.getDirPath());
                }).join();
            } else {
                System.out.println("Scan have already been done.");
            }

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

    static void asyncTest() {
        try {
            Path dir = Paths.get(
                    "your/test/path/here"
            );
            Path img = Paths.get("D:\\KUN\\picture\\wallpaper\\escalator.jpg");

            PhotoLoader loader = new PhotoLoader();
            var scan = loader.scanPathAsync(dir);
            var metadata = loader.loadPhotoMetadataAsync(new Photo(img, true));

            scan.thenRun(() -> {
                System.out.println("Scan done.");
                System.out.println("Photo count: " + loader.getPhotoCount());
                System.out.println("Photo paths: " + loader.getDirPath());
                System.out.println(Thread.currentThread().getName() + "\t|\t" + Thread.currentThread().threadId());
            });

            metadata.thenAccept(p -> {
                System.out.println("Size: " + p.getStorageSizeLiteral());
                System.out.println("Last modified: " + p.getLastModifiedTimeLiteral());
                System.out.println("Dimensions: " + p.getDimensionsLiteral());
                System.out.println(Thread.currentThread().getName() + "\t|\t" + Thread.currentThread().threadId());
            });

            CompletableFuture.allOf(metadata, scan).join();

            loader.cancelTask();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    static void loadPhotoTest() {
        var path = Paths.get("D:\\KUN\\picture\\wallpaper\\escalator.jpg");
        var photo = new Photo(path, true);
        var loader = new PhotoLoader();

        getThreadInfo();
        var loadTask = loader.loadPhotoAsync(photo).thenAccept(image -> {
            getThreadInfo();
            System.out.println("Image loaded: " + image);
            System.out.println("Image width: " + image.getWidth());
            System.out.println("Image height: " + image.getHeight());
            System.out.println();
        }).exceptionally(ex -> {
            System.err.println(ex.getMessage());
            return null;
        });

        getThreadInfo();
        var loadMeta = loader.loadPhotoMetadataAsync(photo).thenAccept(p -> {
            getThreadInfo();
            System.out.println("Metadata loaded: " + p);
            System.out.println("Size: " + p.getStorageSizeLiteral());
            System.out.println("Last modified: " + p.getLastModifiedTimeLiteral());
            System.out.println("Dimensions: " + p.getDimensionsLiteral());
            System.out.println();
        }).exceptionally(ex -> {
            System.err.println(ex.getMessage());
            return null;
        });

        CompletableFuture.allOf(loadTask, loadMeta).join();
        loader.cancelTask();
    }

    static void getThreadInfo() {
        Thread thread = Thread.currentThread();
        System.out.println(
                thread.getName() + "\t|\t" + thread.threadId() + "\t|\t" + System.currentTimeMillis() % 1000
        );
    }
}
