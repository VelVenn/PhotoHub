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

package io.loraine.photohub.demo;

import io.loraine.photohub.photo.PhotoLoader;
import io.loraine.photohub.viewer.Viewers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class ViewerDemoApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        PhotoLoader photoLoader = new PhotoLoader(
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE); // Set all cache strategy failed for demo

        Path path = Objects.requireNonNull(Paths.get("path/to/your/photo.jpg"));

        var packaged = Viewers.createViewerScene(path, photoLoader);

        var controller = packaged.getKey();
        var scene = packaged.getValue();

        stage.titleProperty().bind(controller.curPhotoNameProperty());
        stage.setScene(scene);

        controller.setStageMinSize(stage);

        System.out.println("Start show: " + stage.isShowing() + " " + System.currentTimeMillis());
        stage.show();
        System.out.println("Stage show: " + stage.isShowing() + " " + System.currentTimeMillis());

        stage.setOnCloseRequest(event -> photoLoader.close());
    }

    public static void main(String[] args) {
        System.out.println("Start main: " + System.currentTimeMillis());
        launch();
    }
}