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

package io.loraine.photohub.fileman;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import org.controlsfx.control.ToggleSwitch;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class SettingsController {

    public void viewSourceCode(ActionEvent event) {
        System.out.println("https://github.com/VelVenn/PhotoHub");
        var url = "https://github.com/VelVenn/PhotoHub";
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                // 将字符串转换为 URI 对象
                URI uri = new URI(url);
                // 调用系统浏览器打开链接
                desktop.browse(uri);
            } catch (URISyntaxException | IOException e) {
//                e.printStackTrace();
                System.err.println("无法打开链接: " + url);
            }
        } else {
            System.err.println("当前环境不支持 Desktop 类");
        }

    }

    @FXML
    private ToggleSwitch FolderToggle;

    @FXML
    private ToggleSwitch thumbnailToggle;

    @FXML
    public void initialize() {
        // 监听开关 1 的状态变化
        FolderToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            App.showFolder = newValue;
        });

        // 监听开关 2 的状态变化
        thumbnailToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            App.showThumbnail = newValue;
        });
    }


}