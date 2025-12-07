package com.client.ui;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.client.network.InputSender;
import com.client.render.GameCanvas;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class SceneManager {
    private static Stage stage;
    
    // Untuk mencegah spam input saat tombol ditahan
    private static final Set<KeyCode> pressedKeys = new HashSet<>();

    public static void setStage(Stage s) {
        stage = s;
    }

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

    /**
     * Masuk ke Game.
     * @param canvas Canvas untuk render
     * @param inputSender Object untuk kirim data ke server
     */
    public static void toGame(GameCanvas canvas, InputSender inputSender) {
        StackPane root = new StackPane(canvas);
        Scene gameScene = new Scene(root);
        
        // Reset tombol saat masuk game baru
        pressedKeys.clear();

        // --- KEYBOARD LISTENERS ---
        
        gameScene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            
            // Cek apakah tombol sudah tercatat ditekan (untuk hindari spam input)
            if (!pressedKeys.contains(code)) {
                pressedKeys.add(code);
                handleInput(code, true, inputSender);
                
                // Optional: Zoom camera local
                canvas.handleInput(code); 
            }
        });

        gameScene.setOnKeyReleased(e -> {
            KeyCode code = e.getCode();
            pressedKeys.remove(code);
            handleInput(code, false, inputSender);
        });

        // Resize Canvas otomatis
        gameScene.widthProperty().addListener((obs, oldV, newV) -> canvas.setWidth(newV.doubleValue()));
        gameScene.heightProperty().addListener((obs, oldV, newV) -> canvas.setHeight(newV.doubleValue()));

        stage.setScene(gameScene);
        stage.setTitle("Bomber Game - Playing");
        canvas.requestFocus(); // Fokus agar keyboard terbaca
        
        // Pastikan maximized
        stage.setMaximized(true);
    }

    private static void handleInput(KeyCode code, boolean isPressed, InputSender sender) {
        // Mapping Tombol JavaFX -> Protokol Server
        String keyCommand = null;

        switch (code) {
            case W, UP -> keyCommand = "UP";
            case S, DOWN -> keyCommand = "DOWN";
            case A, LEFT -> keyCommand = "LEFT";
            case D, RIGHT -> keyCommand = "RIGHT";
            case SPACE -> {
                // Untuk Space (Bom), kita kirim ACTION hanya saat ditekan (bukan dilepas)
                if (isPressed) {
                    sender.sendPlaceBomb();
                }
                return; 
            }
            case ESCAPE -> {
                if (isPressed) {
                    // Tampilkan Pause Menu (Nanti diimplementasikan overlay)
                    System.out.println("Pause Pressed");
                }
                return;
            }
        }

        // Kirim ke server jika tombol valid
        if (keyCommand != null) {
            sender.sendInput(keyCommand, isPressed);
        }
    }
}