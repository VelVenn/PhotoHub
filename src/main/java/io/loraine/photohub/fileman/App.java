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

package io.loraine.photohub.fileman;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class App {
    static List<File> clipboard = new ArrayList<>();

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            // 没有扩展名
            return "";
        }

        // 返回扩展名（不包括点）
        return fileName.substring(lastIndex);
    }

}
