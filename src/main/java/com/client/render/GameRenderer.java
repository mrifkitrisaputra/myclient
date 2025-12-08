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

        // Load Tiles
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

        // 2. Render Map
        renderMap(g, map);

        // 3. Render Entities
        for (VisualItem item : gameState.getItems()) item.render(g);
        for (VisualBomb bomb : gameState.getBombs()) bomb.render(g);
        for (VisualExplosion exp : gameState.getExplosions()) exp.render(g);
        for (VisualPlayer player : gameState.getPlayers()) player.render(g);
        
        // 4. RENDER DEBUG (Jika aktif)
        if (gameState.isDebugMode()) {
            renderDebug(g, map);
        }
        
        g.restore();

        // 5. Render UI
        renderUI(g);
    }

    // --- VISUAL DEBUG BARU ---
    private void renderDebug(GraphicsContext g, int[][] map) {
        // 1. Gambar Grid Map & Solid Box
        g.setLineWidth(1.0);
        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[0].length; y++) {
                // Gambar garis grid tipis (Cyan)
                g.setStroke(Color.CYAN.deriveColor(0, 1, 1, 0.3));
                g.strokeRect(x * tile, y * tile, tile, tile);

                // Highlight tembok solid (Merah transparan)
                if (map[x][y] == 1 || map[x][y] == 2) {
                    g.setFill(Color.RED.deriveColor(0, 1, 1, 0.3));
                    g.fillRect(x * tile, y * tile, tile, tile);
                }
            }
        }

        // 2. Gambar Hitbox Player
        // Hitbox = 20.5px, Offset = (32 - 20.5)/2 = 5.75
        double hitboxSize = 20.5;
        double offset = (tile - hitboxSize) / 2.0;

        for (VisualPlayer p : gameState.getPlayers()) {
            double hx = p.getX() + offset;
            double hy = p.getY() + offset;

            // Gambar Kotak Hijau (Outline) -> Area Collision Player
            g.setStroke(Color.LIME);
            g.setLineWidth(1.5);
            g.strokeRect(hx, hy, hitboxSize, hitboxSize);
            
            // Gambar Kotak Kuning (Outline) -> Posisi Tile penuh (32x32)
            g.setStroke(Color.YELLOW);
            g.setLineWidth(0.5);
            g.strokeRect(p.getX(), p.getY(), tile, tile);
        }
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
                g.drawImage(img, x * tile, y * tile);
            }
        }
    }

    private void renderLoading(GraphicsContext g) {
        g.setFill(Color.WHITE);
        g.setFont(new Font("Arial", 30));
        g.fillText("Connecting to Server...", canvas.getWidth()/2 - 100, canvas.getHeight()/2);
    }

    private void renderUI(GraphicsContext g) {
        if (gameState.isGameOver()) {
            g.setFill(Color.RED);
            g.setFont(new Font("Impact", 60));
            g.fillText("GAME OVER", canvas.getWidth()/2 - 150, canvas.getHeight()/2);
        }

        g.setFill(Color.WHITE);
        g.setFont(new Font("Consolas", 30));
        // --- [UPDATE] FORMAT WAKTU (MM:SS) ---
        double totalSeconds = gameState.getGameTime();
        
        // Pastikan tidak negatif
        if (totalSeconds < 0) totalSeconds = 0; 

        int minutes = (int) (totalSeconds / 60);
        int seconds = (int) (totalSeconds % 60);

        // Format string sesuai request: "00:00"
        String timeStr = String.format("%02d:%02d", minutes, seconds);
        
        g.fillText("Time: " + timeStr, 20, 40);
        
        // Info Debug di pojok kanan atas
        if (gameState.isDebugMode()) {
            g.setFill(Color.YELLOW);
            g.setFont(new Font("Consolas", 14));
            g.fillText("[DEBUG ON]", canvas.getWidth() - 100, 30);
        }
    }
}