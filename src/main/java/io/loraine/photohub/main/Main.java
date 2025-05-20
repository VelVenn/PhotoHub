/**
 * Photohub ---- To View Some S3xy Photos
 * Copyright (C) 2025 Yui
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

package io.loraine.photohub.main;

import io.loraine.photohub.fileman.FileManagerController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // 加载 FXML
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/io/loraine/photohub/FXML/FileManager.fxml"));

        Parent root = loader.load();

        // 设置场景和窗口标题
        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setTitle("PhotoHub");
        primaryStage.setScene(scene);
        primaryStage.show();

        FileManagerController controller = loader.getController();
        primaryStage.setOnCloseRequest(event -> {
            controller.dispose();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
