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

package io.loraine.photohub.photo.thumb;

import io.loraine.photohub.photo.Photo;
import io.loraine.photohub.photo.PhotoLoader;

import javafx.scene.image.Image;
import javafx.embed.swing.SwingFXUtils;

import java.awt.image.BufferedImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

import java.io.IOException;

public class ThumbLoader extends PhotoLoader {
    private volatile int width;
    private volatile int height;
    private final Object sizeLock = new Object();


    public ThumbLoader() throws IOException {
        super(120, 4, 120);
        width = 100;
        height = 100;
    }

    /**
     * 创建一个缩略图加载器
     * <p>
     * {@code ThumbLoader} 是一个线程安全的类, 创建者不再需要该类的实例时候,
     * 应当调用 {@link #close()} 来释放资源。
     *
     * @param width          缩略图宽度
     * @param height         缩略图高度
     * @param maxThumbCount  缓存的最大缩略图数量
     * @param maxThreadCount 最大加载线程数
     * @param expireTime     缓存过期时间（秒）
     */
    public ThumbLoader(int width, int height,
                       int maxThumbCount, int maxThreadCount, int expireTime) {
        super(maxThumbCount, maxThreadCount, expireTime);
        this.width = width;
        this.height = height;
    }

    @Override
    protected Image render(Photo photo) throws IOException {
        if (photo == null) {
            throw new NullPointerException("Photo cannot be null");
        }

        int w, h;
        synchronized (sizeLock) {
            w = width;
            h = height;
        }

        try (var in = javax.imageio.ImageIO.createImageInputStream(photo.getPath().toFile())) {
            var readers = javax.imageio.ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IOException("Failed to decode: " + photo.getPath());
            }

            var reader = readers.next();
            try {
                reader.setInput(in);

                int srcW = reader.getWidth(0);
                int srcH = reader.getHeight(0);

                double ratio = Math.min((double) w / srcW, (double) h / srcH);
                int thumbW = Math.max(1, (int) (srcW * ratio));
                int thumbH = Math.max(1, (int) (srcH * ratio));

                // subsampling
                int xSub = Math.max(1, srcW / thumbW);
                int ySub = Math.max(1, srcH / thumbH);

                javax.imageio.ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceSubsampling(xSub, ySub, 0, 0);

                BufferedImage sampled = reader.read(0, param);

                // 再用 Java2D 缩放到目标尺寸（如果还不够小）
                BufferedImage thumb = new BufferedImage(w, h, TYPE_INT_ARGB);
                java.awt.Graphics2D g2d = thumb.createGraphics();
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                        java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

                int x = (w - thumbW) / 2;
                int y = (h - thumbH) / 2;
                g2d.drawImage(
                        sampled, x, y,
                        x + thumbW, y + thumbH,
                        0, 0,
                        sampled.getWidth(), sampled.getHeight(),
                        null
                );
                g2d.dispose();

                Image result = SwingFXUtils.toFXImage(thumb, null);

                if (result.isError()) {
                    throw new IOException("Failed to load thumbnail: " + photo.getPath());
                }


                return result;
            } catch (IOException e) {
                throw new IOException("Failed to load thumbnail: " + photo.getPath(), e);
            } finally {
                reader.dispose();
            }
        }
    }

    public void setWidth(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be positive");
        }

        if (this.width != width) {
            synchronized (sizeLock) {
                if (this.width != width) {
                    this.width = width;
                }
            }
        }
    }

    public void setHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be positive");
        }

        if (this.height != height) {
            synchronized (sizeLock) {
                if (this.height != height) {
                    this.height = height;
                }
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
