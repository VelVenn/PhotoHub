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

package io.loraine.photohub.viewer;

import io.loraine.photohub.photo.*;

import javafx.application.Platform;
import javafx.beans.property.*;

import javafx.beans.value.ChangeListener;
import javafx.scene.image.Image;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ViewProperty {
    private final IntegerProperty curIdx = new SimpleIntegerProperty(this, "curIdx", -1);
    private final IntegerProperty photoCount = new SimpleIntegerProperty(this, "photoCount", -1);

    private final DoubleProperty curHeight = new SimpleDoubleProperty(this, "curHeight", 0);
    private final DoubleProperty curWidth = new SimpleDoubleProperty(this, "curWidth", 0);

    private final DoubleProperty curZoom = new SimpleDoubleProperty(this, "curZoom", -1.0);

    private final ObjectProperty<Photo> curPhoto = new SimpleObjectProperty<>(this, "curPhoto", null);
    private final ObjectProperty<Image> displayImg = new SimpleObjectProperty<>(this, "displayImg", null);

    private final StringProperty displayName = new SimpleStringProperty(this, "displayName", "N/A");
    private final StringProperty displaySize = new SimpleStringProperty(this, "displaySize", "N/A");
    private final StringProperty displayDimension = new SimpleStringProperty(this, "displayDimension", "N/A x N/A");
    private final StringProperty displayIdx = new SimpleStringProperty(this, "displayIdx", "N/A of N/A");
    private final StringProperty displayZoom = new SimpleStringProperty(this, "displayZoom", "N/A %");
    private final StringProperty displayType = new SimpleStringProperty(this, "displayType", "N/A");
    private final StringProperty displayLastModified = new SimpleStringProperty(this, "displayLastModified", "N/A");

    private final StringProperty errMsg = new SimpleStringProperty(this, "errMsg", null);

    private final BooleanProperty isImgLoading = new SimpleBooleanProperty(this, "isImgLoading", false);
    private final BooleanProperty isPlaying = new SimpleBooleanProperty(this, "isPlaying", false);
    private final BooleanProperty isFitted = new SimpleBooleanProperty(this, "isFitted", false);
    private final BooleanProperty isScanDone = new SimpleBooleanProperty(this, "isScanDone", false);

    private final PhotoLoader loader;
    private final Boolean DEBUG = true;

    public ViewProperty(PhotoLoader photoLoader, Photo photo) throws IOException {
        if (photoLoader == null || photo == null) {
            throw new NullPointerException("PhotoLoader cannot be null");
        }

        loader = photoLoader;

        Photos.validatePath(photo.getPath());
        curPhoto.set(photo);

        setPhotoListener();
        setIdxListener();
        setScaleListener();
    }

    private void setPhotoListener() {

        ChangeListener<Photo> photoListener = (o, oldV, newV) -> {
            updatePhotoMeta(null);

            if (newV != null) {
                System.out.println("CurPhoto changed to " + newV.getName());

                loadImg(newV);
                loadPhotoMeta(newV);
                curIdx.set(loader.getPhotoIndex(newV));
                loader.preLoadPhotosAsync(curIdx.get(), 3);
            }
        };

        curPhoto.addListener(photoListener);

        // 注释掉下面的两行如果想要手动触发初始图片的加载
        Photo initPhoto = curPhoto.get();
        Platform.runLater(() -> photoListener.changed(curPhoto, null, initPhoto));
    }

    private void setIdxListener() {
        ChangeListener<Number> changeListener = (o, oldV, newV) -> Platform.runLater(() -> {
            int idx = curIdx.get();
            int all = photoCount.get();

            if (idx >= 0 && all > 0 && idx < all) {
                displayIdx.set((idx + 1) + " of " + all);
            } else if (all == 0) {
                displayIdx.set("0 of 0");
            } else {
                displayIdx.set("N/A of N/A");
            }
        });

        curIdx.addListener(changeListener);
        photoCount.addListener(changeListener);
    }

    private void setScaleListener() {
        ChangeListener<Object> changeListener = (o, oldV, newV) -> Platform.runLater(() -> {
            String msg = isFitted.get() ? "Fit: " : "";
            double scale = curZoom.get();

            if (scale > 0) {
                displayZoom.set(msg + String.format("%.2f %%", scale * 100));
            } else {
                displayZoom.set("N/A %");
            }
        });

        curZoom.addListener(changeListener);
        isFitted.addListener(changeListener);

        // initialize first display
        Platform.runLater(() -> changeListener.changed(null, null, null));
    }

    CompletableFuture<Void> initScanDir() {
        Photo current = curPhoto.get();
        isScanDone.set(false);

        try {
            return loader.scanPathAsync(current.getParent())
                    .thenRun(() -> {
                        isScanDone.set(true);
                        curIdx.set(loader.getPhotoIndex(current));
                        photoCount.set(loader.getPhotoCount());
                    })
                    .exceptionally(ex -> {
                        isScanDone.set(false);
                        setError("Scan directory failed: " + ex.getMessage());
                        if (DEBUG) {
                            System.err.println(
                                    ex.getClass().getName() + ": " + ex.getMessage() + " Cause: " + ex.getCause());
                        }
                        return null;
                    });
        } catch (Exception e) {
            isScanDone.set(false);
            String msg = "Validation error: " + e.getMessage();
            setError(msg);

            if (DEBUG) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage() + " Cause: " + e.getCause());
            }

            return CompletableFuture.failedFuture(new RuntimeException(msg, e.getCause()));
        }
    }

    void loadPhotoMeta(Photo photo) {
        updatePhotoMeta(null);

        if (photo == null) {
            return;
        }

        loader.loadPhotoMetadataAsync(photo).thenAccept(this::updatePhotoMeta).exceptionally(ex -> {
            String msg = "Load metadata failed: " + ex.getMessage();
            setError(msg);

            if (DEBUG) System.err.println(msg);
            return null;
        });
    }

    void loadImg(Photo photo) {
        isImgLoading.set(true);
        displayImg.set(null);

        if (photo == null) {
            return;
        }

        loader.loadPhotoAsync(photo).thenAccept(img -> Platform.runLater(() -> {
                    if (photo.equals(curPhoto.get())) {
                        if (displayImg.get() != img) {
                            displayImg.set(img); // Avoid duplicated setting
                        }

                        isImgLoading.set(false);
                    }
                }))
                .exceptionally(ex -> {
                    String msg = "Load photo failed: " + ex.getMessage();

                    if (photo.equals(curPhoto.get())) {
                        displayImg.set(null);
                        isImgLoading.set(false);
                        setError(msg);
                    }

                    if (DEBUG) System.err.println(msg);
                    return null;
                });
    }

    private void updatePhotoMeta(Photo photo) {
        Platform.runLater(() -> {
            try {
                if (photo != null && photo.isDimensionsLoaded() && photo.isAttributesLoaded()) {
                    displayName.set(photo.getName());
                    displaySize.set(photo.getStorageSizeLiteral());
                    displayDimension.set(photo.getDimensionsLiteral());
                    displayType.set(photo.getType());
                    displayLastModified.set(photo.getLastModifiedTimeLiteral());

                    curHeight.set(photo.getHeight());
                    curWidth.set(photo.getWidth());
                } else {
                    displayName.set("N/A");
                    displaySize.set("N/A");
                    displayDimension.set("N/A x N/A");
                    displayType.set("N/A");
                    displayLastModified.set("N/A");

                    curHeight.set(0);
                    curWidth.set(0);
                }
            } catch (Exception e) {
                String msg = "Failed to load metadata: " + e.getMessage();
                setError(msg);
                if (DEBUG)
                    System.err.println(e.getClass().getName() + ": " + e.getMessage() + " Cause: " + e.getCause());
            }
        });
    }

    public void setError(String msg) {
        Platform.runLater(() -> {
            errMsg.set(null);
            errMsg.set(msg);
            errMsg.set(null);
        });
    }

    public void resetError() {
        Platform.runLater(() -> errMsg.set(null));
    }

    public IntegerProperty curIdxProperty() {
        return curIdx;
    }

    public IntegerProperty photoCountProperty() {
        return photoCount;
    }

    public DoubleProperty curHeightProperty() {
        return curHeight;
    }

    public DoubleProperty curWidthProperty() {
        return curWidth;
    }

    public DoubleProperty curZoomProperty() {
        return curZoom;
    }

    public ObjectProperty<Photo> curPhotoProperty() {
        return curPhoto;
    }

    public ObjectProperty<Image> displayImgProperty() {
        return displayImg;
    }

    public StringProperty displayNameProperty() {
        return displayName;
    }

    public StringProperty displaySizeProperty() {
        return displaySize;
    }

    public StringProperty displayDimensionProperty() {
        return displayDimension;
    }

    public StringProperty displayIdxProperty() {
        return displayIdx;
    }

    public StringProperty displayZoomProperty() {
        return displayZoom;
    }

    public StringProperty displayTypeProperty() {
        return displayType;
    }

    public StringProperty displayLastModifiedProperty() {
        return displayLastModified;
    }

    public StringProperty errMsgProperty() {
        return errMsg;
    }

    public BooleanProperty isFittedProperty() {
        return isFitted;
    }

    public BooleanProperty isImgLoadingProperty() {
        return isImgLoading;
    }

    public BooleanProperty isPlayingProperty() {
        return isPlaying;
    }

    public BooleanProperty isScanDoneProperty() {
        return isScanDone;
    }
}
