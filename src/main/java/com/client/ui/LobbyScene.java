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
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;

public class LobbyScene {
    private final Scene scene;

    public LobbyScene() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #2c3e50;"); 

        // HEADER
        Label title = new Label("GAME LOBBY");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        title.setTextFill(Color.WHITE);
        BorderPane.setAlignment(title, Pos.CENTER);
        root.setTop(title);

        // ROOM LIST
        ListView<String> roomList = new ListView<>();
        roomList.setStyle("-fx-control-inner-background: #34495e; -fx-text-fill: white;");
        
        VBox centerBox = new VBox(10, new Label("Available Rooms:") {{ setTextFill(Color.WHITE); }}, roomList);
        centerBox.setPadding(new Insets(20, 0, 20, 0));
        root.setCenter(centerBox);

        // --- PENTING: LISTEN UPDATE DARI SERVER ---
        // 1. Isi awal (jika data sudah ada)
        if (App.gameState != null) {
            roomList.getItems().addAll(App.gameState.getAvailableRooms());
            
            // 2. Set callback untuk update otomatis
            App.gameState.setOnRoomListUpdate(rooms -> {
                // Update UI wajib di JavaFX Thread
                Platform.runLater(() -> {
                    roomList.getItems().clear();
                    roomList.getItems().addAll(rooms);
                });
            });
        }

        // BUTTONS
        Button btnJoin = new Button("Join Selected");
        Button btnCreate = new Button("Create Room");
        styleButton(btnJoin);
        styleButton(btnCreate);

        HBox bottomBox = new HBox(15, btnJoin, btnCreate);
        bottomBox.setAlignment(Pos.CENTER);
        root.setBottom(bottomBox);

        // LOGIC BUTTONS
        btnCreate.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("Room 1");
            dialog.setTitle("Create Room");
            dialog.setHeaderText("Masukkan Nama Room:");
            dialog.showAndWait().ifPresent(name -> {
                if(App.network != null) App.network.send("CREATE_ROOM;" + name);
                SceneManager.toRoom(name);
            });
        });
        
        btnJoin.setOnAction(e -> {
            String selected = roomList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if(App.network != null) App.network.send("JOIN_ROOM;" + selected);
                SceneManager.toRoom(selected);
            }
        });

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        this.scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
    }

    private void styleButton(Button btn) {
        btn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        btn.setPrefWidth(150);
        btn.setPrefHeight(40);
    }

    public Scene getScene() { return scene; }
}