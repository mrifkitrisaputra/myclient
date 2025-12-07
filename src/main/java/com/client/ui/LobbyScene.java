package com.client.ui;

import com.client.App;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

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
        roomList.setStyle("-fx-control-inner-background: #34495e; -fx-text-fill: white; -fx-font-size: 14px;");
        
        VBox centerBox = new VBox(10, new Label("Available Rooms:") {{ setTextFill(Color.WHITE); }}, roomList);
        centerBox.setPadding(new Insets(20, 0, 20, 0));
        root.setCenter(centerBox);

        // SYNC ROOM LIST
        if (App.gameState != null) {
            roomList.getItems().addAll(App.gameState.getAvailableRooms());
            App.gameState.setOnRoomListUpdate(rooms -> {
                Platform.runLater(() -> {
                    roomList.getItems().clear();
                    roomList.getItems().addAll(rooms);
                });
            });
        }

        // BUTTONS
        Button btnJoin = new Button("Join Room");
        Button btnCreate = new Button("Create Room");
        styleButton(btnJoin, "#2980b9");
        styleButton(btnCreate, "#27ae60");

        HBox bottomBox = new HBox(15, btnJoin, btnCreate);
        bottomBox.setAlignment(Pos.CENTER);
        root.setBottom(bottomBox);

        // --- LOGIC CREATE ROOM ---
        btnCreate.setOnAction(e -> showCreateRoomDialog());
        
        // --- LOGIC JOIN ROOM ---
        btnJoin.setOnAction(e -> {
            String selected = roomList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Parse nama room "RoomName:(Private)" -> "RoomName"
                String roomName = selected.split(":")[0];
                boolean isPrivate = selected.contains("(Private)");

                if (isPrivate) {
                    TextInputDialog passDialog = new TextInputDialog();
                    passDialog.setTitle("Private Room");
                    passDialog.setHeaderText("Enter Password for " + roomName);
                    passDialog.showAndWait().ifPresent(pass -> {
                        joinRoomRequest(roomName, pass);
                    });
                } else {
                    joinRoomRequest(roomName, "");
                }
            }
        });

        this.scene = new Scene(root, 800, 600);
    }
    
    private void showCreateRoomDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Room");
        dialog.setHeaderText("Room Settings");

        ButtonType createBtnType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Room Name");
        CheckBox privateCheck = new CheckBox("Private Room");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Password");
        passField.setDisable(true);

        privateCheck.selectedProperty().addListener((obs, old, isSelected) -> {
            passField.setDisable(!isSelected);
        });

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(privateCheck, 0, 1);
        grid.add(new Label("Password:"), 0, 2);
        grid.add(passField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createBtnType) {
                String name = nameField.getText();
                boolean isPriv = privateCheck.isSelected();
                String pass = passField.getText();
                if(!name.isEmpty()) {
                    // Send: CREATE_ROOM;Name;true/false;pass
                    if(App.network != null) 
                        App.network.send("CREATE_ROOM;" + name + ";" + isPriv + ";" + pass);
                    
                    // Pindah scene langsung (validasi error server nanti via popup kalau mau advanced)
                    SceneManager.toRoom(name);
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void joinRoomRequest(String name, String pass) {
        if(App.network != null) 
            App.network.send("JOIN_ROOM;" + name + ";" + pass);
        SceneManager.toRoom(name);
    }

    private void styleButton(Button btn, String colorHex) {
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        btn.setPrefWidth(150);
        btn.setPrefHeight(40);
    }

    public Scene getScene() { return scene; }
}