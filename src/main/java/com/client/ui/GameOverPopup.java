package com.client.ui;

import com.client.App;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GameOverPopup extends VBox {

    // [TAMBAHAN PARAMETER] isSpectator: True jika player mati tapi game belum berakhir
    public GameOverPopup(boolean isWin, boolean isSpectator) {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        // Default agak gelap
        this.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-padding: 40; -fx-background-radius: 20;");
        this.setMaxSize(500, 400);

        Label title = new Label(isWin ? "VICTORY!" : "YOU DIED");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        title.setTextFill(isWin ? Color.GOLD : Color.RED);

        // --- REMATCH / INFO SECTION ---
        String infoText = isSpectator 
            ? "Game is still running. You can spectate." 
            : "Vote to play again...";
            
        Label lblInfo = new Label(infoText);
        lblInfo.setTextFill(Color.WHITE);
        lblInfo.setFont(Font.font(18));

        Button btnRematch = new Button("Vote Rematch");
        styleButton(btnRematch, "#2980b9"); // Biru

        // --- SPECTATE BUTTON (Khusus jika mati duluan) ---
        Button btnSpectate = new Button("Spectate");
        styleButton(btnSpectate, "#8e44ad"); // Ungu
        // Tombol ini hanya menyembunyikan background popup agar bisa nonton
        btnSpectate.setOnAction(e -> {
            this.setStyle("-fx-background-color: rgba(0, 0, 0, 0.2);"); // Transparan
            title.setVisible(false);
            btnSpectate.setVisible(false);
            lblInfo.setText("Spectating... Waiting for Host to Rematch");
        });

        // Jika game sudah over total, tombol spectate tidak perlu
        if (!isSpectator) {
            btnSpectate.setVisible(false);
            btnSpectate.setManaged(false); // Hapus dari layout
        }

        // Logic Vote Rematch
        btnRematch.setOnAction(e -> {
            // 1. Disable tombol agar tidak spam
            btnRematch.setDisable(true);
            btnRematch.setText("Waiting for Host...");

            // 2. Ubah tampilan jadi mode spectre (transparan)
            this.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);"); // Transparan
            title.setVisible(false); 
            btnSpectate.setVisible(false); // Hide tombol spectate kalau sudah vote

            // 3. Kirim sinyal ke Server
            if (App.network != null) {
                App.network.send("VOTE_REMATCH");
            }
        });

        // --- LEAVE BUTTON ---
        Button btnLeave = new Button("Leave Room");
        styleButton(btnLeave, "#c0392b"); // Merah
        
        btnLeave.setOnAction(e -> {
            // 1. Kirim sinyal keluar
            if (App.network != null) {
                App.network.send("LEAVE_ROOM");
            }
            
            // 2. Kembali ke Lobby
            SceneManager.toLobby();
        });

        HBox buttonBox = new HBox(20, btnLeave, btnRematch);
        if (isSpectator) buttonBox.getChildren().add(btnSpectate); // Tambah tombol spectate di tengah
        buttonBox.setAlignment(Pos.CENTER);

        this.getChildren().addAll(title, lblInfo, buttonBox);
    }

    private void styleButton(Button btn, String color) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        btn.setPrefSize(160, 40);
    }
}