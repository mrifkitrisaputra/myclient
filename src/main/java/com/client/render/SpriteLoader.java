package com.client.render;

import java.util.HashMap;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class SpriteLoader {

    private final HashMap<String, Image> cache = new HashMap<>();

    private static final String PLAYER_PATH = "/com/client/assets/player/";
    private final int TARGET_SIZE = 32;

    public Image get(String name) {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }

        String fullPath = PLAYER_PATH + name + ".png";

        try {
            var stream = getClass().getResourceAsStream(fullPath);
            if (stream == null) {
                System.err.println("Sprite missing: " + fullPath);
                return null;
            }

            Image raw = new Image(stream);

            // Resize otomatis ke 32×32
            Image resized = resizeTo32(raw);

            cache.put(name, resized);
            return resized;

        } catch (Exception e) {
            System.err.println("Error loading: " + e.getMessage());
            return null;
        }
    }

    private Image resizeTo32(Image raw) {
        WritableImage out = new WritableImage(TARGET_SIZE, TARGET_SIZE);

        PixelWriter pw = out.getPixelWriter();
        PixelReader pr = raw.getPixelReader();

        int w = (int) raw.getWidth();
        int h = (int) raw.getHeight();

        for (int y = 0; y < TARGET_SIZE; y++) {
            for (int x = 0; x < TARGET_SIZE; x++) {
                int srcX = x * w / TARGET_SIZE;
                int srcY = y * h / TARGET_SIZE;

                pw.setArgb(x, y, pr.getArgb(srcX, srcY));
            }
        }

        return out;
    }
}
