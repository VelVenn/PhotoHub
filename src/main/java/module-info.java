module io.loraine.photohub {
    requires javafx.controls;
    requires javafx.swing;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;
    requires org.kordamp.ikonli.material2;

    requires com.github.benmanes.caffeine;

    requires com.jfoenix;

    requires java.desktop;

    requires eu.hansolo.tilesfx;

    opens io.loraine.photohub to javafx.fxml;
    opens io.loraine.photohub.viewer to javafx.fxml;

    exports io.loraine.photohub;
    exports io.loraine.photohub.viewer;
}