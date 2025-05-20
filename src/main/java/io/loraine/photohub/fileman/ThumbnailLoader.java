package io.loraine.photohub.fileman;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.MalformedURLException;

public class ThumbnailLoader {

    public static void loadThumbnailAsync(String imagePath, ImageView imageView) {
        Task<Image> loadImageTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                // 在这里加载图像，这个操作将在后台线程中进行
                Image originalImage = new Image(imagePath);

                // 计算缩放比例
                double widthRatio = 80.0 / originalImage.getWidth();
                double heightRatio = 80.0 / originalImage.getHeight();
                double scale = Math.min(widthRatio, heightRatio);

                // 生成缩略图
                double w = (originalImage.getWidth() * scale);
                double h = (originalImage.getHeight() * scale);
                return new Image(imagePath, w, h, true, true);
            }
        };

        // 当任务成功完成后更新ImageView
        loadImageTask.setOnSucceeded(event -> {
            Image loadedImage = loadImageTask.getValue();
            imageView.setImage(loadedImage);
        });

        // 可选：处理加载失败的情况
        loadImageTask.setOnFailed(event -> {
            // 处理错误情况，例如显示默认图像或错误消息
            System.err.println("Error loading image: " + loadImageTask.getException());
        });

        // 启动任务
        Thread thread = new Thread(loadImageTask);
        thread.setDaemon(true); // 设置为守护线程，这样当应用程序关闭时线程也会被销毁
        thread.start();
    }
}