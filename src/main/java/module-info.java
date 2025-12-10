module com.client {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.media;

    // Mengizinkan semua subpackages com.client.* diakses publik
    exports com.client;
    exports com.client.core;
    exports com.client.entities;
    exports com.client.render;
    exports com.client.ui;
    exports com.client.network;

    // Mengizinkan JavaFX (FXML) & Gson menggunakan reflection
    opens com.client to javafx.fxml, javafx.media;
    opens com.client.core to javafx.fxml, javafx.media;
    opens com.client.entities to javafx.fxml, javafx.media;
    opens com.client.render to javafx.fxml, javafx.media;
    opens com.client.ui to javafx.fxml, javafx.media;
    opens com.client.network to javafx.fxml, javafx.media;
}
