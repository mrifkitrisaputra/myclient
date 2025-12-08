package com.client.ui;

import java.util.function.Consumer;

import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;

public class LoginScene {
    private final Scene scene;

    public LoginScene(Consumer<String> onConnect) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #2c3e50;");

        Label title = new Label("CONNECT TO SERVER");
        title.setTextFill(javafx.scene.paint.Color.WHITE);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));

        TextField ipField = new TextField("127.0.0.1");
        ipField.setMaxWidth(300);
        ipField.setPromptText("Server IP Address");
        ipField.setStyle("-fx-font-size: 16px;");

        Button btnConnect = new Button("CONNECT");
        btnConnect.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        btnConnect.setPrefWidth(200);

        btnConnect.setOnAction(e -> {
            String ip = ipField.getText();
            if (!ip.isEmpty()) {
                btnConnect.setDisable(true); // Prevent double click
                btnConnect.setText("Connecting...");
                onConnect.accept(ip); // Panggil fungsi di App.java
            }
        });

        root.getChildren().addAll(title, ipField, btnConnect);
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        this.scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
    }

    public Scene getScene() {
        return scene;
    }
}