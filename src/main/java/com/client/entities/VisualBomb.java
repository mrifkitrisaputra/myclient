package com.client.entities;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class VisualBomb {
    public final int tileX;
    public final int tileY;
    
    // Animasi
    private final Image[] sprites;
    private final float frameInterval = 0.2f;
    private float animTimer = 0;
    private int frameIndex = 0;

    private final int tileSize = 32;
    private final int bombSize = 28;

    public VisualBomb(int tileX, int tileY) {
        this.tileX = tileX;
        this.tileY = tileY;

        // Load Gambar langsung (Atau inject SpriteLoader lebih baik)
        sprites = new Image[3];
        sprites[0] = new Image(getClass().getResourceAsStream("/com/client/assets/bomb/bom1.png"), bombSize, bombSize, false, false);
        sprites[1] = new Image(getClass().getResourceAsStream("/com/client/assets/bomb/bom2.png"), bombSize, bombSize, false, false);
        sprites[2] = new Image(getClass().getResourceAsStream("/com/client/assets/bomb/bom3.png"), bombSize, bombSize, false, false);
    }

    public void update(double dt) {
        // Hanya update animasi denyut
        animTimer += dt;
        if (animTimer >= frameInterval) {
            animTimer = 0;
            frameIndex = (frameIndex + 1) % sprites.length;
        }
    }

    public void render(GraphicsContext g) {
        double px = tileX * tileSize;
        double py = tileY * tileSize;
        double offset = (tileSize - bombSize) / 2.0;

        g.drawImage(sprites[frameIndex], px + offset, py + offset, bombSize, bombSize);
    }
}