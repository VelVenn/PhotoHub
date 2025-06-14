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
import javafx.scene.image.Image;
import javafx.embed.swing.SwingFXUtils;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.nio.file.*;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import java.util.List;

import java.io.IOException;

import java.io.Closeable;

public class PhotoLoader implements Closeable {
    private final Cache<Photo, Image> cache;

    private final ExecutorService executor;

    private CompletableFuture<Void> dirTask = null;
    private final Map<Photo, CompletableFuture<Image>> photoTasks = new ConcurrentHashMap<>();

    private volatile Path dirPath;
    private volatile List<Photo> photoPaths;
    private Map<Photo, Integer> photoIndex;

    private volatile boolean isScanDone = false;
    private final Object scanLock = new Object();

    private int timeOut = Integer.MAX_VALUE;

    private static final boolean DEBUG = false;

    /**
     * Default constructor, setting the image cache to max 200MiB
     * and use the fixed thread pool, whose size is determined by the number of
     * available cores, but not less than 4 and not more than 16.
     */
    public PhotoLoader() {
        this(209_715_200, true); // 200MiB
    }

    /**
     * Constructor which set the maximum photo amount that can be cached.
     * <p>
     * Use the fixed thread pool, whose size is determined by the number of
     * available cores, but not less than 4 and not more than 16.
     *
     * @param cacheSize the maximum size of the cache in number of photos
     */
    public PhotoLoader(int cacheSize) {
        cache = Caffeine.newBuilder()
                .initialCapacity(10)
                .maximumSize(cacheSize)
                .expireAfterAccess(90, TimeUnit.SECONDS)
                .build();

        int availableCores = Runtime.getRuntime().availableProcessors();
        int executorSize = Math.max(4, Math.min(availableCores, 16));
        executor = Executors.newFixedThreadPool(executorSize);
    }

    /**
     * Constructor which set the maximum photo amount that can be cached
     * and the size of the fixed thread pool.
     *
     * @param cacheSize    the maximum size of the cache in number of photos
     * @param executorSize the size of the executor thread pool
     */
    public PhotoLoader(int cacheSize, int executorSize) {
        cache = Caffeine.newBuilder()
                .initialCapacity(10)
                .maximumSize(cacheSize)
                .expireAfterAccess(90, TimeUnit.SECONDS)
                .build();
        executor = Executors.newFixedThreadPool(executorSize);
    }

    /**
     * Constructor which set the maximum photo amount that can be cached,
     * the size of the fixed thread pool and cache's expiration time
     *
     * @param cacheSize    the maximum size of the cache in number of photos
     * @param executorSize the size of the executor thread pool
     * @param expire       the expiration time of the cache in seconds
     */
    public PhotoLoader(int cacheSize, int executorSize, int expire) {
        cache = Caffeine.newBuilder()
                .initialCapacity(10)
                .maximumSize(cacheSize)
                .expireAfterAccess(expire, TimeUnit.SECONDS)
                .build();
        executor = Executors.newFixedThreadPool(executorSize);
    }

    /**
     * Constructor which set the maximum memory usage of the cache.
     * <p>
     * Use the fixed thread pool, whose size is determined by the number of
     * available cores, but not less than 4 and not more than 16.
     *
     * @param cacheWeight the maximum weight of the cache in bytes
     * @param isWeight    any value will do
     */
    public PhotoLoader(long cacheWeight, boolean isWeight) {
        cache = Caffeine.newBuilder()
                .initialCapacity(10)
                .maximumWeight(cacheWeight)
                .expireAfterAccess(90, TimeUnit.SECONDS)
                .weigher((Photo p, Image i) -> {
                    double weight = i.getHeight() * i.getWidth() * 4; // Estimate as ARGB, assume 1 byte per channel
                    if (weight < 0) return 0;
                    return weight > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) weight;
                })
                .build();

        int availableCores = Runtime.getRuntime().availableProcessors();
        int executorSize = Math.max(4, Math.min(availableCores, 16));
        executor = Executors.newFixedThreadPool(executorSize);
    }

    /**
     * Constructor which set the maximum memory usage of the cache
     * and the size of the fixed thread pool.
     *
     * @param cacheWeight  the maximum weight of the cache in bytes
     * @param executorSize the size of the executor thread pool
     * @param isWeight     any value will do
     */
    public PhotoLoader(long cacheWeight, int executorSize, boolean isWeight) {
        cache = Caffeine.newBuilder()
                .initialCapacity(10)
                .maximumWeight(cacheWeight)
                .expireAfterAccess(90, TimeUnit.SECONDS)
                .weigher((Photo p, Image i) -> {
                    double weight = i.getHeight() * i.getWidth() * 4;
                    if (weight < 0) return 0;
                    return weight > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) weight;
                })
                .build();
        executor = Executors.newFixedThreadPool(executorSize);
    }

    /**
     * Constructor which set the maximum memory usage of the cache,
     * the size of the fixed thread pool, cache's expiration time,
     * and the timeout for loading images.
     *
     * @param cacheWeight  the maximum weight of the cache in bytes
     * @param executorSize the size of the executor thread pool
     * @param expire       the expiration time of the cache in seconds
     * @param timeOut      the timeout for loading images in seconds
     */
    public PhotoLoader(long cacheWeight, int executorSize, int expire, int timeOut) {
        cache = Caffeine.newBuilder()
                .initialCapacity(10)
                .maximumWeight(cacheWeight)
                .expireAfterAccess(expire, TimeUnit.SECONDS)
                .weigher((Photo p, Image i) -> {
                    double weight = i.getHeight() * i.getWidth() * 4;
                    if (weight < 0) return 0;
                    return weight > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) weight;
                })
                .build();
        executor = Executors.newFixedThreadPool(executorSize);
        this.timeOut = timeOut > 0 ? timeOut : Integer.MAX_VALUE;
    }

    public void scanPath(Path path) throws IOException {
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

            try {
                validateDirectory(path);
            } catch (IOException e) {
                isScanDone = false;
                photoPaths = Collections.emptyList();
                photoIndex = Collections.emptyMap();
                throw new IOException("Error validating path: " + path, e);
            }

            try (Stream<Path> pathStream = Files.list(path)) {
                referenceBuilder(pathStream);
            } catch (IOException e) {
                isScanDone = false;
                photoPaths = Collections.emptyList();
                photoIndex = Collections.emptyMap();
                throw new IOException("Error scanning path: " + path, e);
            }

            dirTask = CompletableFuture.completedFuture(null);
        }
    }

    public CompletableFuture<Void> scanPathAsync(Path path) {
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
            // Double-check to avoid repeated work:
            // Multiple threads may reach here and compete for the lock at the same time.
            // The first thread will start the task; others, after acquiring the lock,
            // must check again to avoid starting a duplicate task
            // if the first one already finished.

            dirTask = CompletableFuture.runAsync(() -> {
                if (DEBUG) {
                    Logger.log("Scanning path: " + path);
                }

                try {
                    validateDirectory(path);
                } catch (IOException e) {
                    throw new CompletionException("Error validating path: " + path, e);
                }

                try (Stream<Path> pathStream = Files.list(path)) {
                    referenceBuilder(pathStream);
                } catch (IOException e) {
                    throw new RuntimeException("Error scanning path: " + path, e);
                }
            }, executor).whenComplete((v, ex) -> {
                if (ex != null) {
                    isScanDone = false;
                    photoPaths = Collections.emptyList();
                    photoIndex = Collections.emptyMap();
                    if (DEBUG) Logger.logErr("Failure: " + path, ex);
                }

                if (DEBUG) {
                    Logger.log("Scan completed: " + path);
                }
            });
        }

        return dirTask;
    }

    //TODO New cache strategy for gif file should be considered, currently it is simply avoid from being cached.
    public CompletableFuture<Image> loadPhotoAsync(Photo photo) {
        if (photo == null) {
            return CompletableFuture.failedFuture(new NullPointerException("Photo cannot be null."));
        }

        Photo realPhoto =
                (isScanDone && photoIndex.containsKey(photo)) ? photoPaths.get(photoIndex.get(photo)) : photo;

        // Check if photo hit the cache
        Image cached = cache.getIfPresent(realPhoto);
        if (cached != null) {
            if (DEBUG) Logger.log("Cache hit: " + realPhoto.getName());
            return CompletableFuture.completedFuture(cached);
        }

        // Check if the photo is already in loading
        CompletableFuture<Image> future = photoTasks.get(realPhoto);
        if (future != null && !future.isDone()) {
            return future;
        }

        CompletableFuture<Image> loadTask = CompletableFuture.supplyAsync(() -> {
                    try {
                        Image image = render(realPhoto);
                        if (!realPhoto.getType().equals("gif")) {
                            cache.put(realPhoto, image);
                        }
                        if (DEBUG) {
                            Logger.log("Render ended: " + realPhoto.getName());
                        }
                        return image;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, executor)
                .orTimeout(timeOut, TimeUnit.SECONDS);

        // Ensure that only the first started task is put in the map
        CompletableFuture<Image> existingTask = photoTasks.putIfAbsent(realPhoto, loadTask);
        if (existingTask != null) {
            if (!loadTask.isDone()) {
                loadTask.cancel(true);
            }

            return existingTask;
        }

        loadTask.whenComplete((image, ex) -> photoTasks.remove(realPhoto));

        return loadTask;
    }

    //TODO New cache strategy for gif file should be considered, now is simply avoided from being pre-loaded
    public CompletableFuture<Void> preLoadPhotosAsync(int curIndex, int preloadCount) {
        CompletableFuture<Void> preLoadTask = CompletableFuture.supplyAsync(() -> {
            if (!isScanDone || photoPaths == null || photoPaths.isEmpty()) {
                return List.<CompletableFuture<Image>>of();
            }

            if (curIndex < 0 || curIndex >= photoPaths.size()) {
                throw new IndexOutOfBoundsException("Current index is out of bounds.");
            }

            if (preloadCount <= 0) {
                throw new IllegalArgumentException("Preload count is invalid.");
            }

            int total = photoPaths.size() - 1;
            int start = Math.max(0, curIndex - preloadCount);
            int end = Math.min(total, curIndex + preloadCount);

            // For the basic-type stream (such as int, long, double), the map
            // operation could only return the same type of stream.
            // use mapToObj to convert to other types' stream
            return IntStream.range(start, end + 1) // start <= i < end + 1
                    .filter(i -> i != curIndex)
                    .mapToObj(i -> photoPaths.get(i))
                    .filter(photo -> !photo.getType().equals("gif"))
                    .filter(photo -> cache.getIfPresent(photo) == null)
                    .map(this::loadPhotoAsync)
                    .toList(); // toArray here may cause type unsafety

        }, executor).thenCompose(futures -> {
            if (futures == null || futures.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])); // prevent type erasure
        });

        preLoadTask.whenComplete((v, ex) -> {
            if (ex != null) {
                if (DEBUG) Logger.logErr("Preload batch failed: " + preLoadTask, ex);
            }
        });

        return preLoadTask;
    }

    public CompletableFuture<Photo> loadPhotoMetadataAsync(Photo photo) {
        if (photo == null) {
            return CompletableFuture.failedFuture(new NullPointerException("Photo cannot be null."));
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
        }, executor).whenComplete((v, ex) -> {
            if (ex != null) {
                realPhoto.setAttributesLoaded(false);
                realPhoto.setDimensionsLoaded(false);
                if (DEBUG) Logger.logErr("Load metadata failed: " + realPhoto.getName(), ex);
            }
        });
    }

    protected Image render(Photo photo) throws IOException {
        if (photo == null) {
            throw new NullPointerException("Photo cannot be null.");
        }

        if (DEBUG) {
            Logger.log("Rendering " + photo.getName());
        }

        if (photo.getType().equals("gif")) {
            return new Image(photo.getPath().toUri().toString());
        }

        Image result;
        try {
            BufferedImage bufferedImage = ImageIO.read(photo.getPath().toFile()); // The webp images' color may bias due to TwelveMonkeys' bug

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

    private void cancelTask() {
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

    /**
     * 获取当前目录下所有已索引的图片列表的快照。
     * <p>
     * 返回的列表是不可变的，仅在目录扫描 ({@link #scanPath(Path)}/{@link #scanPathAsync(Path)})
     * 完成后可用。
     * <p>
     * <b>注意：</b>此方法返回的列表顺序与索引一致，但直接遍历此列表进行图片访问
     * 可能导致并发一致性问题或性能下降。推荐优先使用
     * {@link #getPhotoIndex(Photo)} 获取图片索引，
     * 或 {@link #getPhotoByIndex(int)} 通过索引访问图片，以获得更好的健壮性和性能。
     *
     * @return 当前目录下所有图片的不可变列表，若尚未扫描完成则返回 {@code null}
     * @see #getPhotoIndex(Photo)
     * @see #getPhotoByIndex(int)
     */
    public List<Photo> getPhotoPaths() {
        if (!isScanDone) {
            return null;
        }

        return List.copyOf(photoPaths);
    }

    public int getPhotoIndex(Photo photo) {
        if (!isScanDone) {
            return -1;
        }

        return photoIndex.getOrDefault(photo, -1);
    }

    public Photo getPhotoByIndex(int index) {
        if (!isScanDone) {
            return null;
        }

        if (index < 0 || index >= photoPaths.size()) {
            throw new IndexOutOfBoundsException("Index is out of bounds.");
        }

        return photoPaths.get(index);
    }

    public Path getDirPath() {
        return dirPath.normalize().toAbsolutePath();
    }

    /**
     * Checks if this PhotoLoader is ready for index-based operations,
     * i.e., directory scan is done and all core data structures are initialized.
     * <p>
     * Returns {@code true} only if the scan is done and {@code dirPath},
     * {@code photoPaths}, and {@code photoIndex} are all non-null.
     * <p>
     * Thread-safe for status checking, but does not guarantee the state remains
     * unchanged after the call.
     *
     * @return {@code true} if the loader is ready for index-based operations, {@code false} otherwise
     */
    public boolean isIndexBasedUsable() {
        return isScanDone && dirPath != null && photoPaths != null && photoIndex != null;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    @Override
    public void close() {
        cancelTask();
    }

    private void validateDirectory(Path path) throws NoSuchFileException, AccessDeniedException {
        if (path == null) {
            throw new NullPointerException("Path cannot be null.");
        }

        path = path.normalize().toAbsolutePath();

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

    private void referenceBuilder(Stream<Path> pathStream) {
        List<Photo> tmpPhotoPaths = pathStream
                .filter(Photos::isValidPhoto)
                .map(p -> new Photo(p, true))
                .toList();

        Map<Photo, Integer> tmpPhotoIndex = IntStream
                .range(0, tmpPhotoPaths.size())
                .boxed() // Box the int to Integer
                .collect(Collectors.toMap(tmpPhotoPaths::get, i -> i));

        photoPaths = tmpPhotoPaths;
        photoIndex = new ConcurrentHashMap<>(tmpPhotoIndex);

        isScanDone = true;
    }
}