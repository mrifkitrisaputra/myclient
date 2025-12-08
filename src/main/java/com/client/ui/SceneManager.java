package com.client.ui;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.client.App; 
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

    public static void toGame(GameCanvas canvas, InputSender inputSender, String roomName) {
        // 1. Setup Root dengan StackPane (Canvas di layer bawah)
        StackPane root = new StackPane(canvas);
        Scene gameScene = new Scene(root);
        
        pressedKeys.clear();

        // 2. Setup PauseMenu (Awalnya tidak ditambahkan ke root/hidden)
        PauseMenu pauseMenu = new PauseMenu(
            () -> { 
                // Aksi Resume: Hapus menu, kembalikan fokus ke game
                root.getChildren().remove(root.getChildren().size() - 1); 
                canvas.requestFocus();
            },
            () -> App.leaveGame() // Aksi Leave: Panggil logic di App
        );

        gameScene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            
            // Cek apakah Menu sedang terbuka
            boolean isMenuOpen = root.getChildren().contains(pauseMenu);

            // --- LOGIC ESCAPE (PAUSE) ---
            if (code == KeyCode.ESCAPE) {
                if (isMenuOpen) {
                    // Resume Game
                    root.getChildren().remove(pauseMenu);
                } else {
                    // Pause Game
                    root.getChildren().add(pauseMenu);
                    // PENTING: Hentikan semua pergerakan saat menu dibuka
                    resetMovement(inputSender); 
                    pressedKeys.clear();
                }
                return; // Stop proses key lain
            }

            // Jika Menu Buka, BLOCK semua input game lain
            if (isMenuOpen) return;

            // --- TOMBOL DEBUG (F3) ---
            if (code == KeyCode.F3) {
                if (App.gameState != null) {
                    App.gameState.toggleDebug();
                    System.out.println("Debug Mode: " + App.gameState.isDebugMode());
                }
                return;
            }

            // --- INPUT GAME (WASD/SPACE) ---
            if (!pressedKeys.contains(code)) {
                pressedKeys.add(code);
                handleInput(code, true, inputSender);
                canvas.handleInput(code); 
            }
        });

        gameScene.setOnKeyReleased(e -> {
            KeyCode code = e.getCode();
            boolean isMenuOpen = root.getChildren().contains(pauseMenu);
            
            // Jika Menu Buka, abaikan release (karena input sudah di-reset saat ESC ditekan)
            if (isMenuOpen) return;

            pressedKeys.remove(code);
            handleInput(code, false, inputSender);
        });

        // Listener resize window agar canvas mengikuti
        gameScene.widthProperty().addListener((obs, oldV, newV) -> canvas.setWidth(newV.doubleValue()));
        gameScene.heightProperty().addListener((obs, oldV, newV) -> canvas.setHeight(newV.doubleValue()));

        stage.setScene(gameScene);
        stage.setTitle("Bomber Game [" + roomName + "] Playing");
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

    // Helper untuk menghentikan paksa karakter saat menu dibuka
    private static void resetMovement(InputSender sender) {
        sender.sendInput("UP", false);
        sender.sendInput("DOWN", false);
        sender.sendInput("LEFT", false);
        sender.sendInput("RIGHT", false);
    }
}