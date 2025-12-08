package com.client.network; 

import java.util.ArrayList;
import java.util.List;

import com.client.App;
import com.client.ClientGameState;
import com.client.entities.VisualPlayer; 
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

        // Split Command dan Sisa Data
        String[] parts = packet.split(";", 2);
        String command = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "YOUR_ID" -> {
                try {
                    gameState.setMyPlayerId(Integer.parseInt(data));
                    Platform.runLater(() -> {
                        // Pastikan pendingRoomName ada isinya di App
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
            
            case "GAME_OVER" -> {
                 // Handle Game Over (Menang/Kalah)
                 // data bisa berisi "TIME_UP" atau info lain
                 System.out.println("GAME OVER: " + data);
            }

            case "ROOM_UPDATE" -> parseRoomUpdate(data);
            case "ROOM_LIST" -> parseRoomList(data);
            case "GAME_STARTED" -> Platform.runLater(App::startGame);
            case "MAP" -> parseMap(data);
            case "STATE" -> parseState(data);
        }
    }

    // [UPDATE PENTING] Parse State sekarang menangani WAKTU
    private void parseState(String data) {
        // Format dari Server: "Waktu;P1#P2#P3...|||"
        
        // 1. Bersihkan penanda akhir paket
        String cleanData = data.replace("|||", "");

        // 2. Pisahkan WAKTU dan DATA PLAYER
        // limit 2 berarti split jadi [Waktu, SisaString]
        String[] parts = cleanData.split(";", 2);

        // Parse Waktu (Bagian pertama)
        if (parts.length >= 1) {
            try {
                double time = Double.parseDouble(parts[0]);
                gameState.setGameTime(time); // Pastikan method ini ada di ClientGameState
            } catch (NumberFormatException e) {}
        }

        // Parse Player (Bagian kedua, jika ada)
        if (parts.length >= 2) {
            parsePlayers(parts[1]);
        } else {
            // Kalau tidak ada data player (misal kosong), clear players
            gameState.clearPlayers();
        }
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
                    if (existing.id == id) {
                        targetPlayer = existing; 
                        break;
                    }
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

    // --- Method Parsing Lain Tetap Sama ---

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
}