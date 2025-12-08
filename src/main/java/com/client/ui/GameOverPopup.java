package com.client.ui;

import com.client.App;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GameOverPopup extends VBox {

    private final Label lblVotes;
    private final Button btnRematch;
    private final Button btnLeave;
    private final Button btnStartGame; // Tombol Khusus Host

    public GameOverPopup(boolean isWin) {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-padding: 40; -fx-background-radius: 20; -fx-border-color: white; -fx-border-width: 1; -fx-border-radius: 20;");
        this.setMaxSize(500, 450); // Tinggi ditambah sedikit

        // --- TITLE ---
        Label title = new Label(isWin ? "VICTORY!" : "YOU DIED");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        title.setTextFill(isWin ? Color.GOLD : Color.RED);
        title.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);");

        // --- VOTES LABEL ---
        // Default text, nanti diupdate server
        lblVotes = new Label("Rematch Votes: 0"); 
        lblVotes.setTextFill(Color.WHITE);
        lblVotes.setFont(Font.font(20));

        // --- TOMBOL ACTIONS ---
        btnRematch = new Button("Vote Rematch");
        styleButton(btnRematch, "#2980b9"); // Biru

        btnLeave = new Button("Leave Room");
        styleButton(btnLeave, "#c0392b"); // Merah

        // --- TOMBOL HOST START ---
        btnStartGame = new Button("START GAME");
        styleButton(btnStartGame, "#27ae60"); // Hijau
        
        // LOGIC: Cek apakah saya Host?
        // Kita asumsikan ada method App.gameState.amIHost() atau membandingkan ID
        boolean isHost = (App.gameState != null && App.gameState.amIHost());
        
        if (isHost) {
            btnStartGame.setVisible(true); // Host lihat tombol ini
        } else {
            btnStartGame.setVisible(false); // Player biasa tidak lihat
            btnStartGame.setManaged(false); // Agar tidak makan tempat layout
        }

        // --- EVENT HANDLERS ---

        // 1. Vote Rematch
        btnRematch.setOnAction(e -> {
            btnRematch.setDisable(true);
            btnRematch.setText("Voted!");
            if (App.network != null) App.network.send("VOTE_REMATCH");
        });

        // 2. Leave
        btnLeave.setOnAction(e -> {
            if (App.network != null) App.network.send("LEAVE_ROOM");
            SceneManager.toLobby();
        });

        // 3. Host Start Game
        btnStartGame.setOnAction(e -> {
            // Host memaksa game mulai ulang (biasanya menunggu vote cukup dulu, tapi terserah host)
            if (App.network != null) App.network.send("START_GAME");
        });

        HBox voteBox = new HBox(20, btnLeave, btnRematch);
        voteBox.setAlignment(Pos.CENTER);

        // Urutan: Judul -> Jumlah Vote -> Tombol Vote/Leave -> Tombol Start (Host Only)
        this.getChildren().addAll(title, lblVotes, voteBox, btnStartGame);
    }

    // Method untuk diupdate oleh PacketParser/SceneManager
public void updateVoteText(int current, int total) {
        // Wajib pakai Platform.runLater karena dipanggil dari thread network
        Platform.runLater(() -> {
            // Ganti teks labelnya!
            lblVotes.setText("Rematch Votes: " + current + " / " + total);
            
            // (Opsional) Efek visual ke tombol host jika vote penuh
            if (current == total && btnStartGame != null && btnStartGame.isVisible()) {
                btnStartGame.setText("START NOW! (" + current + "/" + total + ")");
            }
        });
    }

    private void styleButton(Button btn, String color) {
        String baseStyle = "-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;";
        btn.setStyle(baseStyle);
        btn.setPrefSize(180, 45);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: derive(" + color + ", 20%); -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
    }
}