package com.client.render;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public class CameraScaler {

    private double scale = 1.8;
    private final double MIN_SCALE = 0.5;
    private final double MAX_SCALE = 4.0;
    private final double STEP = 0.1;

    private final Canvas canvas;

    public CameraScaler(Canvas canvas) {
        this.canvas = canvas;
    }

    public void addScale(double s) {
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale + s));
    }

    public void applyTransform(GraphicsContext g) {
        g.scale(scale, scale);
    }

    public double getScale() { return scale; }
    
    // Setter jika perlu reset dari luar
    public void setScale(double s) {
        this.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
    }
}