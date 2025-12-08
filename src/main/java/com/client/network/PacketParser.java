package com.client.network;

import com.client.App;
import com.client.ClientGameState;
import com.client.entities.VisualPlayer;
import com.client.render.SpriteLoader;
import com.client.ui.SceneManager;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.ArrayList;
import java.util.List;

public class PacketParser {

    private final ClientGameState gameState;
    private final SpriteLoader spriteLoader;

    public PacketParser(ClientGameState gameState) {
        this.gameState = gameState;
        this.spriteLoader = new SpriteLoader();
    }

    public void parse(String packet) {
        if (packet == null || packet.isEmpty()) return;

        // Pisahkan command utama dengan datanya
        String[] parts = packet.split(";", 2);
        String command = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        switch (command) {
            // ================= GAMEPLAY EVENTS =================

            case "BOMB_PLACED" -> {
                // Server kirim: BOMB_PLACED;x,y;ownerId
                try {
                    String[] info = data.split(";");
                    String[] coords = info[0].split(",");
                    int bx = Integer.parseInt(coords[0]);
                    int by = Integer.parseInt(coords[1]);
                    
                    // Masukkan ke State agar dirender
                    gameState.addBomb(bx, by);
                    System.out.println("[CLIENT] Bomb spawned at " + bx + "," + by);
                } catch (Exception e) {
                    System.err.println("Error parsing BOMB_PLACED: " + e.getMessage());
                }
            }

            case "EXPLOSION" -> {
                // Server kirim: EXPLOSION;centerX,centerY;part1;part2...
                try {
                    String[] sections = data.split(";");
                    
                    // 1. Ambil Pusat Ledakan & Hapus Bom Visualnya
                    String[] center = sections[0].split(",");
                    int cx = Integer.parseInt(center[0]);
                    int cy = Integer.parseInt(center[1]);
                    
                    gameState.removeBombAt(cx, cy);
                    gameState.addExplosion(cx, cy, false);

                    // 2. Loop sisa part ledakan (api)
                    for (int i = 1; i < sections.length; i++) {
                        String[] p = sections[i].split(",");
                        int px = Integer.parseInt(p[0]);
                        int py = Integer.parseInt(p[1]);
                        boolean vert = Boolean.parseBoolean(p[2]);
                        gameState.addExplosion(px, py, vert);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            case "BREAK_TILE" -> {
                // Server kirim: BREAK_TILE;x,y
                try {
                    String[] coords = data.split(",");
                    int tx = Integer.parseInt(coords[0]);
                    int ty = Integer.parseInt(coords[1]);
                    gameState.breakTile(tx, ty);
                } catch (Exception e) {}
            }

            case "SPAWN_ITEM" -> {
                // Server kirim: SPAWN_ITEM;x,y,TYPE
                try {
                    String[] info = data.split(",");
                    int ix = Integer.parseInt(info[0]);
                    int iy = Integer.parseInt(info[1]);
                    String type = info[2];
                    gameState.spawnItem(ix, iy, type);
                } catch (Exception e) {}
            }

            case "ITEM_PICKED" -> {
                // Server kirim: ITEM_PICKED;pid,x,y,TYPE
                try {
                    String[] info = data.split(",");
                    // info[0] adalah playerID, kita butuh koordinat utk hapus visual
                    int ix = Integer.parseInt(info[1]);
                    int iy = Integer.parseInt(info[2]);
                    gameState.removeItemAt(ix, iy);
                } catch (Exception e) {}
            }

            // ================= DEATH & GAME OVER =================

            case "PLAYER_DIED" -> {
                // Server kirim: PLAYER_DIED;id
                try {
                    int deadId = Integer.parseInt(data);
                    
                    // Cek apakah ID yang mati adalah ID saya sendiri
                    if (deadId == gameState.getMyPlayerId()) {
                        System.out.println("[CLIENT] You Died!");
                        // Munculkan Popup Kalah (false) di Thread JavaFX
                        Platform.runLater(() -> SceneManager.showGameOverPopup(false));
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing PLAYER_DIED");
                }
            }

            case "GAME_OVER" -> {
                // Server kirim: GAME_OVER;WINNER_ID  atau  GAME_OVER;TIME_UP
                String result = data;
                boolean isWin = false;
                
                if (!result.equals("TIME_UP")) {
                    try {
                        int winnerId = Integer.parseInt(result);
                        // Cek apakah saya pemenangnya
                        isWin = (winnerId == gameState.getMyPlayerId());
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing Winner ID");
                    }
                }
                
                System.out.println("[CLIENT] Game Over. Win? " + isWin);
                
                final boolean winStatus = isWin;
                // Munculkan Popup Menang/Kalah di Thread JavaFX
                Platform.runLater(() -> SceneManager.showGameOverPopup(winStatus));
            }

            // ================= STATE & SYNC =================

            case "STATE" -> parseState(data); // Posisi Player & Waktu
            case "MAP" -> parseMap(data);     // Load awal map

            // ================= LOBBY & SYSTEM =================

            case "YOUR_ID" -> {
                try {
                    gameState.setMyPlayerId(Integer.parseInt(data));
                    Platform.runLater(() -> SceneManager.toRoom(App.pendingRoomName));
                } catch (Exception e) {}
            }
            case "GAME_STARTED" -> Platform.runLater(App::startGame);
            
            case "ERROR" -> Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText(data);
                alert.showAndWait();
            });
            case "ROOM_UPDATE" -> parseRoomUpdate(data);
            case "ROOM_LIST" -> parseRoomList(data);
            case "REMATCH_UPDATE" -> {
    // Server kirim: REMATCH_UPDATE;votes;total
    try {
        String[] info = data.split(";");
        int current = Integer.parseInt(info[0]);
        int total = Integer.parseInt(info[1]);
        
        // Update UI via SceneManager
        SceneManager.updateRematchCount(current, total);
        
    } catch (Exception e) {
        System.err.println("Error parsing REMATCH_UPDATE");
    }
}
        }
    }

    private void parseState(String data) {
        String cleanData = data.replace("|||", "");
        String[] parts = cleanData.split(";", 2);
        
        // Parse Waktu
        if (parts.length >= 1) {
            try {
                gameState.setGameTime(Double.parseDouble(parts[0]));
            } catch (Exception e) {}
        }
        
        // Parse Players
        if (parts.length >= 2) parsePlayers(parts[1]);
        else gameState.clearPlayers();
    }

    private void parsePlayers(String section) {
        if (section.isEmpty() || section.equals("NP")) { 
            gameState.clearPlayers(); 
            return; 
        }

        List<VisualPlayer> currentPlayers = gameState.getPlayers();
        List<VisualPlayer> nextFramePlayers = new ArrayList<>();
        
        String[] playersData = section.split("#");
        for (String pStr : playersData) {
            try {
                String[] pVal = pStr.split(",");
                int id = Integer.parseInt(pVal[0]); 
                double x = Double.parseDouble(pVal[1]);
                double y = Double.parseDouble(pVal[2]);
                String stateStr = pVal[3]; 
                String dirStr = pVal[4];   
                
                VisualPlayer targetPlayer = null;
                for (VisualPlayer existing : currentPlayers) {
                    if (existing.id == id) { targetPlayer = existing; break; }
                }

                if (targetPlayer != null) {
                    targetPlayer.setNetworkState(x, y, VisualPlayer.State.valueOf(stateStr), VisualPlayer.Direction.valueOf(dirStr));
                } else {
                    targetPlayer = new VisualPlayer(id, spriteLoader);
                    targetPlayer.setNetworkState(x, y, VisualPlayer.State.valueOf(stateStr), VisualPlayer.Direction.valueOf(dirStr));
                }
                nextFramePlayers.add(targetPlayer);

            } catch (Exception e) {}
        }
        gameState.updatePlayers(nextFramePlayers);
    }

    private void parseMap(String data) {
        try {
            String[] tokens = data.split(";");
            int rows = Integer.parseInt(tokens[0]);
            int cols = Integer.parseInt(tokens[1]);
            String[] tiles = tokens[2].split(",");
            int[][] map = new int[rows][cols];
            for (int i = 0; i < tiles.length; i++) {
                int x = i % rows; int y = i / rows;
                map[x][y] = Integer.parseInt(tiles[i]);
            }
            gameState.setMap(map);
        } catch (Exception e) {}
    }

    private void parseRoomUpdate(String data) {
        try {
            String[] section = data.split(";");
            int hostId = Integer.parseInt(section[0]);
            String[] idsStr = section[1].split(",");
            List<Integer> ids = new ArrayList<>();
            for(String s : idsStr) if(!s.trim().isEmpty()) ids.add(Integer.parseInt(s.trim()));
            gameState.updateRoomPlayers(hostId, ids);
        } catch (Exception e) {}
    }

    private void parseRoomList(String data) {
        String[] raw = data.split(",");
        List<String> rooms = new ArrayList<>();
        for (String r : raw) if (!r.trim().isEmpty()) rooms.add(r.trim());
        gameState.updateRooms(rooms);
    }
}