package com.client.entities;

import com.client.render.SpriteAnimation;
import com.client.render.SpriteLoader;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

public class VisualPlayer {

    public int id; // ID Identitas Player
    
    // Posisi Target (Data asli dari Server)
    private double targetX;
    private double targetY;

    // Posisi Visual (Yang digambar di layar, mengejar target)
    private double x;
    private double y;
    
    // Kecepatan smoothing (Semakin besar = semakin responsif tapi bisa jitter dikit)
    // 12.0 angka yang pas buat top-down 60 FPS
    private final double SMOOTHING_SPEED = 12.0;

    // Status Visual
    public enum State { IDLE, WALK, PLACE, DEAD }
    public enum Direction { DOWN, UP, LEFT, RIGHT }

    public State state = State.IDLE;
    public Direction dir = Direction.DOWN;

    // Animasi
    private final SpriteAnimation animIdleDown;
    private final SpriteAnimation animIdleUp;
    private final SpriteAnimation animIdleLeft;
    private final SpriteAnimation animIdleRight;

    private final SpriteAnimation animWalkDown;
    private final SpriteAnimation animWalkUp;
    private final SpriteAnimation animWalkLeft;
    private final SpriteAnimation animWalkRight;

    private final SpriteAnimation animDeath;

    // Offset visual (agar kaki player pas di kotak collision)
    private final int offsetX = -1; 
    private final int offsetY = -1; 

    public VisualPlayer(int id, SpriteLoader loader) {
        this.id = id;
        
        // --- IDLE ---
        animIdleDown  = new SpriteAnimation(loadFrames(loader, "sP2DownIdle_", 1));
        animIdleUp    = new SpriteAnimation(loadFrames(loader, "sP2UpIdle_", 1));
        animIdleLeft  = new SpriteAnimation(loadFrames(loader, "sP2LeftIdle_", 1));
        animIdleRight = new SpriteAnimation(loadFrames(loader, "sP2RightIdle_", 1));

        // --- WALK ---
        animWalkDown  = new SpriteAnimation(loadFrames(loader, "sP2Down_", 2));
        animWalkUp    = new SpriteAnimation(loadFrames(loader, "sP2Up_", 2));
        animWalkLeft  = new SpriteAnimation(loadFrames(loader, "sP2Left_", 3));
        animWalkRight = new SpriteAnimation(loadFrames(loader, "sP2Right_", 3));

        // --- DEATH ---
        animDeath = new SpriteAnimation(loadFrames(loader, "sP2Death_", 7));
        animDeath.setLoop(false);
    }

    /**
     * Dipanggil saat menerima paket data baru dari Server.
     * Kita hanya update TARGET, bukan posisi X/Y visual langsung.
     */
    public void setNetworkState(double serverX, double serverY, State serverState, Direction serverDir) {
        this.targetX = serverX;
        this.targetY = serverY;
        this.state = serverState;
        this.dir = serverDir;

        // Init posisi awal (jika visual belum punya posisi/baru spawn)
        if (x == 0 && y == 0) {
            x = targetX;
            y = targetY;
        }
        
        // Anti-Teleport Jauh: Jika lag spike dan jarak terlalu jauh (> 3 blok), snap langsung
        double dist = Math.sqrt(Math.pow(targetX - x, 2) + Math.pow(targetY - y, 2));
        if (dist > 100) { 
            x = targetX;
            y = targetY;
        }
    }

    private Image[] loadFrames(SpriteLoader loader, String base, int count) {
        Image[] temp = new Image[count];
        int idx = 0;
        for (int i = 0; i < count; i++) {
            Image img = loader.get(base + i);
            if (img != null) temp[idx++] = img;
        }
        if (idx == 0) return new Image[]{ new WritableImage(1, 1) }; // Fallback blank image
        Image[] frames = new Image[idx];
        System.arraycopy(temp, 0, frames, 0, idx);
        return frames;
    }

    // Update dipanggil 60x per detik oleh GameCanvas
    public void update(double dt) {
        
        // 1. LOGIKA INTERPOLASI (SMOOTHING)
        // x akan bergerak mendekati targetX sebesar fraksi tertentu setiap frame
        x += (targetX - x) * SMOOTHING_SPEED * dt;
        y += (targetY - y) * SMOOTHING_SPEED * dt;

        // 2. UPDATE ANIMASI
        switch (state) {
            case IDLE -> {
                switch (dir) {
                    case DOWN -> animIdleDown.update(dt);
                    case UP -> animIdleUp.update(dt);
                    case LEFT -> animIdleLeft.update(dt);
                    case RIGHT -> animIdleRight.update(dt);
                }
            }
            case WALK -> {
                switch (dir) {
                    case DOWN -> animWalkDown.update(dt);
                    case UP -> animWalkUp.update(dt);
                    case LEFT -> animWalkLeft.update(dt);
                    case RIGHT -> animWalkRight.update(dt);
                }
            }
            case DEAD -> animDeath.update(dt);
            case PLACE -> animIdleDown.update(dt);
        }
    }

    public void render(GraphicsContext g) {
        Image frame = null;

        switch (state) {
            case IDLE -> {
                switch (dir) {
                    case DOWN -> frame = animIdleDown.getCurrentFrame();
                    case UP -> frame = animIdleUp.getCurrentFrame();
                    case LEFT -> frame = animIdleLeft.getCurrentFrame();
                    case RIGHT -> frame = animIdleRight.getCurrentFrame();
                }
            }
            case WALK -> {
                switch (dir) {
                    case DOWN -> frame = animWalkDown.getCurrentFrame();
                    case UP -> frame = animWalkUp.getCurrentFrame();
                    case LEFT -> frame = animWalkLeft.getCurrentFrame();
                    case RIGHT -> frame = animWalkRight.getCurrentFrame();
                }
            }
            case DEAD -> frame = animDeath.getCurrentFrame();
            case PLACE -> frame = animIdleDown.getCurrentFrame();
        }

        if (frame != null) {
            // Render dengan Offset agar posisi kaki pas di tile
            g.drawImage(frame, x + offsetX, y + offsetY);
        }
    }

    // --- PENTING: TAMBAHAN GETTER UNTUK GAME RENDERER (DEBUG MODE) ---
    public double getX() { return x; }
    public double getY() { return y; }
}