package io.loraine.photohub;

import io.loraine.photohub.viewer.ViewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("FXML/Photoview.fxml"));

        ViewController vc = new ViewController();
        fxmlLoader.setController(vc);

        Scene scene = new Scene(fxmlLoader.load());

        stage.setTitle(vc.getCurPhotoName());
        stage.setScene(scene);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}