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

/*
 * This test class for PhotoLoader can only be run in the IntelliJ IDEA IDE normally.
 * It is not intended to be run in the command line or any other IDE.
 *
 * If you want to run it in other ways, we recommend you to use wrapper class LoaderTesterForMvn
 * instead or write the test class or JUnit test yourself.
 */

package io.loraine.photohub.demo;

import io.loraine.photohub.photo.Photo;
import io.loraine.photohub.photo.PhotoLoader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

class ScanDirTest {
    public static void main(String[] args) {
        try {
            PhotoLoader loader = new PhotoLoader();

            Path path = Paths.get(
                    "your/photo/dir"); // Change this to your photo directory

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

            loader.close();
        } catch (IOException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
}

class MetadataLaterTest {
    public static void main(String[] args) {
        try {
            PhotoLoader loader = new PhotoLoader();

            var path = Paths.get("your/test/photo/here.jpg"); // Change this to your photo path

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

            loader.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
}

class AsyncTest {
    public static void main(String[] args) {
        try {
            Path dir = Paths.get(
                    "path/to/your/photo/directory" // Change this to your photo directory
            );

            Path img = Paths.get(Objects.requireNonNull(LoadPhotoTest.class.getResource(
                    "/io/loraine/photohub/Default_Resources/Escalator.jpg")).toURI());

            PhotoLoader loader = new PhotoLoader();
            var scan = loader.scanPathAsync(dir);
            var metadata = loader.loadPhotoMetadataAsync(new Photo(img, true));

            scan.thenRun(() -> {
                System.out.println("Scan done.");
                System.out.println("Photo count: " + loader.getPhotoCount());
                System.out.println("Photo paths: " + loader.getDirPath());
                Utils.getThreadInfo();
            });

            metadata.thenAccept(p -> {
                System.out.println("Size: " + p.getStorageSizeLiteral());
                System.out.println("Last modified: " + p.getLastModifiedTimeLiteral());
                System.out.println("Dimensions: " + p.getDimensionsLiteral());
                Utils.getThreadInfo();
            });

            CompletableFuture.allOf(metadata, scan).join();

            loader.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
}

class LoadPhotoTest {
    public static void main(String[] args) {
        Path path;

        try {
            path = Paths.get(Objects.requireNonNull(LoadPhotoTest.class.getResource(
                    "/io/loraine/photohub/Default_Resources/Escalator.jpg")).toURI());
        } catch (URISyntaxException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            return;
        }

        var photo = new Photo(path, true);
        var loader = new PhotoLoader();

        Utils.getThreadInfo();
        var loadTask = loader.loadPhotoAsync(photo).thenAccept(image -> {
            Utils.getThreadInfo();
            System.out.println("Image loaded: " + image);
            System.out.println("Image width: " + image.getWidth());
            System.out.println("Image height: " + image.getHeight());
            System.out.println();
        }).exceptionally(ex -> {
            System.err.println(ex.getMessage());
            return null;
        });

        Utils.getThreadInfo();
        var loadMeta = loader.loadPhotoMetadataAsync(photo).thenAccept(p -> {
            Utils.getThreadInfo();
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

        Utils.getThreadInfo();
        loader.loadPhotoAsync(photo).thenAccept(image -> {
            Utils.getThreadInfo();
            System.out.println("Image loaded Again: " + image);
        }).exceptionally(ex -> {
            System.err.println(ex.getMessage());
            return null;
        });

        loader.close();
    }
}

class preLoadTest {
    public static void main(String[] args) {
        Path path = Paths.get("your/photo/dir"); // Change this to your photo directory
        Path escalator = Paths.get("your/photo/dir/image.jpg"); // Change this to your photo path

        try (PhotoLoader loader = new PhotoLoader()) {
            var future = loader.scanPathAsync(path);
            future.thenRun(() -> {
                System.out.println("Scan done.");
                var loadSingle = loader.loadPhotoAsync(new Photo(escalator, true));
                var preLoad = loader.preLoadPhotosAsync(1, 5);

                if (loadSingle != null) {
                    loadSingle.thenAccept(image -> {
                        Utils.getThreadInfo();
                        System.out.println("Image loaded: " + image);
                        System.out.println("Image width: " + image.getWidth());
                        System.out.println("Image height: " + image.getHeight());
                        Utils.getThreadInfo();
                        System.out.println();
                    }).exceptionally(ex -> {
                        System.err.println(ex.getMessage());
                        return null;
                    });
                } else {
                    System.out.println("Load single photo failed.");
                }

                if (preLoad != null) {
                    preLoad.thenRun(() -> {
                        System.out.println("Preload done.");

                        var f0 = loader.loadPhotoAsync(loader.getPhotoByIndex(0)).thenAccept(image -> {
                            Utils.getThreadInfo();
                            System.out.println("Image loaded: " + image);
                            System.out.println("Image width: " + image.getWidth());
                            System.out.println("Image height: " + image.getHeight());
                            Utils.getThreadInfo();
                            System.out.println();
                        }).exceptionally(ex -> {
                            System.err.println(ex.getMessage());
                            return null;
                        });

                        var f3 = loader.loadPhotoAsync(loader.getPhotoByIndex(3)).thenAccept(image -> {
                            Utils.getThreadInfo();
                            System.out.println("Image loaded: " + image);
                            System.out.println("Image width: " + image.getWidth());
                            System.out.println("Image height: " + image.getHeight());
                            Utils.getThreadInfo();
                            System.out.println();
                        }).exceptionally(ex -> {
                            System.err.println(ex.getMessage());
                            return null;
                        });

                        var f6 = loader.loadPhotoAsync(loader.getPhotoByIndex(6)).thenAccept(image -> {
                            Utils.getThreadInfo();
                            System.out.println("Image loaded: " + image);
                            System.out.println("Image width: " + image.getWidth());
                            System.out.println("Image height: " + image.getHeight());
                            Utils.getThreadInfo();
                            System.out.println();
                        }).exceptionally(ex -> {
                            System.err.println(ex.getMessage());
                            return null;
                        });

                        CompletableFuture.allOf(f0, f3, f6).join();
                    });

                    preLoad.exceptionally(ex -> {
                        System.err.println(ex.getMessage());
                        return null;
                    });
                } else {
                    System.out.println("Preload failed.");
                }

                CompletableFuture.allOf(loadSingle, preLoad).join();
            }).join();
        }
    }
}

class Utils {
    private Utils() {
    }

    static void getThreadInfo() {
        Thread thread = Thread.currentThread();
        System.out.println(
                thread.getName() + "\t|\t" + thread.threadId() + "\t|\t" + System.currentTimeMillis() % 1000
        );
    }
}

class pathTest {
    public static void foo (Path path) {
        path = path.normalize().toAbsolutePath();
        System.out.println("Path: " + path);
    }

    public static void main(String[] args) {
        Path path = Paths.get("a/../b");
        foo(path);
        System.out.println("Path: " + path);
    }
}