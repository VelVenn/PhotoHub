package io.loraine.photohub.viewer;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.util.Pair;

import io.loraine.photohub.photo.PhotoLoader;


import java.io.IOException;
import java.nio.file.Path;

public class Viewers {
    private Viewers() {}

    public static Pair<ViewController, Parent> createViewerPane(Path photoPath, PhotoLoader loader) throws IOException {
        FXMLLoader fLoader =
                new FXMLLoader(Viewers.class.getResource("/io/loraine/photohub/FXML/PhotoView.fxml"));

        ViewController vc = new ViewController(photoPath, loader);
        fLoader.setController(vc);

        Parent root = fLoader.load();
        return new Pair<>(vc, root);
    }

    public static Pair<ViewController, Scene> createViewerScene(Path photoPath, PhotoLoader loader) throws IOException {
        FXMLLoader fLoader =
                new FXMLLoader(Viewers.class.getResource("/io/loraine/photohub/FXML/PhotoView.fxml"));

        ViewController vc = new ViewController(photoPath, loader);
        fLoader.setController(vc);

        Parent root = fLoader.load();
        Scene scene = new Scene(root);

        return new Pair<>(vc, scene);
    }

    public static Pair<ViewController, Stage> createViewerStage(Path photoPath, PhotoLoader loader) throws IOException {
        FXMLLoader fLoader =
                new FXMLLoader(Viewers.class.getResource("/io/loraine/photohub/FXML/PhotoView.fxml"));

        ViewController vc = new ViewController(photoPath, loader);
        fLoader.setController(vc);

        Parent root = fLoader.load();
        Scene scene = new Scene(root);

        Stage stage = new Stage();
        stage.setScene(scene);

        vc.setStageMinSize(stage);

        return new Pair<>(vc, stage);
    }

}
