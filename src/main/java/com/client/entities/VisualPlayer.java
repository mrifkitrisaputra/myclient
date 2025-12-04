package com.client.entities;

import com.client.render.SpriteAnimation;
import com.client.render.SpriteLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class VisualPlayer {

    // Posisi (Server Authoritative)
    public double x;
    public double y;
    
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

    private final int offsetX = 0; // Sesuaikan offset sprite jika perlu
    private final int offsetY = -16; // Biasanya sprite char agak naik dari tile

    public VisualPlayer(SpriteLoader loader) {
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

    // Dipanggil setiap kali terima paket dari Server
    public void setNetworkState(double x, double y, State state, Direction dir) {
        this.x = x;
        this.y = y;
        this.state = state;
        this.dir = dir;
    }

    // Helper load frames
    private Image[] loadFrames(SpriteLoader loader, String base, int count) {
        Image[] temp = new Image[count];
        int idx = 0;
        for (int i = 0; i < count; i++) {
            Image img = loader.get(base + i);
            if (img != null) temp[idx++] = img;
        }
        if (idx == 0) return new Image[]{ new javafx.scene.image.WritableImage(1, 1) };
        Image[] frames = new Image[idx];
        System.arraycopy(temp, 0, frames, 0, idx);
        return frames;
    }

    // Update hanya memajukan frame animasi
    public void update(double dt) {
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
            case PLACE -> animIdleDown.update(dt); // Fallback
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
            // Gambar di posisi X,Y yang dikirim server
            g.drawImage(frame, x + offsetX, y + offsetY);
        }
    }
}