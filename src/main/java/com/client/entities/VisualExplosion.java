package com.client.entities;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class VisualExplosion {
    
    public final int tileX;
    public final int tileY;
    private final boolean isVertical;
    
    // Lifecycle: Menandakan animasi selesai agar dihapus dari GameState
    private boolean finished = false;

    // Animasi
    private Image[] sprites;
    private final float frameDuration = 0.1f; // Kecepatan animasi (0.1 detik per frame)
    private float animTimer = 0;
    private int frameIndex = 0;
    
    // Status visual
    private boolean imageLoaded = false;
    private final int tileSize = 32;

    // Constructor: Menerima 1 koordinat tile, bukan List
    public VisualExplosion(int x, int y, boolean isVertical) {
        this.tileX = x;
        this.tileY = y;
        this.isVertical = isVertical;

        loadResources();
    }

    private void loadResources() {
        try {
            // Kita siapkan array 4 frame (sesuai aset kamu)
            sprites = new Image[4];
            String type = isVertical ? "vertical" : "horizontal";

            // Loop load e_horizontal1.png s/d e_horizontal4.png
            for (int i = 0; i < 4; i++) {
                String path = "/com/client/assets/explosion/e_" + type + (i + 1) + ".png";
                sprites[i] = new Image(getClass().getResourceAsStream(path), tileSize, tileSize, false, false);
            }
            
            // Cek validitas gambar pertama
            if (!sprites[0].isError()) {
                imageLoaded = true;
            }
        } catch (Exception e) {
            System.err.println("GAGAL LOAD GAMBAR EXPLOSION: " + e.getMessage());
            imageLoaded = false;
        }
    }

    public void update(double dt) {
        if (finished) return;

        animTimer += dt;
        if (animTimer >= frameDuration) {
            animTimer = 0;
            frameIndex++;
            
            // Jika frame sudah melebihi jumlah sprite, animasi selesai
            if (imageLoaded && frameIndex >= sprites.length) {
                finished = true;
            } 
            // Fallback logic jika gambar tidak ada (durasi manual)
            else if (!imageLoaded && frameIndex >= 5) {
                finished = true;
            }
        }
    }

    public void render(GraphicsContext g) {
        if (finished) return;

        double px = tileX * tileSize;
        double py = tileY * tileSize;

        if (imageLoaded) {
            // Safety check array bounds
            if (frameIndex < sprites.length) {
                g.drawImage(sprites[frameIndex], px, py);
            }
        } else {
            // FALLBACK: Kotak Oranye Transparan
            g.setFill(Color.ORANGE.deriveColor(0, 1, 1, 0.7));
            g.fillRect(px, py, tileSize, tileSize);
            
            g.setStroke(Color.RED);
            g.strokeRect(px, py, tileSize, tileSize);
        }
    }

    // Dipanggil oleh ClientGameState untuk bersih-bersih memory
    public boolean isFinished() {
        return finished;
    }
}