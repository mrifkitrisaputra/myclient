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

        // Cek apakah data map sudah diterima dari server?
        int[][] map = gameState.getMap();
        if (map == null) {
            renderLoading(g);
            return;
        }

        // Hitung Offset agar map di tengah
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

        // 3. Render Entities (Loop dari list di GameState)
        for (VisualItem item : gameState.getItems()) {
            item.render(g);
        }

        for (VisualBomb bomb : gameState.getBombs()) {
            bomb.render(g);
        }

        for (VisualExplosion exp : gameState.getExplosions()) {
            exp.render(g);
        }

        for (VisualPlayer player : gameState.getPlayers()) {
            player.render(g);
        }
        
        g.restore();

        // 4. Render UI
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
        // Timer atau Info Game dari GameState
        if (gameState.isGameOver()) {
            g.setFill(Color.RED);
            g.setFont(new Font("Impact", 60));
            g.fillText("GAME OVER", canvas.getWidth()/2 - 150, canvas.getHeight()/2);
        }

        // Render Game Timer (dikirim server)
        g.setFill(Color.WHITE);
        g.setFont(new Font("Consolas", 30));
        g.fillText("Time: " + (int)gameState.getGameTime(), 20, 40);
    }
}