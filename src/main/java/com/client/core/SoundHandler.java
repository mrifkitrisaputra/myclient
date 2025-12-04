package com.client.core;

import java.net.URL;
import java.util.Random;

import javafx.scene.media.AudioClip;

public class SoundHandler {
    private static AudioClip[] bombDrops;
    private static AudioClip[] explosions;
    private static final Random rand = new Random();
    private static boolean isInitialized = false;

    // Load semua sound asset ke memory
    public static void init() {
        if (isInitialized) return;

        // NOTE: Path resource tetap mengarah ke 'com/client' karena Anda copy folder resources 100%
        // Jika Anda merename folder resources jadi 'myclient', ubah string di bawah ini.
        
        // Load Bomb Drop Sounds (BMDROP1 - BMDROP3)
        bombDrops = new AudioClip[3];
        loadSound(bombDrops, 0, "/com/client/assets/bomb/BMDROP1.wav");
        loadSound(bombDrops, 1, "/com/client/assets/bomb/BMDROP2.wav");
        loadSound(bombDrops, 2, "/com/client/assets/bomb/BMDROP3.wav");

        // Load Explosion Sounds (EXPLO1, EXPLODE2 - EXPLODE4)
        explosions = new AudioClip[4];
        loadSound(explosions, 0, "/com/client/assets/explosion/EXPLO1.wav");
        loadSound(explosions, 1, "/com/client/assets/explosion/EXPLODE2.wav");
        loadSound(explosions, 2, "/com/client/assets/explosion/EXPLODE3.wav");
        loadSound(explosions, 3, "/com/client/assets/explosion/EXPLODE4.wav");

        isInitialized = true;
    }

    private static void loadSound(AudioClip[] array, int index, String path) {
        try {
            URL resource = SoundHandler.class.getResource(path);
            if (resource != null) {
                array[index] = new AudioClip(resource.toExternalForm());
            } else {
                System.err.println("Sound not found: " + path);
            }
        } catch (Exception e) {
            System.err.println("Error loading sound: " + path);
            e.printStackTrace();
        }
    }

    public static void playBombPlace() {
        if (!isInitialized || bombDrops == null) return;
        int idx = rand.nextInt(bombDrops.length);
        if (bombDrops[idx] != null) {
            bombDrops[idx].play();
        }
    }

    public static void playExplosion() {
        if (!isInitialized || explosions == null) return;
        int idx = rand.nextInt(explosions.length);
        if (explosions[idx] != null) {
            explosions[idx].play();
        }
    }
}