package com.client;

import java.io.IOException;

import com.client.network.ClientNetworkManager;
import com.client.network.InputSender;
import com.client.render.GameCanvas;
import com.client.ui.LoginScene;
import com.client.ui.SceneManager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class App extends Application {

    private static Stage primaryStage;
    
    public static ClientNetworkManager network;
    public static ClientGameState gameState;
    
    // BARU: Menyimpan nama room sementara saat request join/create
    public static String pendingRoomName = "";
    private static GameCanvas currentGameCanvas; 

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        SceneManager.setStage(stage);

        gameState = new ClientGameState();

        LoginScene login = new LoginScene(this::tryConnect);
        stage.setScene(login.getScene());
        stage.setTitle("Bomber Client - Connect");
        
        stage.show();
        stage.setMaximized(true);
    }

    public void tryConnect(String ip) {
        int port = 5000;
        
        if (network != null) {
            network.close();
            network = null;
        }

        ClientNetworkManager manager = new ClientNetworkManager(ip, port, gameState);
        network = manager; 
        
        manager.setOnDisconnect(() -> Platform.runLater(() -> {
            if (network == manager) {
                showError("Connection Lost", "Disconnected from server.");
                SceneManager.toLogin(this::tryConnect);
                if (primaryStage != null) primaryStage.setMaximized(true);
            }
        }));

        new Thread(() -> {
            try {
                manager.connect();
                Platform.runLater(() -> {
                    if (network == manager) {
                        System.out.println("Connected to " + ip);
                        SceneManager.toLobby();
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    if (network == manager) {
                        showError("Connection Failed", "Cannot reach server at " + ip + "\n" + e.getMessage());
                        SceneManager.toLogin(this::tryConnect);
                        if (primaryStage != null) primaryStage.setMaximized(true);
                    }
                });
            }
        }).start();
    }

    public static void startGame() {
        if (gameState == null || network == null) return;
        
        // 1. Buat Canvas
        currentGameCanvas = new GameCanvas(gameState);
        
        // 2. Buat Pengirim Input
        InputSender inputSender = new InputSender(network);

        // 3. Mulai Loop Render
        currentGameCanvas.start();
        
        // 4. Pindah Scene (Bawa Canvas & InputSender)
        SceneManager.toGame(currentGameCanvas, inputSender, pendingRoomName);
    }

    public static void leaveGame() {
        // 1. Matikan Render Loop agar tidak makan resource di lobby
        if (currentGameCanvas != null) {
            currentGameCanvas.stop(); 
            currentGameCanvas = null;
        }

        // 2. Kirim paket LEAVE ke server
        if (network != null) {
            network.send("LEAVE_ROOM");
        }

        // 3. Bersihkan state client (opsional, biar bersih saat main lagi)
        if (gameState != null) {
            gameState.players.clear(); 
        }

        // 4. Pindah Scene kembali ke Lobby
        SceneManager.toLobby();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    @Override
    public void stop() throws Exception {
        if (network != null) network.close();
        super.stop();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch();
    }
}