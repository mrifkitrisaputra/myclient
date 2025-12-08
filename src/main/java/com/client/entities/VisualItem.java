package com.client.entities;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class VisualItem {
    
    public enum Type {
        BOMB_UP, FIRE_UP, SPEED_UP
    }

    // --- UPDATE: Ganti nama variabel agar sesuai dengan ClientGameState ---
    public final int tileX; 
    public final int tileY; 
    public final Type type;

    private static Image imgBombUp;
    private static Image imgFireUp;
    private static Image imgSpeedUp;
    private final int tileSize = 32;

    public VisualItem(int x, int y, Type type) {
        this.tileX = x; // Assign ke tileX
        this.tileY = y; // Assign ke tileY
        this.type = type;
        
        // Load static agar hemat memori (kalau belum di-load)
        if (imgBombUp == null) {
            loadImages();
        }
    }

    private void loadImages() {
        try {
            // Pastikan nama folder "item" atau "items" sesuai project kamu
            imgBombUp = new Image(getClass().getResourceAsStream("/com/client/assets/item/BombUp.png"), tileSize, tileSize, false, false);
            imgFireUp = new Image(getClass().getResourceAsStream("/com/client/assets/item/FireUp.png"), tileSize, tileSize, false, false);
            imgSpeedUp = new Image(getClass().getResourceAsStream("/com/client/assets/item/SpeedUp.png"), tileSize, tileSize, false, false);
        } catch (Exception e) {
            System.err.println("Gagal load gambar item: " + e.getMessage());
        }
    }

    public void render(GraphicsContext g) {
        Image img = switch (type) {
            case BOMB_UP -> imgBombUp;
            case FIRE_UP -> imgFireUp;
            case SPEED_UP -> imgSpeedUp;
        };
        
        double px = tileX * tileSize;
        double py = tileY * tileSize;

        // Render Gambar jika berhasil di-load
        if (img != null && !img.isError()) {
            g.drawImage(img, px, py);
        } else {
            // FALLBACK: Gambar Kotak Biru Kecil jika gambar gagal di-load / path salah
            g.setFill(Color.CYAN);
            g.fillRect(px + 8, py + 8, 16, 16);
            g.setStroke(Color.BLACK);
            g.strokeRect(px + 8, py + 8, 16, 16);
        }
    }
}