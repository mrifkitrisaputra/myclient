package com.client.render;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

public class SpriteAnimation {

    private final Image[] frames;
    private int index = 0;
    private double timer = 0;

    private double frameTime = 0.12;   // Durasi setiap frame
    private boolean loop = true;
    private boolean finished = false;

    public SpriteAnimation(Image... frames) {
        if (frames == null || frames.length == 0) {
            // Prevent crash → placeholder 1px
            this.frames = new Image[]{ new WritableImage(1, 1) };
        } else {
            this.frames = frames;
        }
    }

    public Image update(double dt) {
        if (!loop && finished) {
            return frames[frames.length - 1];
        }

        timer += dt;

        while (timer >= frameTime) {
            timer -= frameTime;
            if (loop) {
                index = (index + 1) % frames.length;
            } else {
                if (index < frames.length - 1) {
                    index++;
                } else {
                    finished = true;
                }
            }
        }
        return frames[index];
    }

    public void reset() {
        index = 0;
        timer = 0;
        finished = false;
    }

    public Image getCurrentFrame() {
        return frames[index];
    }

    public void setLoop(boolean loop) { this.loop = loop; }
    public boolean isFinished() { return finished; }
}