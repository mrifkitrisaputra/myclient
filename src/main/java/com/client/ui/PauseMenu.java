package com.client.ui;

import com.client.App;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class PauseMenu extends VBox {

    public PauseMenu(Runnable onResume, Runnable onLeave) {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85); -fx-padding: 30; -fx-background-radius: 15;");
        this.setMaxSize(300, 300);

        Label title = new Label("PAUSED");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("Arial", 32));

        Button btnResume = new Button("Resume Game");
        Button btnLeave = new Button("Leave Room");

        styleButton(btnResume, "#27ae60");
        styleButton(btnLeave, "#c0392b");

        // Aksi Resume
        btnResume.setOnAction(e -> onResume.run());
        
        // Aksi Leave
        btnLeave.setOnAction(e -> {
            // 1. Kirim sinyal ke Server kalau kita keluar
            if (App.network != null) {
                App.network.send("LEAVE_ROOM");
            }
            
            // 2. Jalankan logic pindah scene (biasanya kembali ke Lobby)
            onLeave.run();
        });

        this.getChildren().addAll(title, btnResume, btnLeave);
    }

    private void styleButton(Button btn, String color) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 16px;");
        btn.setPrefWidth(200);
    }
}