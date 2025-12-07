package com.client.ui;

import com.client.App;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;

public class RoomScene {
    private final Scene scene;
    private final ListView<String> playerList;
    private final Button btnStart;

    public RoomScene(String roomName) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #2c3e50;");

        // HEADER
        VBox header = new VBox(5);
        Label lblRoomName = new Label("ROOM: " + roomName);
        lblRoomName.setFont(new Font("Arial", 28));
        lblRoomName.setTextFill(Color.WHITE);
        header.getChildren().addAll(lblRoomName);
        header.setAlignment(Pos.CENTER);
        root.setTop(header);

        // PLAYER LIST
        playerList = new ListView<>();
        playerList.setStyle("-fx-control-inner-background: #34495e; -fx-text-fill: white; -fx-font-size: 16px;");
        root.setCenter(playerList);

        // SYNC LOGIC
        if (App.gameState != null) {
            App.gameState.setOnRoomStateUpdate(v -> updateUI());
            // Initial load
            updateUI();
        }

        // BUTTONS
        btnStart = new Button("START GAME");
        Button btnLeave = new Button("LEAVE ROOM");
        
        btnStart.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px;");
        btnLeave.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 16px;");

        // Logic Tombol Start (Default disabled, dinyalakan di updateUI)
        btnStart.setDisable(true);

        btnStart.setOnAction(e -> {
            if(App.network != null) App.network.send("START_GAME");
        });

        btnLeave.setOnAction(e -> {
            if(App.network != null) App.network.send("LEAVE_ROOM");
            SceneManager.toLobby();
        });

        HBox bottomBox = new HBox(20, btnLeave, btnStart);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(20));
        root.setBottom(bottomBox);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        this.scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
    }

    private void updateUI() {
        Platform.runLater(() -> {
            playerList.getItems().clear();
            
            var ids = App.gameState.getRoomPlayerIds();
            int myId = App.gameState.getMyPlayerId();
            int hostId = App.gameState.getHostPlayerId();
            
            for (int id : ids) {
                StringBuilder sb = new StringBuilder("Player " + id);
                if (id == myId) sb.append(" (You)");
                if (id == hostId) sb.append(" [HOST]");
                playerList.getItems().add(sb.toString());
            }

            // Logic Button Start: Hanya Host DAN Player >= 2
            boolean isHost = App.gameState.amIHost();
            boolean enoughPlayers = ids.size() >= 1;
            
            if (isHost) {
                btnStart.setDisable(!enoughPlayers);
                btnStart.setText(enoughPlayers ? "START GAME" : "WAITING FOR PLAYERS...");
            } else {
                btnStart.setDisable(true);
                btnStart.setText("WAITING FOR HOST...");
            }
        });
    }

    public Scene getScene() { return scene; }
}