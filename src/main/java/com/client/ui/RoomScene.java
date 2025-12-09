package com.client.ui;

import com.client.App;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.util.Callback;

public class RoomScene {
    private final Scene scene;
    
    // [UPDATE] Ubah tipe ListView jadi Integer (ID) agar mudah di-manage
    private final ListView<Integer> playerList; 
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

        // PLAYER LIST (Custom Cell Factory untuk tombol Kick)
        playerList = new ListView<>();
        playerList.setStyle("-fx-control-inner-background: #34495e; -fx-background-color: #34495e;");
        
        // Logic render setiap baris list
        playerList.setCellFactory(new Callback<ListView<Integer>, ListCell<Integer>>() {
            @Override
            public ListCell<Integer> call(ListView<Integer> param) {
                return new ListCell<Integer>() {
                    @Override
                    protected void updateItem(Integer playerId, boolean empty) {
                        super.updateItem(playerId, empty);
                        
                        if (empty || playerId == null) {
                            setText(null);
                            setGraphic(null);
                            setStyle("-fx-background-color: transparent;");
                        } else {
                            // Container Baris
                            HBox row = new HBox(10);
                            row.setAlignment(Pos.CENTER_LEFT);
                            row.setPadding(new Insets(5));

                            // Nama Player
                            int myId = App.gameState.getMyPlayerId();
                            int hostId = App.gameState.getHostPlayerId();
                            
                            StringBuilder sb = new StringBuilder("Player " + playerId);
                            if (playerId == myId) sb.append(" (You)");
                            if (playerId == hostId) sb.append(" [HOST]");
                            
                            Label lblName = new Label(sb.toString());
                            lblName.setTextFill(Color.WHITE);
                            lblName.setFont(new Font("Arial", 16));
                            
                            // Spacer agar tombol ke kanan
                            HBox spacer = new HBox();
                            HBox.setHgrow(spacer, Priority.ALWAYS);
                            
                            row.getChildren().addAll(lblName, spacer);

                            // [FITUR KICK]
                            // Jika SAYA HOST dan TARGET BUKAN SAYA
                            if (App.gameState.amIHost() && playerId != myId) {
                                Button btnKick = new Button("KICK");
                                btnKick.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
                                btnKick.setOnAction(e -> {
                                    System.out.println("Kicking player " + playerId);
                                    if (App.network != null) {
                                        App.network.send("KICK_PLAYER;" + playerId);
                                    }
                                });
                                row.getChildren().add(btnKick);
                            }

                            setGraphic(row);
                            setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
                        }
                    }
                };
            }
        });
        
        root.setCenter(playerList);

        // SYNC LOGIC
        if (App.gameState != null) {
            App.gameState.setOnRoomStateUpdate(v -> updateUI());
            updateUI();
        }

        // BUTTONS
        btnStart = new Button("START GAME");
        Button btnLeave = new Button("LEAVE ROOM");
        
        btnStart.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-pref-width: 200;");
        btnLeave.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-pref-width: 200;");

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
            // Update Data List
            var ids = App.gameState.getRoomPlayerIds();
            playerList.getItems().clear();
            playerList.getItems().addAll(ids); // Masukkan ID integer langsung
            
            // Logic Button Start
            boolean isHost = App.gameState.amIHost();
            boolean enoughPlayers = ids.size() >= 2; // Bisa start sendiri buat testing
            
            if (isHost) {
                btnStart.setDisable(!enoughPlayers);
                btnStart.setText(enoughPlayers ? "START GAME" : "WAITING FOR PLAYERS...");
                btnStart.setVisible(true);
            } else {
                btnStart.setDisable(true);
                btnStart.setText("WAITING FOR HOST...");
                // Opsional: Sembunyikan tombol start jika bukan host agar bersih
                // btnStart.setVisible(false); 
            }
        });
    }

    public Scene getScene() { return scene; }
}