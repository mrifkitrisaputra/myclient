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

    // ... (Kode variabel lama tetap sama) ...
    private int[][] map;
    private boolean gameOver = false;
    private double gameTime = 0;
    
    // --- TAMBAHAN BARU: DEBUG MODE ---
    private boolean debugMode = false; // Default mati

    // ... (Kode identity & list entities tetap sama) ...
    private int myPlayerId = -1;
    private int hostPlayerId = -1;
    private final List<Integer> roomPlayerIds = new ArrayList<>(); 
    final List<VisualPlayer> players = new CopyOnWriteArrayList<>();
    private final List<VisualBomb> bombs = new CopyOnWriteArrayList<>();
    private final List<VisualExplosion> explosions = new CopyOnWriteArrayList<>();
    private final List<VisualItem> items = new CopyOnWriteArrayList<>();
    private final List<String> availableRooms = new ArrayList<>();
    private Consumer<List<String>> onRoomListUpdate; 
    private Consumer<Void> onRoomStateUpdate; 

    // --- METHODS DEBUG BARU ---
    public boolean isDebugMode() { return debugMode; }
    public void toggleDebug() { this.debugMode = !this.debugMode; }

    // ... (Sisa method updateVisuals lama, saya update isinya sedikit untuk bersihkan ledakan) ...
    
    public void updateVisuals(double dt) {
        for (VisualPlayer p : players) p.update(dt);
        for (VisualBomb b : bombs) b.update(dt);
        
        // Update Ledakan
        for (VisualExplosion e : explosions) e.update(dt);
        
        // [TAMBAHAN] Hapus ledakan yang animasinya sudah selesai dari list
        // Ini penting biar ledakan gak numpuk di memory dan layar
        explosions.removeIf(VisualExplosion::isFinished);
    }

    // ==================================================================
    // [BAGIAN BARU] LOGIC TAMBAHAN UNTUK PACKET PARSER
    // Method-method ini dibutuhkan agar PacketParser bisa menambah/hapus
    // entity satu per satu (bukan overwrite semua list).
    // ==================================================================

    // 1. Logic Bom
    public void addBomb(int x, int y) {
        // Cek duplicate biar ga numpuk visual di tile yang sama
        for(VisualBomb b : bombs) {
            if(b.tileX == x && b.tileY == y) return;
        }
        bombs.add(new VisualBomb(x, y));
    }

    public void removeBombAt(int x, int y) {
        bombs.removeIf(b -> b.tileX == x && b.tileY == y);
    }

    // 2. Logic Ledakan
    public void addExplosion(int x, int y, boolean vertical) {
        explosions.add(new VisualExplosion(x, y, vertical));
    }

    // 3. Logic Item
    public void spawnItem(int x, int y, String typeStr) {
        try {
            VisualItem.Type type = VisualItem.Type.valueOf(typeStr);
            items.add(new VisualItem(x, y, type));
        } catch (Exception e) {
            System.err.println("Unknown Item Type: " + typeStr);
        }
    }

    public void removeItemAt(int x, int y) {
        items.removeIf(i -> i.tileX == x && i.tileY == y);
    }

    // 4. Logic Map (Hancurkan Tembok)
    public void breakTile(int x, int y) {
        if (map != null && x >= 0 && x < map.length && y >= 0 && y < map[0].length) {
            map[x][y] = 0; // Ubah tile jadi 0 (Lantai)
        }
    }

    // ==================================================================
    // [AKHIR BAGIAN BARU] SISA KODE LAMA DI BAWAH TETAP ADA
    // ==================================================================

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
    public void setOnRoomStateUpdate(Consumer<Void> callback) { this.onRoomStateUpdate = callback; }

    public void setOnRoomListUpdate(Consumer<List<String>> callback) { this.onRoomListUpdate = callback; }
    public void updateRooms(List<String> newRooms) {
        synchronized (availableRooms) {
            availableRooms.clear();
            availableRooms.addAll(newRooms);
        }
        if (onRoomListUpdate != null) onRoomListUpdate.accept(newRooms);
    }
    public List<String> getAvailableRooms() { return availableRooms; }

    public void setMap(int[][] newMap) { this.map = newMap; }
    public void setGameTime(float time) { this.gameTime = time; }
    public void setGameOver(boolean status) { this.gameOver = status; }

    public void updatePlayers(List<VisualPlayer> newPlayers) { players.clear(); players.addAll(newPlayers); }
    public void updateBombs(List<VisualBomb> newBombs) { bombs.clear(); bombs.addAll(newBombs); }
    public void updateExplosions(List<VisualExplosion> newExplosions) { explosions.clear(); explosions.addAll(newExplosions); }
    public void updateItems(List<VisualItem> newItems) { items.clear(); items.addAll(newItems); }
    public void clearPlayers() { players.clear(); }

    public int[][] getMap() { return map; }
    public List<VisualPlayer> getPlayers() { return players; }
    public List<VisualBomb> getBombs() { return bombs; }
    public List<VisualExplosion> getExplosions() { return explosions; }
    public List<VisualItem> getItems() { return items; }
    
    public void setGameTime(double time) {
        this.gameTime = time;
    }

    public double getGameTime() {
        return gameTime;
    }
    
    public boolean isGameOver() { return gameOver; }
}