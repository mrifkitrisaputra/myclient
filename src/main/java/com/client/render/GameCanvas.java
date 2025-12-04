package com.client.render;

import com.client.ClientGameState;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;

public class GameCanvas extends Canvas {
    
    // Referensi ke "Otak Semu" Client
    private final ClientGameState gameState;
    private final GameRenderer renderer;
    private final CameraScaler scaler;

    // Loop Render
    private final AnimationTimer loop = new AnimationTimer() {
        long last = System.nanoTime();

        @Override
        public void handle(long now) {
            double dt = (now - last) / 1e9;
            last = now;
            
            // 1. Update animasi visual (bukan logika game)
            if (gameState != null) {
                gameState.updateVisuals(dt);
            }

            // 2. Render
            renderer.render();
        }
    };

    public GameCanvas(ClientGameState gameState) {
        this.gameState = gameState;
        this.scaler = new CameraScaler(this);
        
        // Renderer mengambil data dari gameState
        this.renderer = new GameRenderer(this, gameState, scaler);

        // Bind ukuran canvas ke scene nanti
        widthProperty().addListener(evt -> renderer.render());
        heightProperty().addListener(evt -> renderer.render());
    }

    public void start() {
        loop.start();
    }

    public void stop() {
        loop.stop();
    }

    // Helper untuk Input Zoom (Optional)
    public void handleInput(KeyCode code) {
        switch (code) {
            case EQUALS, ADD -> scaler.addScale(0.1);
            case MINUS, SUBTRACT -> scaler.addScale(-0.1);
        }
    }

    public double getRenderScale() { return scaler.getScale(); }
}