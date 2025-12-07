package com.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.client.entities.VisualBomb;
import com.client.entities.VisualExplosion;
import com.client.entities.VisualItem;
import com.client.entities.VisualPlayer;

public class ClientGameState {

    // --- DATA GAME ---
    private int[][] map;
    private boolean gameOver = false;
    private float gameTime = 0;

    // Entities
    private final List<VisualPlayer> players = new CopyOnWriteArrayList<>();
    private final List<VisualBomb> bombs = new CopyOnWriteArrayList<>();
    private final List<VisualExplosion> explosions = new CopyOnWriteArrayList<>();
    private final List<VisualItem> items = new CopyOnWriteArrayList<>();

    // --- DATA LOBBY (BARU) ---
    private final List<String> availableRooms = new ArrayList<>();
    private Consumer<List<String>> onRoomListUpdate; // Callback untuk UI

    // --- UPDATE VISUAL ---
    public void updateVisuals(double dt) {
        for (VisualPlayer p : players) p.update(dt);
        for (VisualBomb b : bombs) b.update(dt);
        for (VisualExplosion e : explosions) e.update(dt);
    }

    // --- LOBBY METHODS (BARU) ---
    public void setOnRoomListUpdate(Consumer<List<String>> callback) {
        this.onRoomListUpdate = callback;
    }

    public void updateRooms(List<String> newRooms) {
        synchronized (availableRooms) {
            availableRooms.clear();
            availableRooms.addAll(newRooms);
        }
        // Beritahu UI jika ada listener
        if (onRoomListUpdate != null) {
            onRoomListUpdate.accept(newRooms);
        }
    }
    
    public List<String> getAvailableRooms() {
        return availableRooms;
    }

    // --- SETTERS ---
    public void setMap(int[][] newMap) { this.map = newMap; }
    public void setGameTime(float time) { this.gameTime = time; }
    public void setGameOver(boolean status) { this.gameOver = status; }

    // --- UPDATERS ---
    public void updatePlayers(List<VisualPlayer> newPlayers) {
        players.clear(); players.addAll(newPlayers);
    }
    public void updateBombs(List<VisualBomb> newBombs) {
        bombs.clear(); bombs.addAll(newBombs);
    }
    public void updateExplosions(List<VisualExplosion> newExplosions) {
        explosions.clear(); explosions.addAll(newExplosions);
    }
    public void updateItems(List<VisualItem> newItems) {
        items.clear(); items.addAll(newItems);
    }
    public void clearPlayers() { players.clear(); }

    // --- GETTERS ---
    public int[][] getMap() { return map; }
    public List<VisualPlayer> getPlayers() { return players; }
    public List<VisualBomb> getBombs() { return bombs; }
    public List<VisualExplosion> getExplosions() { return explosions; }
    public List<VisualItem> getItems() { return items; }
    public float getGameTime() { return gameTime; }
    public boolean isGameOver() { return gameOver; }
}