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

import io.loraine.photohub.photo.Photo;
import io.loraine.photohub.photo.Photos;
import io.loraine.photohub.photo.PhotoLoader;

import io.loraine.photohub.util.Logger;
import javafx.animation.PauseTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ChangeListener;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import javafx.util.Duration;

import javafx.application.Platform;

import java.nio.file.Path;

import org.controlsfx.control.StatusBar;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXComboBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;

import static javafx.geometry.Orientation.VERTICAL;

public class ViewController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private HBox topHBox;

    @FXML
    private Region topFilling;

    @FXML
    private StackPane centerStackPane;

    @FXML
    private ImageView photoView;

    @FXML
    private StatusBar photoStatus;

    @FXML
    private JFXButton prevButton;
    @FXML
    private JFXButton nextButton;
    @FXML
    private JFXButton zoomInButton;
    @FXML
    private JFXButton zoomOutButton;

    @FXML
    private JFXButton playButton;
    @FXML
    private FontIcon playIcon;
    @FXML
    private FontIcon pauseIcon;

    @FXML
    private JFXComboBox<String> sizeCombo;
    @FXML
    private JFXComboBox<String> timeCombo;

    @FXML
    private JFXSlider zoomSlider;

    @FXML
    private Label loadMsg;

    private final Label photoSize = new Label("N/A");
    private final Label photoName = new Label("N/A");
    private final Label photoLastModified = new Label("N/A");
    private final Label photoDimension = new Label("N/A");
    private final Label photoType = new Label("N/A");
    private final Label photoZoom = new Label("N/A %");
    private final Label photoIdx = new Label("N/A of N/A");

    private final Separator errSeparator = new Separator(VERTICAL);
    private final Label errMsg = new Label();
    private final PauseTransition hideTimer = new PauseTransition();

    private final PhotoLoader loader;
    private ViewProperty viewProperty;
    private Timeline slideShowTimeline;

    private static final boolean DEBUG = true;

    private ViewController() {
        this.loader = null;
        this.viewProperty = null;
    }

    /**
     * Constructor for ViewController, controller for the Photoview.fxml
     * <p>
     * The {@code PhotoLoader} loader is a closeable instance for loading photos
     * and should have the same lifetime as the controller.
     *
     * @param initPhotoPath Path of the initial photo
     * @param loader        PhotoLoader instance
     * @throws IOException if the path is invalid or the loader is null
     */
    public ViewController(Path initPhotoPath, PhotoLoader loader) throws IOException {
        if (initPhotoPath == null || loader == null) {
            throw new NullPointerException("Path and loader cannot be null.");
        }

        Photos.validatePath(initPhotoPath);

        this.loader = loader;

        viewProperty = new ViewProperty(loader, new Photo(initPhotoPath, true));
    }

    private void toNextPhoto() {
        if (isIndexAndCurPhotoInvalid()) {
            return;
        }

        viewProperty.isFittedProperty().set(true);

        Photo curPhoto = viewProperty.curPhotoProperty().get();

        int curIdx = loader.getPhotoIndex(curPhoto);
        int amount = loader.getPhotoCount();

        int nextIdx = (curIdx + 1) % amount;

        Photo newPhoto = loader.getPhotoByIndex(nextIdx);
        viewProperty.curPhotoProperty().set(newPhoto);
    }

    private void toPrevPhoto() {
        if (isIndexAndCurPhotoInvalid()) {
            return;
        }

        viewProperty.isFittedProperty().set(true);

        Photo curPhoto = viewProperty.curPhotoProperty().get();

        int curIdx = loader.getPhotoIndex(curPhoto);
        int amount = loader.getPhotoCount();

        int prevIdx = (curIdx - 1 + amount) % amount;

        Photo newPhoto = loader.getPhotoByIndex(prevIdx);
        viewProperty.curPhotoProperty().set(newPhoto);
    }

    private void playPhoto() {
        if (isIndexAndCurPhotoInvalid()) {
            return;
        }

        boolean curPlayStat = !viewProperty.isPlayingProperty().get();
        viewProperty.isPlayingProperty().set(curPlayStat);
    }

    private void startSlideShow() {
        if (slideShowTimeline != null) {
            slideShowTimeline.stop();
        }

        String timeStr = String.valueOf(timeCombo.getValue());

        Duration gap = parseTimeToSeconds(timeStr);

        KeyFrame player = new KeyFrame(gap, e -> {
            if (!isIndexAndCurPhotoInvalid()) {
                toNextPhoto();
            } else {
                viewProperty.isPlayingProperty().set(false);
            }
        });

        slideShowTimeline = new Timeline(player);
        slideShowTimeline.setCycleCount(Timeline.INDEFINITE);
        slideShowTimeline.play();

        if (DEBUG)
            Logger.log("Slide show started with gap: " + gap.toSeconds() + " seconds");
    }

    private void stopSlideShow() {
        if (slideShowTimeline != null) {
            slideShowTimeline.stop();
        }

        if (DEBUG) {
            Logger.log("Slide show stopped");
        }
    }

    /**
     * Parse a time string to seconds
     *
     * @param timeStr Time string, e.g. "1s", "2s", "3s"
     * @return {@code javafx.util.Duration} in seconds if {@code timeStr} is valid
     * else return {@code javafx.util.Duration.seconds(1.0)}
     */
    private Duration parseTimeToSeconds(String timeStr) {
        if (timeStr == null || !timeStr.endsWith("s")) {
            return Duration.seconds(1.0);
        }

        try {
            double seconds = Double.parseDouble(timeStr.substring(0, timeStr.length() - 1));
            seconds = seconds > 0 ? seconds : 1.0;

            return Duration.seconds(seconds);
        } catch (NumberFormatException e) {
            String msg = String.format("Invalid time string: %s", timeStr);
            showErrorLater(msg, Duration.seconds(10));

            if (DEBUG)
                Logger.logErr(msg, e);
            return Duration.seconds(1.0);
        }
    }

    /**
     * 检查当前的索引和照片是否无效
     *
     * @return 返回 {@code true} 如果索引或照片无效，否则返回 {@code false}
     */
    private boolean isIndexAndCurPhotoInvalid() {
        if (!loader.isIndexBasedUsable()) {
            String msg = "Indexing is failed or still in progress.";
            showError(msg, Duration.seconds(15));

            if (DEBUG)
                Logger.logErr(msg);
            return true;
        }

        Photo current = viewProperty.curPhotoProperty().get();
        if (current == null) {
            String msg = "Current photo is null.";
            showError(msg, Duration.seconds(15));

            if (DEBUG)
                Logger.logErr(msg);
            return true;
        }

        return false;
    }

    private void zoomScaleByCombo() {
        String selected = String.valueOf(sizeCombo.getValue());
        double scale = parsePercentLiteral(selected);

        setZoom(scale);
    }

    /**
     * 将当前图片显示比例放大 {@code 10%}，最大 {@code 500%}
     */
    private void zoomInScale() {
        double scale = getPhotoViewScale(photoView.getImage());

        if (scale > 0) {
            scale = Math.min(5.0, scale + 0.1);
        }

        setZoom(scale);
    }

    /**
     * 如果当前图片显示比例大于 {@code 10%}，则缩小 {@code 10%}，最小 {@code 10%}
     */
    private void zoomOutScale() {
        double scale = getPhotoViewScale(photoView.getImage());

        if (scale > 0.1) {
            scale = Math.max(0.1, scale - 0.1);
        }

        setZoom(scale);
    }

    private void zoomBySlider() {
        viewProperty.isFittedProperty().set(false);
        double scale = zoomSlider.getValue();
        setZoom(scale);
    }

    /**
     * 设置图片的缩放模式与缩放比例
     *
     * @param scale 缩放比例，如果大于 {@code 0} 则为自由缩放模式，否则为自适应缩放模式。
     *              {@code scale} 大于 {@code 0} 时，将当前图片的缩放比例设置为
     *              {@code scale}
     */
    private void setZoom(double scale) {
        photoView.fitHeightProperty().unbind();
        photoView.fitWidthProperty().unbind();

        if (scale > 0) {
            // Free scale mode
            viewProperty.isFittedProperty().set(false);

            Image image = photoView.getImage();
            if (image != null) {
                photoView.setFitWidth(image.getWidth());
                photoView.setFitHeight(image.getHeight());

                photoView.setScaleX(scale);
                photoView.setScaleY(scale);
            } else {
                String msg = "Image is unloaded.";
                // showError(msg, Duration.seconds(3));

                if (DEBUG)
                    Logger.logErr(msg);
            }
            clampViewOffset();
        } else {
            viewProperty.isFittedProperty().set(true);

            photoView.fitWidthProperty().bind(centerStackPane.widthProperty());
            photoView.fitHeightProperty().bind(centerStackPane.heightProperty());

            photoView.setScaleX(1.0);
            photoView.setScaleY(1.0);

            photoView.setTranslateX(0);
            photoView.setTranslateY(0);
        }
    }

    /**
     * 控制图片在 {@code centerStackPane} 中的偏移量，防止当前图片在缩放时超出
     * 可视区域，即 {@code centerStackPane} 的边界
     */
    private void clampViewOffset() {
        double offsetX = photoView.getTranslateX();
        double offsetY = photoView.getTranslateY();

        setViewOffset(offsetX, offsetY);
    }

    /**
     * Parse a percent string to a double value
     *
     * @param percentStr Percent string, e.g. "20%", "50%", "100%"
     * @return {@code double} value of the percent string if valid, else return -1.0
     */
    private double parsePercentLiteral(String percentStr) {
        if (percentStr == null || !percentStr.endsWith("%")) {
            return -1.0;
        }

        if (percentStr.equals("Fit")) {
            return -1.0;
        }

        try {
            double percent = Double.parseDouble(percentStr.substring(0, percentStr.length() - 1)) / 100.0;
            percent = percent > 0 ? percent : -1.0;

            return percent;
        } catch (NumberFormatException e) {
            String msg = String.format("Invalid percent string: %s", percentStr);
            showErrorLater(msg, Duration.seconds(10));

            if (DEBUG)
                Logger.logErr(msg, e);
            return -1.0;
        }
    }

    private final double[] mouseAnchor = {0, 0}; // 鼠标移动前的坐标
    private final double[] viewAnchor = {0, 0}; // 图片移动前的位移

    /**
     * 记录鼠标在{@code photoView}内部按下时的坐标和图片的位移
     */
    private void mousePressedOnView(MouseEvent event) {
        if (!viewProperty.isFittedProperty().get()) {
            mouseAnchor[0] = event.getSceneX();
            mouseAnchor[1] = event.getSceneY();

            viewAnchor[0] = photoView.getTranslateX();
            viewAnchor[1] = photoView.getTranslateY();
        }
    }

    private void mouseEnteredOnView(MouseEvent event) {
        if (!viewProperty.isFittedProperty().get()) {
            photoView.setCursor(Cursor.OPEN_HAND);
        }
    }

    private void mouseExitOnView() {
        photoView.setCursor(Cursor.DEFAULT);
    }

    /**
     * 鼠标双击时，设置图片的缩放模式为自适应缩放
     */
    private void mouseClickedOnView(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            viewProperty.isFittedProperty().set(true);
        }
    }

    /**
     * 鼠标释放时，恢复鼠标的光标
     */
    private void mouseReleasedOnView() {
        if (viewProperty.isFittedProperty().get()) {
            photoView.setCursor(Cursor.DEFAULT);
        } else {
            photoView.setCursor(Cursor.OPEN_HAND);
        }
    }

    /**
     * 鼠标在{@code photoView}内部拖动时，计算鼠标的位移然后更新图片的位移
     */
    private void mouseDraggedOnView(MouseEvent event) {
        if (!viewProperty.isFittedProperty().get()) {
            photoView.setCursor(Cursor.CLOSED_HAND);

            double deltaX = event.getSceneX() - mouseAnchor[0];
            double deltaY = event.getSceneY() - mouseAnchor[1];

            double offsetX = viewAnchor[0] + deltaX;
            double offsetY = viewAnchor[1] + deltaY;

            setViewOffset(offsetX, offsetY);
        }
    }

    /**
     * 设置图片在可视区域{@code centerStackPane}内的偏移量。
     * 计算图片与可视区域的尺寸差，将offsetX/offsetY限制在最大允许范围内，
     * 防止图片被拖出可视区域边界。
     * <p>
     * 计算方法：
     * <p>
     * 1. 获取{@code centerStackPane}的宽高{@code paneW / paneH}
     * 以及图片相对于{@code centerStackPane}缩放后的宽高{@code imgW / imgH}
     * <p>
     * 2. 计算图片在{@code centerStackPane}中的最大允许偏移量, 公式为
     * <pre>
     * {@code
     *      maxX = Math.abs(imgW - paneW) / 2
     *      maxY = Math.abs(imgH - paneH) / 2
     * }
     * </pre>
     * 因为对于{@code StackPane}来说，所有子节点的锚点都是在中心点
     * <p>
     * 3. 设置 {@code -maxX/-maxY <= offsetX/offsetY <= maxX/maxY}
     *
     * @param offsetX 图片在 {@code centerStackPane} 中的水平位移
     * @param offsetY 图片在 {@code centerStackPane} 中的垂直位移
     */
    private void setViewOffset(double offsetX, double offsetY) {
        double imgW = photoView.getBoundsInParent().getWidth();
        double imgH = photoView.getBoundsInParent().getHeight();
        double paneW = centerStackPane.getWidth();
        double paneH = centerStackPane.getHeight();

        double maxX = Math.abs(imgW - paneW) / 2;
        double maxY = Math.abs(imgH - paneH) / 2;

        if (offsetX > maxX) {
            offsetX = maxX;
        } else if (offsetX < -maxX) {
            offsetX = -maxX;
        }

        if (offsetY > maxY) {
            offsetY = maxY;
        } else if (offsetY < -maxY) {
            offsetY = -maxY;
        }

        photoView.setTranslateX(offsetX);
        photoView.setTranslateY(offsetY);
    }

    /**
     * 获取图片相对于{@code photoView}的缩放比例
     *
     * @param img 当前图片
     * @return {@code img}有效时返回缩放比例，否则返回{@code -1}
     */
    private double getPhotoViewScale(Image img) {
        if (img == null) {
            return -1;
        }

        double imgW = img.getWidth();
        double imgH = img.getHeight();
        if (imgW <= 0 || imgH <= 0) {
            return -1;
        }

        double fitW = photoView.getFitWidth();
        double fitH = photoView.getFitHeight();
        double scale = 1.0;

        // Calculate the fit scale
        if (fitW > 0 || fitH > 0) {
            double wRatio = fitW > 0 ? fitW / imgW : Double.POSITIVE_INFINITY;
            double hRatio = fitH > 0 ? fitH / imgH : Double.POSITIVE_INFINITY;
            scale = Math.min(wRatio, hRatio);
        }

        // Overlay scaleX
        scale *= photoView.getScaleX(); // scaleX should be same as scaleY when preserveRatio is true

        return scale;
    }

    private void showError(String msg, int delayMillis) {
        resetError();

        errMsg.setText(msg);

        photoStatus.getRightItems().addAll(errSeparator, errMsg);

        hideTimer.stop();
        hideTimer.setDelay(Duration.millis(delayMillis));
        hideTimer.setOnFinished(e -> photoStatus.getRightItems().removeAll(errSeparator, errMsg));
        hideTimer.playFromStart();
    }

    private void showError(String msg, Duration duration) {
        resetError();

        errMsg.setText(msg);

        photoStatus.getRightItems().addAll(errSeparator, errMsg);

        hideTimer.stop();
        hideTimer.setDelay(duration);
        hideTimer.setOnFinished(e -> photoStatus.getRightItems().removeAll(errSeparator, errMsg));
        hideTimer.playFromStart();
    }

    private void showErrorLater(String msg, int delayMillis) {
        Platform.runLater(() -> showError(msg, delayMillis));
    }

    private void showErrorLater(String msg, Duration duration) {
        Platform.runLater(() -> showError(msg, duration));
    }

    private void resetError() {
        hideTimer.stop();
        photoStatus.getRightItems().removeAll(errSeparator, errMsg);
    }

    private void resetErrorLater() {
        Platform.runLater(this::resetError);
    }

    public String getCurPhotoName() {
        return viewProperty.curPhotoProperty().get().getName();
    }

    /**
     * Set the stage's minimum size, who holds this controller,
     * to meet the Photoview.fxml's layout requirements. ( 540 x 200 )
     *
     * @param stage The stage to set the minimum size for
     */
    public void setStageMinSize(Stage stage) {
        stage.setMinWidth(540);
        stage.setMinHeight(200);
    }

    public int getMinWidth() {
        return 540;
    }

    public int getMinHeight() {
        return 200;
    }

    public ReadOnlyStringProperty curPhotoNameProperty() {
        return viewProperty.displayNameProperty();
    }

    private ChangeListener<String> errMsgListener = (o, oldV, newV) -> {
        if (newV != null) {
            showErrorLater(newV, Duration.seconds(3));
        }
    };

    private ChangeListener<Boolean> playListener = (o, oldV, newV) -> {
        if (newV) {
            startSlideShow();
        } else {
            stopSlideShow();
        }
    };

    private ChangeListener<String> timeStrListener = (o, oldV, newV) -> {
        if (viewProperty.isPlayingProperty().get()) {
            startSlideShow();
        }
    };

    private ChangeListener<Boolean> sizeComboShowingListener = (o, oldV, newV) -> {
        if (newV) {
            sizeCombo.getSelectionModel().clearSelection();
        }

        if (!newV && !sizeCombo.getSelectionModel().isEmpty()) {
            zoomScaleByCombo();
        }
    };

    private ChangeListener<Boolean> fittedListener = (o, oldV, newV) -> {
        if (newV) {
            setZoom(-1.0);
        }
    };

    private ChangeListener<Object> scaleListener = (o, oldV, newV) -> {
        Image curImg = photoView.getImage();

        double scale = getPhotoViewScale(curImg);
        viewProperty.curZoomProperty().set(scale);

        zoomSlider.setValue(scale);
    };

    @FXML
    private void initialize() {
        try {
            errMsg.setStyle("-fx-text-fill: red;");

            // 设置错误消息监听
            viewProperty.errMsgProperty().addListener(errMsgListener);

            // 设置图片切换与播放按键禁用属性绑定
            nextButton.disableProperty().bind(viewProperty.isScanDoneProperty().not());
            prevButton.disableProperty().bind(viewProperty.isScanDoneProperty().not());
            playButton.disableProperty().bind(viewProperty.isScanDoneProperty().not());

            // 设置播放按键图标可见性绑定
            playIcon.visibleProperty().bind(viewProperty.isPlayingProperty().not());
            pauseIcon.visibleProperty().bind(viewProperty.isPlayingProperty());

            nextButton.setOnMouseClicked(event -> {
                viewProperty.isPlayingProperty().set(false);
                toNextPhoto();
            });
            prevButton.setOnMouseClicked(event -> {
                viewProperty.isPlayingProperty().set(false);
                toPrevPhoto();
            });
            playButton.setOnMouseClicked(event -> playPhoto());

            // 设置播放状态监听
            viewProperty.isPlayingProperty().addListener(playListener);
            timeCombo.valueProperty().addListener(timeStrListener);

            photoStatus.getLeftItems().addAll(
                    photoSize,
                    new Separator(VERTICAL),
                    photoType,
                    new Separator(VERTICAL),
                    photoDimension,
                    new Separator(VERTICAL),
                    photoZoom,
                    new Separator(VERTICAL),
                    photoIdx,
                    new Separator(VERTICAL),
                    photoLastModified);

            photoName.textProperty().bind(viewProperty.displayNameProperty());
            photoSize.textProperty().bind(viewProperty.displaySizeProperty());
            photoType.textProperty().bind(viewProperty.displayTypeProperty());
            photoDimension.textProperty().bind(viewProperty.displayDimensionProperty());
            photoZoom.textProperty().bind(viewProperty.displayZoomProperty());
            photoIdx.textProperty().bind(viewProperty.displayIdxProperty());
            photoLastModified.textProperty().bind(viewProperty.displayLastModifiedProperty());

            // Prevent the centerStackPane from overflow to overlap the topHBox
            // The BorderPane won't clip the center region automatically
            centerStackPane.setMinSize(0, 0);

            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(centerStackPane.widthProperty());
            clip.heightProperty().bind(centerStackPane.heightProperty());
            centerStackPane.setClip(clip);

            rootPane.setMinSize(0, 0);

            loadMsg.visibleProperty().bind(viewProperty.isImgLoadingProperty());

            // TODO Flickers will happened when set Image to fast, need new display cache strategy
            photoView.imageProperty().bind(viewProperty.displayImgProperty());

            // 设置图片的缩放属性绑定与监听
            photoView.setOnMousePressed(this::mousePressedOnView);
            photoView.setOnMouseEntered(this::mouseEnteredOnView);
            photoView.setOnMouseExited(e -> mouseExitOnView());
            photoView.setOnMouseClicked(this::mouseClickedOnView);
            photoView.setOnMouseReleased(e -> mouseReleasedOnView());
            photoView.setOnMouseDragged(this::mouseDraggedOnView);

            sizeCombo.showingProperty().addListener(sizeComboShowingListener);
            viewProperty.isFittedProperty().addListener(fittedListener);

            zoomInButton.setOnMousePressed(event -> zoomInScale());
            zoomOutButton.setOnMousePressed(event -> zoomOutScale());

            zoomSlider.setOnMouseDragged(e -> zoomBySlider());
            zoomSlider.setOnMouseReleased(e -> zoomBySlider()); // 点击时竟然能改变值

            photoView.imageProperty().addListener(scaleListener);
            photoView.fitWidthProperty().addListener(scaleListener);
            photoView.fitHeightProperty().addListener(scaleListener);
            photoView.scaleXProperty().addListener(scaleListener);
            photoView.scaleYProperty().addListener(scaleListener);

            // set photoView to auto-fit the centerStackPane
            photoView.fitHeightProperty().bind(centerStackPane.heightProperty());
            photoView.fitWidthProperty().bind(centerStackPane.widthProperty());
            viewProperty.isFittedProperty().set(true);

            photoView.setPreserveRatio(true);
            photoView.setSmooth(true);
            photoView.setCache(true);

            sizeCombo.getItems().addAll("Fit", "20%", "50%", "70%", "100%", "150%", "200%", "300%", "500%");
            timeCombo.getItems().addAll("1s", "2s", "3s", "5s", "10s", "20s", "30s", "60s", "0.01s");

            // throw new IOException("Test exception");
        } catch (Exception e) {
            showError(e.getMessage(), 2000);
            if (DEBUG)
                Logger.logErr(null, e);
        }

        Platform.runLater(() -> {
            if (DEBUG) {
                Logger.log("ViewController initialized: " + this);
            }

            viewProperty
                    .initScanDir()
                    .thenRun(() -> {
                        int curIndex = loader.getPhotoIndex(viewProperty.curPhotoProperty().get());
                        loader.preLoadPhotosAsync(curIndex, 1);
                    })
                    .exceptionally(ex -> {
                        if (DEBUG)
                            Logger.logErr(null, ex);
                        return null;
                    });

            // viewProperty.loadImg(viewProperty.curPhotoProperty().get());
            // viewProperty.loadPhotoMeta(viewProperty.curPhotoProperty().get());

            // 注意：如果初始化时需要依赖控件的确切尺寸、布局或其它在界面显示后才确定的属性，
            // 或者其他一些要初始化但是非常耗时的操作，比如扫描文件，加载图片等操作, 应当在
            // initialize 方法末尾用 Platform.runLater 包裹相关代码。这样可以确保这些
            // 属性和数据已经被 JavaFX 布局引擎正确计算和赋值，以及加快界面显示的速度。
        });
    }

    public void dispose() {
        if (DEBUG) {
            Logger.logErr("Disposing ViewController: " + this);
        }

        // 1. 停止并清理幻灯片动画，移除播放状态监听
        if (slideShowTimeline != null) {
            stopSlideShow();

            if (DEBUG) {
                Logger.logErr("Slide show stopped on deposing the: " + this);
            }

            slideShowTimeline = null;
        }

        // 2. 解绑 photoView 的属性与事件监听
        if (photoView != null) {
            photoView.setOnMousePressed(null);
            photoView.setOnMouseEntered(null);
            photoView.setOnMouseExited(null);
            photoView.setOnMouseClicked(null);
            photoView.setOnMouseReleased(null);
            photoView.setOnMouseDragged(null);

            photoView.imageProperty().unbind();
            photoView.fitWidthProperty().unbind();
            photoView.fitHeightProperty().unbind();
            photoView.scaleXProperty().unbind();
            photoView.scaleYProperty().unbind();

            photoView.imageProperty().removeListener(scaleListener);
            photoView.fitWidthProperty().removeListener(scaleListener);
            photoView.fitHeightProperty().removeListener(scaleListener);
            photoView.scaleXProperty().removeListener(scaleListener);
            photoView.scaleYProperty().removeListener(scaleListener);

            photoView = null;
        }

        // 3. 解绑 sizeCombo、timeCombo 监听
        if (sizeCombo != null) {
            sizeCombo.showingProperty().removeListener(sizeComboShowingListener);
            sizeCombo.getSelectionModel().clearSelection();
            sizeCombo.getItems().clear();
            sizeCombo = null;
        }
        if (timeCombo != null) {
            timeCombo.valueProperty().removeListener(timeStrListener);
            timeCombo.getSelectionModel().clearSelection();
            timeCombo.getItems().clear();
            timeCombo = null;
        }

        // 4. 解绑按钮事件
        if (nextButton != null) {
            nextButton.setOnMouseClicked(null);
            nextButton = null;
        }
        if (prevButton != null) {
            prevButton.setOnMouseClicked(null);
            prevButton = null;
        }
        if (playButton != null) {
            playButton.setOnMouseClicked(null);
            playButton = null;
        }
        if (playIcon != null) {
            playIcon.visibleProperty().unbind();
            playIcon = null;
        }
        if (pauseIcon != null) {
            pauseIcon.visibleProperty().unbind();
            pauseIcon = null;
        }
        if (zoomInButton != null) {
            zoomInButton.setOnMousePressed(null);
            zoomInButton = null;
        }
        if (zoomOutButton != null) {
            zoomOutButton.setOnMousePressed(null);
            zoomOutButton = null;
        }
        if (zoomSlider != null) {
            zoomSlider.setOnMouseDragged(null);
            zoomSlider.setOnMouseReleased(null);
            zoomSlider = null;
        }

        // 5. 解绑 viewProperty 的属性监听
        if (viewProperty != null) {
            viewProperty.dispose();

            // 移除错误消息监听
            viewProperty.errMsgProperty().removeListener(errMsgListener);

            // 移除图片播放状态监听
            viewProperty.isPlayingProperty().removeListener(playListener);

            // 解绑图片适应窗口属性监听
            viewProperty.isFittedProperty().removeListener(fittedListener);

            viewProperty = null;
        }

        // 6. 解绑其它控件绑定
        photoName.textProperty().unbind();
        photoSize.textProperty().unbind();
        photoType.textProperty().unbind();
        photoDimension.textProperty().unbind();
        photoZoom.textProperty().unbind();
        photoIdx.textProperty().unbind();
        photoLastModified.textProperty().unbind();

        // 7. 清理错误提示
        hideTimer.stop();
        if (photoStatus != null) {
            photoStatus.getRightItems().removeAll(errSeparator, errMsg);
        }

        // 8. 断开其它引用（便于 GC）
        rootPane = null;
        topHBox = null;
        topFilling = null;
        photoStatus = null;

        centerStackPane.clipProperty().unbind();
        centerStackPane.setClip(null);
        centerStackPane = null;

        loadMsg.visibleProperty().unbind();
        loadMsg = null;

        errMsgListener = null;
        playListener = null;
        timeStrListener = null;
        sizeComboShowingListener = null;
        fittedListener = null;
        scaleListener = null;

        if (DEBUG) {
            Logger.logErr("ViewController disposed: " + this);
        }
    }
}
