package com.client.network; 

import java.util.ArrayList;
import java.util.List;

import com.client.App;
import com.client.ClientGameState;
import com.client.entities.VisualPlayer; // Pastikan import ini ada
import com.client.render.SpriteLoader;
import com.client.ui.SceneManager; 

import javafx.application.Platform;
import javafx.scene.control.Alert;

public class PacketParser {

    private final ClientGameState gameState;
    private final SpriteLoader spriteLoader; 

    public PacketParser(ClientGameState gameState) {
        this.gameState = gameState;
        this.spriteLoader = new SpriteLoader(); 
    }

    public void parse(String packet) {
        if (packet == null || packet.isEmpty()) return;

        String[] parts = packet.split(";", 2);
        String command = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "YOUR_ID" -> {
                try {
                    gameState.setMyPlayerId(Integer.parseInt(data));
                    Platform.runLater(() -> {
                        SceneManager.toRoom(App.pendingRoomName);
                    });
                } catch (Exception e) {}
            }
            
            case "ERROR" -> {
                System.err.println("Server Error: " + data);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Join Failed");
                    alert.setHeaderText("Cannot Join Room");
                    alert.setContentText(data);
                    alert.showAndWait();
                });
            }

            case "ROOM_UPDATE" -> parseRoomUpdate(data);
            case "ROOM_LIST" -> parseRoomList(data);
            case "GAME_STARTED" -> Platform.runLater(App::startGame);
            case "MAP" -> parseMap(data);
            case "STATE" -> parseState(data);
        }
    }

    private void parseRoomUpdate(String data) {
        try {
            String[] section = data.split(";");
            int hostId = Integer.parseInt(section[0]);
            String[] idsStr = section[1].split(",");
            List<Integer> ids = new ArrayList<>();
            for(String s : idsStr) {
                if(!s.trim().isEmpty()) ids.add(Integer.parseInt(s.trim()));
            }
            gameState.updateRoomPlayers(hostId, ids);
        } catch (Exception e) {}
    }

    private void parseRoomList(String data) {
        String[] raw = data.split(",");
        List<String> rooms = new ArrayList<>();
        for (String r : raw) {
            if (!r.trim().isEmpty()) rooms.add(r.trim());
        }
        gameState.updateRooms(rooms);
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

    private void parseState(String data) {
        String[] sections = data.split("\\|", -1);
        if (sections.length >= 1) parsePlayers(sections[0]);
    }

    // --- BAGIAN UTAMA YANG DIPERBAIKI ---
    private void parsePlayers(String section) {
        if (section.isEmpty() || section.equals("NP")) { 
            gameState.clearPlayers(); 
            return;
        }

        // 1. Ambil daftar player yang SAAT INI ada di game
        List<VisualPlayer> currentPlayers = gameState.getPlayers();
        
        // 2. Siapkan list untuk frame berikutnya
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
                
                // 3. CARI: Apakah Player ID ini sudah ada di memori?
                VisualPlayer targetPlayer = null;
                for (VisualPlayer existing : currentPlayers) {
                    if (existing.id == id) {
                        targetPlayer = existing; // Ketemu! Pakai object lama
                        break;
                    }
                }

                if (targetPlayer != null) {
                    // UPDATE: Object lama di-update datanya (Timer animasi jalan terus)
                    targetPlayer.setNetworkState(x, y, VisualPlayer.State.valueOf(stateStr), VisualPlayer.Direction.valueOf(dirStr));
                } else {
                    // CREATE: Belum ada, bikin baru (Hanya terjadi 1x saat connect)
                    targetPlayer = new VisualPlayer(id, spriteLoader);
                    targetPlayer.setNetworkState(x, y, VisualPlayer.State.valueOf(stateStr), VisualPlayer.Direction.valueOf(dirStr));
                }

                // Masukkan ke list frame berikutnya
                nextFramePlayers.add(targetPlayer);

            } catch (Exception e) {}
        }
        
        // 4. Update GameState dengan list yang berisi Object LAMA (yang sudah diupdate)
        // Jadi timer animasi di dalam object tersebut tidak ter-reset.
        gameState.updatePlayers(nextFramePlayers);
    }
}