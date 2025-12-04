package com.client;

import java.io.IOException;

import com.client.network.ClientNetworkManager;
import com.client.render.GameCanvas;
import com.client.ui.LoginScene;
import com.client.ui.SceneManager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class App extends Application {

    private static Stage primaryStage;
    
    // Global Access (Sederhana untuk skala ini)
    public static ClientNetworkManager network;
    public static ClientGameState gameState;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        SceneManager.setStage(stage);

        // 1. Init Game State (Memori Data Game)
        gameState = new ClientGameState();

        // 2. Tampilkan Login Scene dulu (Input IP)
        LoginScene login = new LoginScene(this::tryConnect);
        stage.setMaximized(true);
        stage.setScene(login.getScene());
        stage.setTitle("Bomber Client - Connect");
        stage.show();
    }

    /**
     * Logika Koneksi:
     * Dipanggil saat tombol "Connect" di LoginScene ditekan.
     */
    public void tryConnect(String ip) {
        int port = 5000;
        
        // Safety: Close koneksi lama jika ada (misal retry setelah disconnect)
        if (network != null) {
            network.close();
            network = null; // Pastikan null agar GC bisa bekerja
        }

        // PERBAIKAN: Gunakan variabel lokal 'manager' untuk inisialisasi
        // Ini mencegah Race Condition jika 'network' static berubah di tengah jalan
        ClientNetworkManager manager = new ClientNetworkManager(ip, port, gameState);
        network = manager; // Update global reference
        
        // Set callback jika putus koneksi saat main
        manager.setOnDisconnect(() -> Platform.runLater(() -> {
            // Cek apakah manager yang putus ini adalah manager yang sedang aktif
            if (network == manager) {
                showError("Connection Lost", "Disconnected from server.");
                SceneManager.toLogin(this::tryConnect);
            }
        }));

        // Coba Konek (Jalankan di Thread terpisah agar UI tidak freeze)
        new Thread(() -> {
            try {
                manager.connect(); // BLOCKING - Gunakan local variable 'manager'
                
                // Jika lolos baris ini, berarti sukses connect
                Platform.runLater(() -> {
                    // Cek lagi apakah kita masih menggunakan manager yang sama
                    if (network == manager) {
                        System.out.println("Connected to " + ip);
                        SceneManager.toLobby(); // Masuk Lobby
                    }
                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    // Hanya tampilkan error jika ini adalah attempt terakhir
                    if (network == manager) {
                        showError("Connection Failed", "Cannot reach server at " + ip + "\n" + e.getMessage());
                        SceneManager.toLogin(this::tryConnect);
                    }
                });
            }
        }).start();
    }

    /**
     * Memulai Game (Pindah ke Canvas)
     */
    public static void startGame() {
        if (gameState == null || network == null) return;
        
        GameCanvas gameCanvas = new GameCanvas(gameState);
        gameCanvas.start();
        SceneManager.toGame(gameCanvas);
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