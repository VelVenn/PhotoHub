package io.loraine.photohub.photo;

import io.loraine.photohub.util.Logger;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoaderManager {
    private volatile static LoaderManager instance;

    private final Map<Path, LoaderReference> referenceMap = new ConcurrentHashMap<>();

    private LoaderManager() {
    }

    private static final Object instanceGetterLock = new Object();

    private static final boolean DEBUG = true;

    public static LoaderManager getInstance() {
        if (instance == null) {
            synchronized (instanceGetterLock) {
                if (instance == null) {
                    instance = new LoaderManager();
                }
            }
        }
        return instance;
    }

    public PhotoLoader acquire(Path directory) {
        LoaderReference reference = referenceMap.computeIfAbsent(directory, dir -> {
            PhotoLoader loader = new PhotoLoader(
                    500 * 1024 * 1024,
                    20,
                    60,
                    20
            );

            loader.scanPathAsync(dir).exceptionally(ex -> {
                if (DEBUG) Logger.logErr("Error scanning path: " + dir, ex);
                return null;
            });

            if (DEBUG) Logger.log("New loader created for: " + dir);

            return new LoaderReference(loader);
        });
        reference.retain();
        return reference.getLoader();
    }

    public void release(Path directory) {
        referenceMap.computeIfPresent(directory, (dir, ref) -> {
            if (ref.release()) {
                return null;
            }
            return ref;
        });
    }

    private static class LoaderReference {
        private final PhotoLoader loader;
        private volatile int referenceCount = 0;

        public LoaderReference(PhotoLoader loader) {
            this.loader = loader;
        }

        synchronized void retain() {
            referenceCount++;
        }

        synchronized Boolean release() {
            if (referenceCount > 0) {
                referenceCount--;
            }

            if (referenceCount == 0) {
                loader.close();
                if (DEBUG) Logger.logErr("Loader released: " + loader.getDirPath());
                return true;
            }

            return false;
        }

        PhotoLoader getLoader() {
            return loader;
        }
    }
}
