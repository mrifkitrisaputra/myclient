package com.client.entities;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class VisualItem {
    
    public enum Type {
        BOMB_UP, FIRE_UP, SPEED_UP
    }

    public int x; // Grid X
    public int y; // Grid Y
    public Type type;

    private static Image imgBombUp;
    private static Image imgFireUp;
    private static Image imgSpeedUp;
    private final int tileSize = 32;

    public VisualItem(int x, int y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
        
        // Load static agar hemat memori (kalau belum di-load)
        if (imgBombUp == null) {
            loadImages();
        }
    }

    private void loadImages() {
        try {
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
        
        if (img != null) {
            g.drawImage(img, x * tileSize, y * tileSize);
        }
    }
}