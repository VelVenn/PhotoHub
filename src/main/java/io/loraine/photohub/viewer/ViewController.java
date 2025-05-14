package io.loraine.photohub.viewer;

import io.loraine.photohub.photo.Photo;
import io.loraine.photohub.photo.Photos;
import io.loraine.photohub.photo.PhotoLoader;

import javafx.animation.PauseTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ChangeListener;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

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

    /**
     * The default constructor is set to private to prevent undefined behaviors
     */
    private ViewController() {
        loader = new PhotoLoader(0, 0, true);
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

    @FXML
    private void toNextPhoto() {
        if (isIndexAndCurPhotoInvalid()) {
            return;
        }

        Photo curPhoto = viewProperty.curPhotoProperty().get();

        int curIdx = loader.getPhotoIndex(curPhoto);
        int amount = loader.getPhotoCount();

        int nextIdx = (curIdx + 1) % amount;

        Photo newPhoto = loader.getPhotoByIndex(nextIdx);
        viewProperty.curPhotoProperty().set(newPhoto);
    }

    @FXML
    private void toPrevPhoto() {
        if (isIndexAndCurPhotoInvalid()) {
            return;
        }

        Photo curPhoto = viewProperty.curPhotoProperty().get();

        int curIdx = loader.getPhotoIndex(curPhoto);
        int amount = loader.getPhotoCount();

        int prevIdx = (curIdx - 1 + amount) % amount;

        Photo newPhoto = loader.getPhotoByIndex(prevIdx);
        viewProperty.curPhotoProperty().set(newPhoto);
    }

    @FXML
    private void playPhoto() {
        if (isIndexAndCurPhotoInvalid()) {
            return;
        }

        boolean curPlayStat = !viewProperty.isPlayingProperty().get();
        viewProperty.isPlayingProperty().set(curPlayStat);
    }

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
    }

    private void stopSlideShow() {
        if (slideShowTimeline != null) {
            slideShowTimeline.stop();
        }
    }

    private Duration parseTimeToSeconds(String timeString) {
        if (timeString == null || !timeString.endsWith("s")) {
            return Duration.seconds(1.0);
        }

        try {
            double seconds = Double.parseDouble(timeString.substring(0, timeString.length() - 1));
            seconds = seconds > 0 ? seconds : 1.0;

            return Duration.seconds(seconds);
        } catch (NumberFormatException e) {
            String msg = String.format("Invalid time string: %s", timeString);
            showErrorLater(msg, Duration.seconds(10));

            if (DEBUG) System.err.println(msg + "Caused by: " + e.getCause().getMessage());
            return Duration.seconds(1.0);
        }
    }

    private boolean isIndexAndCurPhotoInvalid() {
        if (!loader.isIndexBasedUsable()) {
            String msg = "Indexing is failed or still in progress.";
            showError(msg, Duration.seconds(15));

            if (DEBUG) System.err.println(msg);
            return true;
        }

        Photo current = viewProperty.curPhotoProperty().get();
        if (current == null) {
            String msg = "Current photo is null.";
            showError(msg, Duration.seconds(15));

            if (DEBUG) System.err.println(msg);
            return true;
        }

        return false;
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

    @FXML
    public void initialize() {
        try {
            errMsg.setStyle("-fx-text-fill: red;");

            viewProperty.errMsgProperty().addListener((o, oldV, newV) -> {
                if (newV != null) {
                    showErrorLater(newV, Duration.seconds(5));
                }
            });

            nextButton.disableProperty().bind(viewProperty.isScanDoneProperty().not());
            prevButton.disableProperty().bind(viewProperty.isScanDoneProperty().not());
            playButton.disableProperty().bind(viewProperty.isScanDoneProperty().not());

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

            viewProperty.isPlayingProperty().addListener((o, oldV, newV) -> {
                if (newV != null) {
                    if (newV) {
                        startSlideShow();
                    } else {
                        stopSlideShow();
                    }
                } else {
                    String msg = "Playing status is accidentally set to null.";
                    showErrorLater(msg, Duration.seconds(10));
                    stopSlideShow();

                    viewProperty.isPlayingProperty().set(false);
                    if (DEBUG) System.err.println(msg);
                }
            });

            timeCombo.valueProperty().addListener((o, oldV, newV) -> {
                if (viewProperty.isPlayingProperty().get()) {
                    startSlideShow();
                }
            });

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
                    photoLastModified
            );

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
            rootPane.setMinSize(0, 0);

            loadMsg.visibleProperty().bind(viewProperty.isImgLoadingProperty());

            // TODO Flickers will happened when set Image to fast, need new display cache strategy
            photoView.imageProperty().bind(viewProperty.displayImgProperty());

            ChangeListener<Object> scaleListener = (o, oldV, newV) -> {
                double scale = getPhotoViewScale(photoView.getImage());
                viewProperty.curZoomProperty().set(scale);
            };
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
            timeCombo.getItems().addAll("1s", "2s", "3s", "5s", "10s", "20s", "30s", "60s");

            // Loading methods below
            viewProperty
                    .initScanDir()
                    .exceptionally(ex -> {
                        if (DEBUG) System.err.println(ex.getMessage());
                        return null;
                    });

            if (DEBUG) {
                System.out.println(photoView.getFitHeight());
                System.out.println(centerStackPane.getHeight());
            }

            // throw new IOException("Test exception");
        } catch (Exception e) {
            showError(e.getMessage(), 2000);
            if (DEBUG) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage() + " Cause: " + e.getCause());
            }
        }
    }
}
