package com.client.render;

import com.client.ClientGameState;
import com.client.entities.VisualBomb;
import com.client.entities.VisualExplosion;
import com.client.entities.VisualItem;
import com.client.entities.VisualPlayer;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class GameRenderer {

    private final GameCanvas canvas;
    private final ClientGameState gameState;
    private final CameraScaler scaler;
    
    private final int tile = 32;
    private final Image ground, breakable, unbreak;

    public GameRenderer(GameCanvas canvas, ClientGameState gameState, CameraScaler scaler) {
        this.canvas = canvas;
        this.gameState = gameState;
        this.scaler = scaler;

        // Load Tiles (Pastikan path benar)
        ground = new Image(getClass().getResourceAsStream("/com/client/assets/tiles/ground.png"), tile, tile, false, false);
        breakable = new Image(getClass().getResourceAsStream("/com/client/assets/tiles/break.png"), tile, tile, false, false);
        unbreak = new Image(getClass().getResourceAsStream("/com/client/assets/tiles/unbreak.png"), tile, tile, false, false);
    }

    public void render() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        // 1. Bersihkan Layar
        g.setFill(Color.web("#2c3e50"));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        int[][] map = gameState.getMap();
        if (map == null) {
            renderLoading(g);
            return;
        }

        double scale = scaler.getScale();
        double mapPixelW = map.length * tile * scale;
        double mapPixelH = map[0].length * tile * scale;
        double offsetX = (canvas.getWidth() - mapPixelW) / 2.0;
        double offsetY = (canvas.getHeight() - mapPixelH) / 2.0;

        g.save();
        g.translate(offsetX, offsetY);
        g.scale(scale, scale);

        // 2. Render Map (Lantai & Tembok)
        renderMap(g, map);

        renderArenaShrink(g);

        // 3. Render Entities (Urutan Penting)
        // Item di bawah bom/player
        for (VisualItem item : gameState.getItems()) item.render(g);
        
        // Bom dan Ledakan
        for (VisualBomb bomb : gameState.getBombs()) bomb.render(g);
        for (VisualExplosion exp : gameState.getExplosions()) exp.render(g);
        
        // Player paling atas
        for (VisualPlayer player : gameState.getPlayers()) player.render(g);
        
        // 4. RENDER DEBUG (Jika aktif, tekan tombol debug utk lihat hitbox)
        if (gameState.isDebugMode()) {
            renderDebug(g, map);
        }
        
        g.restore();

        // 5. Render UI
        renderUI(g);
    }

    private void renderMap(GraphicsContext g, int[][] map) {
        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[0].length; y++) {
                int type = map[x][y];
                Image img = switch(type) {
                    case 1 -> unbreak;
                    case 2 -> breakable;
                    default -> ground;
                };
                // Selalu gambar ground dulu biar background ga bolong pas tembok hancur
                if (type != 0) g.drawImage(ground, x * tile, y * tile);
                g.drawImage(img, x * tile, y * tile);
            }
        }
    }

    private void renderDebug(GraphicsContext g, int[][] map) {
        g.setLineWidth(1.0);
        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[0].length; y++) {
                g.setStroke(Color.CYAN.deriveColor(0, 1, 1, 0.3));
                g.strokeRect(x * tile, y * tile, tile, tile);
                if (map[x][y] == 1 || map[x][y] == 2) {
                    g.setFill(Color.RED.deriveColor(0, 1, 1, 0.3));
                    g.fillRect(x * tile, y * tile, tile, tile);
                }
            }
        }
        
        double hitboxSize = 20.5;
        double offset = (tile - hitboxSize) / 2.0;

        for (VisualPlayer p : gameState.getPlayers()) {
            g.setStroke(Color.LIME);
            g.strokeRect(p.getX() + offset, p.getY() + offset, hitboxSize, hitboxSize);
        }
    }

    private void renderLoading(GraphicsContext g) {
        g.setFill(Color.WHITE);
        g.setFont(new Font("Arial", 30));
        g.fillText("Connecting to Server...", canvas.getWidth()/2 - 100, canvas.getHeight()/2);
    }

    private void renderUI(GraphicsContext g) {
        // Game Over Overlay
        if (gameState.isGameOver()) {
            g.setFill(Color.RED);
            g.setFont(new Font("Impact", 60));
            g.fillText("GAME OVER", canvas.getWidth()/2 - 150, canvas.getHeight()/2);
        }

        // Timer UI
        g.setFill(Color.WHITE);
        g.setFont(new Font("Consolas", 30));
        
        double totalSeconds = gameState.getGameTime();
        if (totalSeconds < 0) totalSeconds = 0; 
        int minutes = (int) (totalSeconds / 60);
        int seconds = (int) (totalSeconds % 60);
        String timeStr = String.format("%02d:%02d", minutes, seconds);
        
        g.fillText("Time: " + timeStr, 20, 40);
        
        // Debug Indicator
        if (gameState.isDebugMode()) {
            g.setFill(Color.YELLOW);
            g.setFont(new Font("Consolas", 14));
            g.fillText("[DEBUG ON]", canvas.getWidth() - 100, 30);
        }
    }

    private void renderArenaShrink(GraphicsContext g) {
        // Cek apakah ada warning aktif di state
        if (!gameState.isShrinking()) return;

        // Logic Denyut Jantung (Math.sin)
        // Semakin timer mendekati 0, semakin cepat denyutnya (biar tegang)
        double time = gameState.getShrinkTimer();
        double speed = 10 + (5.0 - time) * 5; // Makin lama makin cepat
        
        // Alpha oscillate antara 0.2 sampai 0.6 (Merah Transparan)
        double alpha = 0.2 + 0.4 * Math.abs(Math.sin(System.nanoTime() / 1e9 * speed));
        
        g.setFill(Color.rgb(255, 0, 0, alpha));

        // Ambil koordinat grid dari GameState
        int left = gameState.getShrinkLeft();
        int right = gameState.getShrinkRight();
        int top = gameState.getShrinkTop();
        int bottom = gameState.getShrinkBottom();
        String pattern = gameState.getShrinkPattern();
        
        // Ambil ukuran map total (untuk panjang baris/kolom)
        // Kita bisa ambil dari map.length atau hitung manual 13x13
        int mapW = gameState.getMap().length; 
        int mapH = gameState.getMap()[0].length;

        // Gambar Overlay Merah sesuai pola
        if ("LR".equals(pattern)) {
            // Kotak Kiri
            g.fillRect(left * tile, 0, tile, mapH * tile);
            // Kotak Kanan
            g.fillRect(right * tile, 0, tile, mapH * tile);
        } 
        else if ("TB".equals(pattern)) {
            // Kotak Atas
            g.fillRect(0, top * tile, mapW * tile, tile);
            // Kotak Bawah
            g.fillRect(0, bottom * tile, mapW * tile, tile);
        }
    }
}