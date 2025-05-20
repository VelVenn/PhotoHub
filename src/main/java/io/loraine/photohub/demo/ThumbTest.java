/**
 * Photohub ---- To View Some S3xy Photos
 * Copyright (C) 2025 Loraine K. Cheung
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

import io.loraine.photohub.photo.Photo;
import io.loraine.photohub.photo.thumb.ThumbLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.nio.file.Path;

public class ThumbTest extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Path dir = Path.of("path/to/your/dir"); // change to your directory
        ThumbLoader loader = new ThumbLoader(200, 120, 20, 4, 90);
        loader.scanPath(dir);

        if (!loader.isIndexBasedUsable()) {
            throw new RuntimeException("No photos found in dir: " + dir);
        }

        Photo photo = loader.getPhotoByIndex(0); // choose the first photo or use get(n) for others
        ImageView view = new ImageView(loader.loadPhotoAsync(photo).get());
        view.setPreserveRatio(true);
        view.setFitWidth(200);
        view.setFitHeight(120);

        StackPane root = new StackPane(view);
        Scene scene = new Scene(root, 300, 200);
        stage.setTitle("ThumbLoader Scaling");
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(e -> loader.close());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
