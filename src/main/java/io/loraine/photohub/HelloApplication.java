package io.loraine.photohub;

import io.loraine.photohub.photo.PhotoLoader;
import io.loraine.photohub.viewer.ViewController;
import io.loraine.photohub.viewer.Viewers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        PhotoLoader photoLoader = new PhotoLoader(30, 20);

        Path path = Objects.requireNonNull(Paths.get("your/test/photo.jpg"));

        var packaged = Viewers.createViewerScene(path, photoLoader);

        var controller = packaged.getKey();
        var scene = packaged.getValue();

        stage.titleProperty().bind(controller.curPhotoNameProperty());
        stage.setScene(scene);

        controller.setStageMinSize(stage);
        stage.show();

        stage.setOnCloseRequest(event -> photoLoader.close());
    }

    public static void main(String[] args) {
        launch();
    }
}