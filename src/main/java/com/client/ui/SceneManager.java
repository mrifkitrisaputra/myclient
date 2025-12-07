package com.client.ui;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.client.App; // Import App untuk akses GameState
import com.client.network.InputSender;
import com.client.render.GameCanvas;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class SceneManager {
    private static Stage stage;
    private static final Set<KeyCode> pressedKeys = new HashSet<>();

    public static void setStage(Stage s) { stage = s; }

    public static void toLogin(Consumer<String> onConnect) {
        LoginScene login = new LoginScene(onConnect);
        stage.setScene(login.getScene());
        stage.setTitle("Bomber Client - Login");
    }

    public static void toLobby() {
        LobbyScene lobby = new LobbyScene();
        stage.setScene(lobby.getScene());
        stage.setTitle("Bomber Game - Lobby");
    }

    public static void toRoom(String roomName) {
        RoomScene room = new RoomScene(roomName);
        stage.setScene(room.getScene());
        stage.setTitle("Room: " + roomName);
    }

    public static void toGame(GameCanvas canvas, InputSender inputSender) {
        StackPane root = new StackPane(canvas);
        Scene gameScene = new Scene(root);
        
        pressedKeys.clear();

        gameScene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            
            // --- TOMBOL DEBUG (F3) ---
            if (code == KeyCode.F3) {
                if (App.gameState != null) {
                    App.gameState.toggleDebug();
                    System.out.println("Debug Mode: " + App.gameState.isDebugMode());
                }
                return;
            }

            if (!pressedKeys.contains(code)) {
                pressedKeys.add(code);
                handleInput(code, true, inputSender);
                canvas.handleInput(code); 
            }
        });

        gameScene.setOnKeyReleased(e -> {
            KeyCode code = e.getCode();
            pressedKeys.remove(code);
            handleInput(code, false, inputSender);
        });

        gameScene.widthProperty().addListener((obs, oldV, newV) -> canvas.setWidth(newV.doubleValue()));
        gameScene.heightProperty().addListener((obs, oldV, newV) -> canvas.setHeight(newV.doubleValue()));

        stage.setScene(gameScene);
        stage.setTitle("Bomber Game - Playing");
        canvas.requestFocus(); 
        stage.setMaximized(true);
    }

    private static void handleInput(KeyCode code, boolean isPressed, InputSender sender) {
        String keyCommand = null;
        switch (code) {
            case W, UP -> keyCommand = "UP";
            case S, DOWN -> keyCommand = "DOWN";
            case A, LEFT -> keyCommand = "LEFT";
            case D, RIGHT -> keyCommand = "RIGHT";
            case SPACE -> {
                if (isPressed) sender.sendPlaceBomb();
                return; 
            }
        }
        if (keyCommand != null) {
            sender.sendInput(keyCommand, isPressed);
        }
    }
}