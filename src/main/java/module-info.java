module io.loraine.photohub {
    requires javafx.controls;
    requires javafx.swing;
    requires javafx.fxml;
    requires javafx.web;

    requires transitive javafx.graphics;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;
    requires org.kordamp.ikonli.material2;

    requires com.github.benmanes.caffeine;

    requires com.jfoenix;

    requires java.desktop;

    opens io.loraine.photohub.demo to javafx.fxml;
    opens io.loraine.photohub.main to javafx.fxml;
    opens io.loraine.photohub.viewer to javafx.fxml;
    opens io.loraine.photohub.fileman to javafx.fxml;

    exports io.loraine.photohub.demo;
    exports io.loraine.photohub.main;
    exports io.loraine.photohub.photo;
    exports io.loraine.photohub.viewer;
    exports io.loraine.photohub.fileman;
}