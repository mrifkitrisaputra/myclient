package com.client.entities;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class VisualBomb {
    public final int tileX;
    public final int tileY;
    
    // Animasi
    private Image[] sprites;
    private final float frameInterval = 0.2f;
    private float animTimer = 0;
    private int frameIndex = 0;

    private final int tileSize = 32;
    private final int bombSize = 28;
    
    // Status gambar
    private boolean imageLoaded = false;

    public VisualBomb(int tileX, int tileY) {
        this.tileX = tileX;
        this.tileY = tileY;

        try {
            sprites = new Image[3];
            // Pastikan folder ini benar ada di src/main/resources atau src
            sprites[0] = new Image(getClass().getResourceAsStream("/com/client/assets/bomb/bom1.png"), bombSize, bombSize, false, false);
            sprites[1] = new Image(getClass().getResourceAsStream("/com/client/assets/bomb/bom2.png"), bombSize, bombSize, false, false);
            sprites[2] = new Image(getClass().getResourceAsStream("/com/client/assets/bomb/bom3.png"), bombSize, bombSize, false, false);
            
            // Cek simple apakah load sukses
            if (!sprites[0].isError()) {
                imageLoaded = true;
            }
        } catch (Exception e) {
            System.err.println("GAGAL LOAD GAMBAR BOM: " + e.getMessage());
            imageLoaded = false;
        }
    }

    public void update(double dt) {
        animTimer += dt;
        if (animTimer >= frameInterval) {
            animTimer = 0;
            if (imageLoaded) {
                frameIndex = (frameIndex + 1) % sprites.length;
            } else {
                frameIndex = (frameIndex + 1) % 2; // Kedip2 kalau error
            }
        }
    }

    public void render(GraphicsContext g) {
        double px = tileX * tileSize;
        double py = tileY * tileSize;
        double offset = (tileSize - bombSize) / 2.0;

        if (imageLoaded) {
            g.drawImage(sprites[frameIndex], px + offset, py + offset, bombSize, bombSize);
        } else {
            // FALLBACK VISUAL: Lingkaran Merah
            g.setFill(frameIndex == 0 ? Color.DARKRED : Color.RED);
            g.fillOval(px + offset, py + offset, bombSize, bombSize);
            g.setStroke(Color.BLACK);
            g.strokeOval(px + offset, py + offset, bombSize, bombSize);
        }
    }
}