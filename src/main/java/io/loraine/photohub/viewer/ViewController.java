package io.loraine.photohub.viewer;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.StatusBar;

import java.io.IOException;
import java.nio.file.Path;

import static javafx.geometry.Orientation.VERTICAL;

public class ViewController {
    @FXML
    private StatusBar photoStatus;

    @FXML
    private StackPane centerStackPane;

    @FXML
    private ImageView photoView;

    @FXML
    private Region topFilling;

    @FXML
    private BorderPane rootPane;

    private String curPhotoName = "Unselected";

    public ViewController(Path path) throws IOException {

    }

    public ViewController() {

    }

    public void initialize() {
        Label photoSize = new Label("Size: N/A");
        Label photoName = new Label("Name: N/A");
        Label photoDate = new Label("Date: N/A");

        photoStatus.getLeftItems().addAll(
                photoSize, new Separator(VERTICAL), photoName, new Separator(VERTICAL), photoDate);

        photoView.fitHeightProperty().bind(centerStackPane.heightProperty());
//        topFilling.setMinHeight(0.0);

        rootPane.autosize();

        System.out.println(photoView.getFitHeight());
        System.out.println(centerStackPane.getHeight());
    }

    public void setCurPhotoName(String photoName) {
        curPhotoName = photoName;
    }

    public String getCurPhotoName() {
        return curPhotoName;
    }
}
