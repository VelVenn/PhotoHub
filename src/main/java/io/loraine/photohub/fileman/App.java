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
