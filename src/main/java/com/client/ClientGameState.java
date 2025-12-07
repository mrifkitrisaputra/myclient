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
    
    // --- IDENTITY (Siapa saya & Host) ---
    private int myPlayerId = -1;
    private int hostPlayerId = -1;
    private final List<Integer> roomPlayerIds = new ArrayList<>(); 

    // Entities
    private final List<VisualPlayer> players = new CopyOnWriteArrayList<>();
    private final List<VisualBomb> bombs = new CopyOnWriteArrayList<>();
    private final List<VisualExplosion> explosions = new CopyOnWriteArrayList<>();
    private final List<VisualItem> items = new CopyOnWriteArrayList<>();

    // --- LOBBY DATA ---
    private final List<String> availableRooms = new ArrayList<>();
    
    // --- CALLBACKS ---
    private Consumer<List<String>> onRoomListUpdate; 
    private Consumer<Void> onRoomStateUpdate; 

    // --- UPDATE VISUAL ---
    public void updateVisuals(double dt) {
        for (VisualPlayer p : players) p.update(dt);
        for (VisualBomb b : bombs) b.update(dt);
        for (VisualExplosion e : explosions) e.update(dt);
    }

    // --- IDENTITY METHODS ---
    public void setMyPlayerId(int id) { this.myPlayerId = id; }
    public int getMyPlayerId() { return myPlayerId; }
    
    public void setHostPlayerId(int id) { this.hostPlayerId = id; }
    public int getHostPlayerId() { return hostPlayerId; }
    
    public boolean amIHost() { return myPlayerId == hostPlayerId; }

    public void updateRoomPlayers(int hostId, List<Integer> ids) {
        this.hostPlayerId = hostId;
        synchronized (roomPlayerIds) {
            roomPlayerIds.clear();
            roomPlayerIds.addAll(ids);
        }
        if (onRoomStateUpdate != null) onRoomStateUpdate.accept(null);
    }
    
    public List<Integer> getRoomPlayerIds() { return roomPlayerIds; }
    
    public void setOnRoomStateUpdate(Consumer<Void> callback) {
        this.onRoomStateUpdate = callback;
    }

    // --- LOBBY METHODS ---
    public void setOnRoomListUpdate(Consumer<List<String>> callback) {
        this.onRoomListUpdate = callback;
    }

    public void updateRooms(List<String> newRooms) {
        synchronized (availableRooms) {
            availableRooms.clear();
            availableRooms.addAll(newRooms);
        }
        if (onRoomListUpdate != null) onRoomListUpdate.accept(newRooms);
    }
    
    public List<String> getAvailableRooms() { return availableRooms; }

    // --- SETTERS ---
    public void setMap(int[][] newMap) { this.map = newMap; }
    public void setGameTime(float time) { this.gameTime = time; }
    public void setGameOver(boolean status) { this.gameOver = status; }

    // --- UPDATERS (Entity) ---
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

    // --- GETTERS (INI YANG TADI HILANG) ---
    public int[][] getMap() { return map; }
    public List<VisualPlayer> getPlayers() { return players; }
    public List<VisualBomb> getBombs() { return bombs; }
    public List<VisualExplosion> getExplosions() { return explosions; }
    public List<VisualItem> getItems() { return items; }
    public float getGameTime() { return gameTime; }
    public boolean isGameOver() { return gameOver; }
}