package com.client.ui;

import com.client.App;

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
        ListView<String> playerList = new ListView<>();
        playerList.getItems().add("You (Connected)");
        playerList.setStyle("-fx-control-inner-background: #34495e; -fx-text-fill: white;");
        root.setCenter(playerList);

        // BUTTONS
        Button btnStart = new Button("START GAME");
        Button btnLeave = new Button("LEAVE ROOM");
        
        btnStart.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px;");
        btnLeave.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 16px;");

        btnStart.setOnAction(e -> {
            // Kirim sinyal ke server bahwa host mau start
            if(App.network != null) App.network.send("START_GAME");
            
            // Untuk testing sekarang, langsung start local
            App.startGame();
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

    public Scene getScene() { return scene; }
}