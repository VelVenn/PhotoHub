/**
 * Photohub ---- To View Some S3xy Photos
 * Copyright (C) 2025 Yui, Loraine
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

import io.loraine.photohub.photo.Photo;
import io.loraine.photohub.photo.Photos;
import io.loraine.photohub.photo.thumb.ThumbLoader;
import io.loraine.photohub.util.Logger;
import io.loraine.photohub.viewer.Viewers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class FileManagerController {
    @FXML
    private TreeView<File> fileTree;
    @FXML
    private TilePane fileTilePane;
    @FXML
    private Label pathStatisticLabel;
    @FXML
    private Label locationLabel;
    @FXML
    private Label statsLabel;
    @FXML
    private Pane selectionPane;
    @FXML
    private Rectangle selectionRect;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private TabPane rightTabPane; // 绑定到右侧的 TabPane
    @FXML
    private Tab settingsTab; // 绑定到设置页面的 Tab


    private double lastManualPosition = -1; // 记录用户手动调整的位置
    private double dragStartX, dragStartY;
    private final List<VBox> selectedItems = new ArrayList<>();
    private final List<VBox> allFileBoxes = new ArrayList<>();
    private double selectedSize = 0;

    private static final boolean DEBUG = false;

    @FXML
    private void pasteButtonHandler() {
        if (App.clipboard.isEmpty()) {
            return;
        }
        var curPath = locationLabel.getText();
        for (var src : App.clipboard) {
            var name = src.getName();
            var dst = new File(curPath + '/' + name);
            if (dst.exists()) {
                dst = new File(curPath + '/' + "[new]" + name);
            }
            try {
                Files.copy(Paths.get(src.getAbsolutePath()), Paths.get(dst.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.out.println("复制失败!");
            }
        }
        showFilesInTilePane(new File(locationLabel.getText()));
    }

    @FXML
    private void handleSelectAll() {
        for (var item : allFileBoxes) {
            var file = (File) (item.getUserData());
            if (file.isDirectory()) {
                continue;
            }
            selectItem(item);
        }
    }

    @FXML
    private void handleCancelSelection() {
        clearSelection();
    }

    @FXML
    public void refreshButtonHandler() {
        if (rightTabPane.getSelectionModel().getSelectedIndex() == 1) {
            rightTabPane.getSelectionModel().select(0);
        }
        TreeItem<File> selectedItem = fileTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            if (fileTree.getSelectionModel().getSelectedItems().contains(selectedItem)) {
                showFilesInTilePane(selectedItem.getValue());
            }
        }
    }

    @FXML
    public void openSettings() {
        if (rightTabPane.getSelectionModel().getSelectedIndex() == 0) {
            rightTabPane.getSelectionModel().select(1);
        } else {
            rightTabPane.getSelectionModel().select(0);
        }
    }

    @FXML
    public void initialize() {
        setupFileTree();
        setupSelectionPane();
        setupTilePane();
        setupSplitPaneBehavior();
    }

    private void setupSplitPaneBehavior() {
        // 初始左侧宽度（像素）
        double initialLeftWidth = 300;

        // 首次加载时设置初始位置
        Platform.runLater(() -> {
            if (mainSplitPane.getWidth() > 0) {
                lastManualPosition = initialLeftWidth / mainSplitPane.getWidth();
                mainSplitPane.setDividerPosition(0, lastManualPosition);
            }
        });

        // 窗口大小变化时的处理
        mainSplitPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (lastManualPosition >= 0 && newVal.doubleValue() > 0) {
                // 保持左侧绝对宽度不变
                double currentLeftWidth = lastManualPosition * oldVal.doubleValue();
                lastManualPosition = currentLeftWidth / newVal.doubleValue();
                mainSplitPane.setDividerPosition(0, lastManualPosition);
            }
        });

        // 记录用户手动调整的位置
        mainSplitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
            if (!mainSplitPane.getScene().getWindow().isShowing()) return;

            // 检查是否是用户拖动（而非我们的程序调整）
            if (Math.abs(newVal.doubleValue() - lastManualPosition) > 0.001) {
                lastManualPosition = newVal.doubleValue();
            }
        });
    }

    // 初始化文件树
    private void setupFileTree() {
        TreeItem<File> rootItem = new TreeItem<>(new File("/"));
        rootItem.setExpanded(true);
        fileTree.setRoot(rootItem);
        fileTree.setShowRoot(false);

        fileTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // 仅显示文件夹名称
                    setText(item.getName());
                }
            }
        });


        File[] roots = File.listRoots();

        for (File root : roots) {
            if (root.isDirectory()) rootItem.getChildren().add(createNode(root));
        }

        fileTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // 如果打开了设置页面就把它关了
            if (rightTabPane.getSelectionModel().getSelectedIndex() == 1) {
                rightTabPane.getSelectionModel().select(0);
            }
            TreeItem<File> selectedItem = fileTree.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                if (fileTree.getSelectionModel().getSelectedItems().contains(selectedItem)) {
                    showFilesInTilePane(selectedItem.getValue());
                }
            }
        });
    }

    // 初始化选择面板
    private void setupSelectionPane() {
        selectionRect.setManaged(false);
        selectionPane.setOnMousePressed(this::handleMousePressed);
        selectionPane.setOnMouseDragged(this::handleMouseDragged);
        selectionPane.setOnMouseReleased(this::handleMouseReleased);
    }

    // 初始化TilePane
    private void setupTilePane() {
        Platform.runLater(() -> {
            fileTilePane.setAlignment(Pos.TOP_LEFT);
        });

        fileTilePane.prefWidthProperty().bind(
                selectionPane.widthProperty().subtract(20) // 减去padding
        );

        selectionPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0 && !allFileBoxes.isEmpty()) {
                Platform.runLater(this::updateTilePaneColumns);
            }
        });
    }

    // 创建树节点（懒加载）
    private TreeItem<File> createNode(final File file) {
        return new TreeItem<File>(file) {
            private boolean isLeaf;
            private boolean isFirstTimeChildren = true;
            private boolean isFirstTimeLeaf = true;

            @Override
            public ObservableList<TreeItem<File>> getChildren() {
                if (isFirstTimeChildren) {
                    isFirstTimeChildren = false;
                    super.getChildren().setAll(buildChildren(this));
                }
                return super.getChildren();
            }

            @Override
            public boolean isLeaf() {
                if (isFirstTimeLeaf) {
                    isFirstTimeLeaf = false;
                    isLeaf = !file.isDirectory();
                }
                return isLeaf;
            }

            private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> item) {
                File f = item.getValue();
                if (f != null && f.isDirectory()) {
                    File[] files = f.listFiles();

                    if (files != null) {
                        Arrays.sort(files, Comparator.comparing(File::getName));
                        ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();
                        for (File childFile : files) {
                            if (childFile.isDirectory()) children.add(createNode(childFile));
                        }
                        return children;
                    }
                }
                return FXCollections.emptyObservableList();
            }
        };
    }

    // 显示文件到右侧面板
    private void showFilesInTilePane(File directory) {
        clearTilePane();
        File[] files = directory.listFiles();
        if (files == null) return;
//        Arrays.sort(files, Comparator.comparing(File::getName));
        Arrays.sort(files, (f1, f2) -> {
            // 优先按是否是文件夹排序：文件夹排在前面
            if (f1.isDirectory() && !f2.isDirectory()) {
                return -1; // f1 是文件夹，f2 不是文件夹，f1 排在前面
            } else if (!f1.isDirectory() && f2.isDirectory()) {
                return 1; // f1 不是文件夹，f2 是文件夹，f2 排在前面
            } else {
                // 如果都是文件或都是文件夹，则按文件名排序
                return f1.getName().compareTo(f2.getName());
            }
        });

        fileTilePane.setPrefColumns(-1); // 禁用初始列数设置

        int fileCount = 0;
        double fileSize = 0;
        for (File file : files) {
            if (!Photos.isValidPhoto(Paths.get(file.getAbsolutePath()))) {
                if (file.isDirectory() && App.showFolder) {
                    VBox fileBox = createFileItem(file);
                    fileTilePane.getChildren().add(fileBox);
                    allFileBoxes.add(fileBox);
                }
            } else {
                fileCount++;
                fileSize += file.length() / (1024.0 * 1024.0);
                VBox fileBox = createFileItem(file);
                fileTilePane.getChildren().add(fileBox);
                allFileBoxes.add(fileBox);
            }
        }

        locationLabel.setText(directory.getAbsolutePath());
        pathStatisticLabel.setText(String.format("共有 %d 张图片, 总大小 %.2f MB", fileCount, fileSize));

        Platform.runLater(() -> {
            updateTilePaneColumns();
            fileTilePane.requestLayout();
        });
    }

    // 创建文件显示项
    private VBox createFileItem(File file) {
        VBox fileBox = new VBox(5);
        fileBox.setAlignment(Pos.CENTER);
        fileBox.getStyleClass().add("file-item");
        fileBox.setMinSize(100, 120);
        fileBox.setPrefSize(100, 120);
        fileBox.setMaxSize(100, 120);

        ImageView icon = new ImageView();
        icon.setFitWidth(40);
        icon.setFitHeight(40);

        // 设置缩略图
        icon.setPreserveRatio(true);
        icon.setImage(loadIcon(file));

        Label fileNameLabel = new Label(file.getName());
        fileNameLabel.setMaxWidth(95);
        fileNameLabel.setWrapText(true);
        fileNameLabel.setAlignment(Pos.CENTER);
        fileNameLabel.getStyleClass().add("file-label");

        fileBox.getChildren().addAll(icon, fileNameLabel);
        fileBox.setUserData(file);

        fileBox.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                handleFileItemClick(fileBox, event);
            }

//            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
//                System.out.println("Double click on file: " + file.getAbsolutePath());
//            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteMenuItem = new MenuItem("删除");
        deleteMenuItem.setOnAction(event -> {
            System.out.println("删除文件: " + file.getAbsolutePath());
            // 显示确认弹窗
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText("您确定要删除所选文件吗？");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    file.delete();
                    showFilesInTilePane(new File(locationLabel.getText()));
                }
            });

        });
        MenuItem renameMenuItem = new MenuItem("重命名");
        renameMenuItem.setOnAction(event -> {
            System.out.println("重命名文件: " + file.getAbsolutePath());
            var parentPath = file.getParent();

            // 创建重命名弹窗
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("重命名");
            dialog.setHeaderText("请输入新的文件名, 不包含拓展名");
            dialog.setContentText("新文件名:");

            // 显示弹窗并获取用户输入
            Optional<String> result = dialog.showAndWait();

            // 如果用户输入了内容
            result.ifPresent(newFileName -> {
                if (newFileName.trim().isEmpty()) {
                    statsLabel.setText("无效的文件名");
                    return;
                }

                // 执行重命名逻辑
                if (selectedItems.size() == 1) {
                    var newPath = parentPath + "/" + newFileName;
                    var ext = App.getFileExtension(file.getName());
                    System.out.println(newPath + ext);
                    file.renameTo(new File(newPath + ext));
                } else {
                    int id = 1;
                    for (var item : selectedItems) {
                        File fileItem = (File) item.getUserData();
                        var newPath = parentPath + "/" + newFileName;
                        var ext = App.getFileExtension(fileItem.getName());
                        System.out.println(newPath + "." + id + ext);
                        file.renameTo(new File(newPath + "." + id + ext));
                        id++;
                    }
                }
                showFilesInTilePane(new File(locationLabel.getText()));
            });

        });
        MenuItem copyMenuItem = new MenuItem("复制");
        copyMenuItem.setOnAction(event -> {
            for (var item : selectedItems) {
                App.clipboard.add((File) (item.getUserData()));
            }
        });
        contextMenu.getItems().addAll(
                copyMenuItem,
                renameMenuItem,
                deleteMenuItem
        );

        if (!file.isDirectory()) {
            // 绑定右键菜单到文件项
            fileBox.setOnContextMenuRequested(event -> {
                if (!selectedItems.contains(fileBox)) {
                    clearSelection();
                    selectItem(fileBox);
                }
                contextMenu.show(fileBox, event.getScreenX(), event.getScreenY());
                event.consume(); // 阻止默认行为
            });
        }

        return fileBox;
    }

    // 显示缩略图
    private Image loadIcon(File file) {

        if (file.isDirectory()) {
            // 直接显示文件夹图标
            try {
                var iconURL = Objects.requireNonNull(getClass().getResource("/io/loraine/photohub/Default_Resources/folder.png"));
                return new Image(iconURL.toString(), true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {

            if (!App.showThumbnail) {
                // 直接显示占位图, 不加载缩略图
                try {
                    var iconURL = Objects.requireNonNull(getClass().getResource("/io/loraine/photohub/Default_Resources/file.png"));
                    return new Image(iconURL.toString());
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                }
            }

            if (App.betterThumbnail) {
                // 加载原始图片
                Image originalImage = null;
                try {
                    originalImage = new Image(file.toURI().toURL().toString());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }

                // 计算缩放比例
                double widthRatio = 100.0 / originalImage.getWidth();
                double heightRatio = 100.0 / originalImage.getHeight();
                double scale = Math.min(widthRatio, heightRatio);

                // 生成缩略图
                double w = (originalImage.getWidth() * scale);
                double h = (originalImage.getHeight() * scale);
//                System.out.println("w" + w);
//                System.out.println("h" + h);
                try {
                    return new Image(file.toURI().toURL().toString(), w, h, true, true);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }

            } else {
                // 使用 Image 直接加载原图, 如果加载失败再加载占位图
                try {
                    return new Image(file.toURI().toURL().toString(), true);
                } catch (Exception e) {
                    // 如果加载失败, 则显示默认图片图标
                    try {
                        var iconURL = Objects.requireNonNull(getClass().getResource("/io/loraine/photohub/Default_Resources/file.png"));
                        return new Image(iconURL.toString());
                    } catch (Exception e1) {
                        throw new RuntimeException(e1);
                    }
                }
            }
        }

    }

    // 更新TilePane列数
    private void updateTilePaneColumns() {
        double availableWidth = fileTilePane.getWidth() - 20; // 减去padding
        if (availableWidth > 0) {
            double itemWidth = 100 + 15; // 项目宽度 + hgap
            int columns = (int) (availableWidth / itemWidth);
            columns = Math.max(3, columns); // 最小保持3列
            fileTilePane.setPrefColumns(columns);
        }
    }

    // 清空TilePane
    private void clearTilePane() {
        fileTilePane.getChildren().clear();
        allFileBoxes.clear();
        clearSelection();
    }

    // 鼠标按下事件
    private void handleMousePressed(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            dragStartX = event.getX();
            dragStartY = event.getY();

            boolean clickedOnItem = allFileBoxes.stream()
                    .anyMatch(box -> box.getBoundsInParent().contains(event.getX(), event.getY()));

            if (!clickedOnItem && !event.isControlDown() && !event.isShiftDown()) {
                clearSelection();
            }

            initSelectionRect(dragStartX, dragStartY);
        }
    }

    // 鼠标拖动事件
    private void handleMouseDragged(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            updateSelectionRect(event.getX(), event.getY());
            updateSelection(selectionRect.getBoundsInParent());
        }
    }

    // 鼠标释放事件
    private void handleMouseReleased(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            selectionRect.setVisible(false);

            if (isClickEvent(event)) {
                handleSingleClickSelection(event);
            }
        }
    }

    // 初始化选择矩形
    private void initSelectionRect(double x, double y) {
        selectionRect.setX(x);
        selectionRect.setY(y);
        selectionRect.setWidth(0);
        selectionRect.setHeight(0);
        selectionRect.setVisible(true);
    }

    // 更新选择矩形
    private void updateSelectionRect(double x, double y) {
        selectionRect.setX(Math.min(dragStartX, x));
        selectionRect.setY(Math.min(dragStartY, y));
        selectionRect.setWidth(Math.abs(x - dragStartX));
        selectionRect.setHeight(Math.abs(y - dragStartY));
    }

    // 判断是否是点击事件
    private boolean isClickEvent(MouseEvent event) {
        return Math.abs(event.getX() - dragStartX) < 5 &&
                Math.abs(event.getY() - dragStartY) < 5;
    }

    // 处理单选点击
    private void handleSingleClickSelection(MouseEvent event) {
        allFileBoxes.stream()
                .filter(box -> box.getBoundsInParent().contains(event.getX(), event.getY()))
                .findFirst()
                .ifPresent(fileBox -> handleFileItemClick(fileBox, event));
    }

    // 处理文件项点击
    private void handleFileItemClick(VBox fileBox, MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            Path path = Paths.get(fileBox.getUserData().toString());

            if (Photos.isValidPhoto(path)) {
                try {
                    var viewStage = Viewers.createViewerStage(path);
                    viewStage.show();
                    if (DEBUG) Logger.logErr("Stage show: " + viewStage.isShowing() + " - " + path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
//                System.err.println("Not a photo: " + path);
                var file = (File) (fileBox.getUserData());
                if (file.isDirectory()) {
                    showFilesInTilePane(file);
                }
            }

            return;
        }

        var file = (File) (fileBox.getUserData());
        if (file.isDirectory()) {
            return;
        }

        if (event.isControlDown()) {
            toggleSelection(fileBox);
        } else if (event.isShiftDown() && !selectedItems.isEmpty()) {
            rangeSelect(fileBox);
        } else if (!selectedItems.contains(fileBox)) {
            clearSelection();
            selectItem(fileBox);
        }
    }

    // 范围选择
    private void rangeSelect(VBox endItem) {
        int startIndex = allFileBoxes.indexOf(selectedItems.get(selectedItems.size() - 1));
        int endIndex = allFileBoxes.indexOf(endItem);

        if (startIndex != -1 && endIndex != -1) {
            int min = Math.min(startIndex, endIndex);
            int max = Math.max(startIndex, endIndex);

            for (int i = min; i <= max; i++) {
                VBox item = allFileBoxes.get(i);
                if (!selectedItems.contains(item)) {
                    selectItem(item);
                }
            }
        }
    }

    // 选择项目
    private void selectItem(VBox fileBox) {
        var file = (File) (fileBox.getUserData());
        if (file.isDirectory()) {
            return;
        }
        if (!selectedItems.contains(fileBox)) {
            selectedItems.add(fileBox);
            selectedSize += ((File) (fileBox.getUserData())).length() / (1024.0 * 1024.0);
            fileBox.getStyleClass().add("selected");
        }
        statsLabel.setText(String.format("选中文件数: %d, 总大小: %.2f MB", selectedItems.size(), selectedSize));
    }

    // 取消选择
    private void deselectItem(VBox fileBox) {
        selectedItems.remove(fileBox);
        selectedSize -= ((File) (fileBox.getUserData())).length() / (1024.0 * 1024.0);
        fileBox.getStyleClass().remove("selected");
        statsLabel.setText(String.format("选中文件数: %d, 总大小: %.2f MB", selectedItems.size(), selectedSize));
    }

    // 切换选择状态
    private void toggleSelection(VBox fileBox) {
        if (selectedItems.contains(fileBox)) {
            deselectItem(fileBox);
        } else {
            selectItem(fileBox);
        }
    }

    // 清空选择
    private void clearSelection() {
        selectedSize = 0;
        selectedItems.forEach(item -> item.getStyleClass().remove("selected"));
        selectedItems.clear();
        statsLabel.setText("选中文件数: 0, 总大小: 0 KB");
    }

    private void updateSelection(Bounds selectionBounds) {
        // 临时记录当前在选区内的项目
        List<VBox> newlySelected = new ArrayList<>();

        for (VBox fileBox : allFileBoxes) {
            // 检查项目是否与选择矩形相交
            boolean isInSelection = fileBox.getBoundsInParent().intersects(
                    selectionBounds.getMinX(),
                    selectionBounds.getMinY(),
                    selectionBounds.getWidth(),
                    selectionBounds.getHeight());

            if (isInSelection) {
                newlySelected.add(fileBox);
                // 如果不在已选列表中则添加
                if (!selectedItems.contains(fileBox)) {
                    selectItem(fileBox);
                }
            }
        }

        // 找出需要取消选择的项目（之前选中但不在当前选区内的）
        List<VBox> toDeselect = new ArrayList<>(selectedItems);
        toDeselect.removeAll(newlySelected);

        // 取消选择这些项目
        for (VBox fileBox : toDeselect) {
            deselectItem(fileBox);
        }
    }
}