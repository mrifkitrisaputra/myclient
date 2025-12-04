package com.client.entities;

import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class VisualExplosion {
    // Data visual murni
    public static record ExplosionPart(int x, int y, boolean isVertical) {}

    private final int centerX, centerY;
    private final List<ExplosionPart> parts;
    
    // Animasi
    private final Image centerImg;
    private final Image[] hAnimation; 
    private final Image[] vAnimation; 

    private float animTimer = 0;
    private int frameIndex = 0;
    private final int totalFrames = 4; 
    private final int tileSize = 32;

    // Constructor menerima parts yang sudah dihitung Server
    public VisualExplosion(int startX, int startY, List<ExplosionPart> serverParts) {
        this.centerX = startX;
        this.centerY = startY;
        this.parts = serverParts; // Terima daftar koordinat dari server

        // Load Asset
        centerImg = new Image(getClass().getResourceAsStream("/com/client/assets/explosion/e_largeexplosion1.png"), tileSize, tileSize, false, false);
        
        hAnimation = new Image[4];
        vAnimation = new Image[4];
        for (int i = 0; i < 4; i++) {
            hAnimation[i] = new Image(getClass().getResourceAsStream("/com/client/assets/explosion/e_horizontal" + (i+1) + ".png"), tileSize, tileSize, false, false);
            vAnimation[i] = new Image(getClass().getResourceAsStream("/com/client/assets/explosion/e_vertical" + (i+1) + ".png"), tileSize, tileSize, false, false);
        }
    }

    public void update(double dt) {
        // Hanya update animasi api membesar/mengecil
        float timePerFrame = 0.8f / totalFrames; 
        animTimer += dt;
        
        if (animTimer >= timePerFrame) {
            animTimer = 0;
            frameIndex++;
            if (frameIndex >= totalFrames) frameIndex = totalFrames - 1; 
        }
    }

    public void render(GraphicsContext g) {
        // Render Pusat
        g.drawImage(centerImg, centerX * tileSize, centerY * tileSize);

        // Render Bagian Api (Daftar koordinat dari Server)
        for (ExplosionPart part : parts) {
            Image sprite = part.isVertical ? vAnimation[frameIndex] : hAnimation[frameIndex];
            g.drawImage(sprite, part.x * tileSize, part.y * tileSize);
        }
    }
}