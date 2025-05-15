package io.loraine.photohub.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // 加载 FXML
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/io/loraine/photohub/FXML/FileManager.fxml"));

        Parent root = loader.load();

        // 设置场景和窗口标题
        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setTitle("Fileman");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
