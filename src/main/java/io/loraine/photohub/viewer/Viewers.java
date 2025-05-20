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

import io.loraine.photohub.util.Logger;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.util.Pair;

import io.loraine.photohub.photo.PhotoLoader;
import io.loraine.photohub.photo.LoaderManager;

import java.io.IOException;
import java.nio.file.Path;

public class Viewers {
    private Viewers() {
    }

    private static final boolean DEBUG = false;

    /**
     * 创建一个完整可用的{@code Photoview Pane}和与其绑定的控制器
     *
     * @param photoPath 初始照片路径
     * @param loader    照片加载器
     *
     * @return {@code javafx.util.Pair<ViewController, Parent>}
     *         包含控制器和{@code Parent}的对象
     *
     * @throws IOException 如果加载FXML文件或者构建控制器失败时抛出
     */
    public static Pair<ViewController, Parent> createViewerPane(Path photoPath, PhotoLoader loader) throws IOException {
        FXMLLoader fLoader = new FXMLLoader(Viewers.class.getResource("/io/loraine/photohub/FXML/PhotoView.fxml"));

        ViewController vc = new ViewController(photoPath, loader);
        fLoader.setController(vc);

        Parent root = fLoader.load();
        return new Pair<>(vc, root);
    }

    /**
     * 创建一个不绑定控制器的{@code Photoview Pane}
     *
     * @return {@code Parent} 无控制器的{@code Photoview Pane}
     *
     * @throws IOException 如果加载FXML文件失败时抛出
     */
    public static Parent createSkeletonViewerPane() throws IOException {
        FXMLLoader fLoader = new FXMLLoader(Viewers.class.getResource("/io/loraine/photohub/FXML/PhotoView.fxml"));

        return fLoader.load();
    }

    /**
     * 创建一个完整可用的{@code Photoview Scene}和与其根节点{@code Photoview Pane}绑定的控制器
     *
     * @param photoPath 初始照片路径
     * @param loader    照片加载器
     *
     * @return {@code javafx.util.Pair<ViewController, Scene>}
     *         包含控制器和{@code Scene}的对象
     *
     * @throws IOException 如果加载FXML文件或者构建控制器失败时抛出
     */
    public static Pair<ViewController, Scene> createViewerScene(Path photoPath, PhotoLoader loader) throws IOException {
        FXMLLoader fLoader = new FXMLLoader(Viewers.class.getResource("/io/loraine/photohub/FXML/PhotoView.fxml"));

        ViewController vc = new ViewController(photoPath, loader);
        fLoader.setController(vc);

        Parent root = fLoader.load();
        Scene scene = new Scene(root);

        return new Pair<>(vc, scene);
    }

    /**
     * 创建一个{@code Photoview Scene}，其根节点为{@code Photoview Pane}，但不绑定控制器
     *
     * @return {@code Scene} 无控制器的{@code Photoview Scene}
     *
     * @throws IOException 如果加载FXML文件失败时抛出
     */
    public static Scene createSkeletonViewerScene() throws IOException {
        FXMLLoader fLoader = new FXMLLoader(Viewers.class.getResource("/io/loraine/photohub/FXML/PhotoView.fxml"));

        Parent root = fLoader.load();
        return new Scene(root);
    }

    /**
     * 创建一个完整可用的{@code Photoview Stage}和与其根布局{@code Photoview Pane}绑定的控制器
     *
     * @param photoPath 初始照片路径
     * @param loader    照片加载器
     *
     * @return {@code javafx.util.Pair<ViewController, Stage>}
     *         包含控制器和{@code Stage}的对象
     *
     * @throws IOException 如果加载FXML文件或者构建控制器失败时抛出
     */
    public static Pair<ViewController, Stage> createViewerStage(Path photoPath, PhotoLoader loader) throws IOException {
        FXMLLoader fLoader = new FXMLLoader(Viewers.class.getResource("/io/loraine/photohub/FXML/PhotoView.fxml"));

        ViewController vc = new ViewController(photoPath, loader);
        fLoader.setController(vc);

        Parent root = fLoader.load();
        Scene scene = new Scene(root);

        Stage stage = new Stage();
        stage.setScene(scene);

        vc.setStageMinSize(stage);
        stage.titleProperty().bind(vc.curPhotoNameProperty());

        return new Pair<>(vc, stage);
    }

    public static Stage createViewerStage(Path photoPath) throws IOException {
        FXMLLoader fLoader = new FXMLLoader(Viewers.class.getResource("/io/loraine/photohub/FXML/PhotoView.fxml"));

        Path parentPath = photoPath.getParent();

        PhotoLoader loader = LoaderManager.getInstance().acquire(parentPath);
        ViewController vc = new ViewController(photoPath, loader);
        fLoader.setController(vc);

        Parent root = fLoader.load();
        Scene scene = new Scene(root);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            if (DEBUG)
                Logger.logErr("View Stage closed which is open by: " + photoPath);
            LoaderManager.getInstance().release(parentPath);
            stage.titleProperty().unbind();
            vc.close();
        });

        stage.titleProperty().bind(vc.curPhotoNameProperty());
        vc.setStageMinSize(stage);
        return stage;
    }
}
