package com.client.render;

import java.util.HashMap;

import javafx.scene.image.Image;

public class SpriteLoader {

    private final HashMap<String, Image> cache = new HashMap<>();

    // Path folder aset (Sesuai tree resources kamu: /com/client/assets/player/)
    private static final String PLAYER_PATH = "/com/client/assets/player/";

    /**
     * Method ini dipanggil oleh VisualPlayer: loader.get("sP2Down_0")
     */
    public Image get(String name) {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }

        // Susun path lengkap: /com/client/assets/player/sP2Down_0.png
        String fullPath = PLAYER_PATH + name + ".png";
        
        try {
            // Validasi apakah file ada sebelum di-load
            var stream = getClass().getResourceAsStream(fullPath);
            if (stream == null) {
                System.err.println("Sprite missing (File Not Found): " + fullPath);
                return null; // Return null agar game tidak crash, nanti VisualPlayer handle fallback
            }

            Image img = new Image(stream);
            if (img.isError()) {
                System.err.println("Error loading sprite image: " + name);
                return null;
            }
            cache.put(name, img);
            return img;
        } catch (Exception e) {
            System.err.println("Exception loading sprite: " + e.getMessage());
            return null;
        }
    }

    public Image load(String path) {
        if (cache.containsKey(path)) return cache.get(path);
        
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            System.err.println("Image not found: " + path);
            return null;
        }
        
        Image img = new Image(stream);
        cache.put(path, img);
        return img;
    }
}