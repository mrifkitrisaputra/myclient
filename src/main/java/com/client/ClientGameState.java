package com.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.client.entities.VisualBomb;
import com.client.entities.VisualExplosion;
import com.client.entities.VisualItem;
import com.client.entities.VisualPlayer;

/**
 * Class ini menyimpan SNAPSHOT dari kondisi game saat ini.
 * Data di sini diperbarui oleh Network (saat terima paket),
 * dan dibaca oleh Renderer (saat menggambar).
 */
public class ClientGameState {

    // --- DATA GAME ---
    private int[][] map; // 0=Ground, 1=Unbreak, 2=Breakable
    private boolean gameOver = false;
    private float gameTime = 0;

    // Thread-safe list untuk menghindari crash saat render & update bersamaan
    private final List<VisualPlayer> players = new CopyOnWriteArrayList<>();
    private final List<VisualBomb> bombs = new CopyOnWriteArrayList<>();
    private final List<VisualExplosion> explosions = new CopyOnWriteArrayList<>();
    private final List<VisualItem> items = new CopyOnWriteArrayList<>();

    // --- UPDATE VISUAL (Animasi) ---
    public void updateVisuals(double dt) {
        for (VisualPlayer p : players) p.update(dt);
        for (VisualBomb b : bombs) b.update(dt);
        for (VisualExplosion e : explosions) e.update(dt);
    }

    // --- SETTERS (Dari Network) ---
    public void setMap(int[][] newMap) {
        this.map = newMap;
    }

    public void setGameTime(float time) {
        this.gameTime = time;
    }

    public void setGameOver(boolean status) {
        this.gameOver = status;
    }

    // --- UPDATERS (Method yang menyebabkan error sebelumnya) ---
    
    public void updatePlayers(List<VisualPlayer> newPlayers) {
        players.clear();
        players.addAll(newPlayers);
    }

    public void updateBombs(List<VisualBomb> newBombs) {
        bombs.clear();
        bombs.addAll(newBombs);
    }

    public void updateExplosions(List<VisualExplosion> newExplosions) {
        explosions.clear();
        explosions.addAll(newExplosions);
    }

    public void updateItems(List<VisualItem> newItems) {
        items.clear();
        items.addAll(newItems);
    }
    
    public void clearPlayers() {
        players.clear();
    }

    // --- GETTERS (Untuk Renderer) ---
    public int[][] getMap() { return map; }
    public List<VisualPlayer> getPlayers() { return players; }
    public List<VisualBomb> getBombs() { return bombs; }
    public List<VisualExplosion> getExplosions() { return explosions; }
    public List<VisualItem> getItems() { return items; }
    public float getGameTime() { return gameTime; }
    public boolean isGameOver() { return gameOver; }
}