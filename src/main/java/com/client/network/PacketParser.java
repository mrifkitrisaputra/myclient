package com.client.network; 

import java.util.ArrayList;
import java.util.List;

import com.client.App;
import com.client.ClientGameState;
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
            // --- SERVER MENERIMA REQUEST MASUK ROOM ---
            case "YOUR_ID" -> {
                gameState.setMyPlayerId(Integer.parseInt(data));
                
                // Kunci Logika: Server sudah setuju, sekarang baru kita pindah visual ke Room
                Platform.runLater(() -> {
                    SceneManager.toRoom(App.pendingRoomName);
                });
            }
            
            // --- SERVER MENOLAK (PASSWORD SALAH DLL) ---
            case "ERROR" -> {
                System.err.println("Server Error: " + data);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Join Failed");
                    alert.setHeaderText("Cannot Join Room");
                    alert.setContentText(data); // "Wrong Password" atau "Room Name Taken"
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

    private void parsePlayers(String section) {
        if (section.isEmpty() || section.equals("NP")) { 
            gameState.clearPlayers(); return;
        }
        List<com.client.entities.VisualPlayer> updatedPlayers = new ArrayList<>();
        String[] playersData = section.split("#");
        for (String pStr : playersData) {
            try {
                String[] pVal = pStr.split(",");
                int id = Integer.parseInt(pVal[0]); 
                double x = Double.parseDouble(pVal[1]);
                double y = Double.parseDouble(pVal[2]);
                String stateStr = pVal[3]; 
                String dirStr = pVal[4];   
                
                var vp = new com.client.entities.VisualPlayer(id, spriteLoader);
                vp.setNetworkState(x, y, com.client.entities.VisualPlayer.State.valueOf(stateStr), com.client.entities.VisualPlayer.Direction.valueOf(dirStr));
                updatedPlayers.add(vp);
            } catch (Exception e) {}
        }
        gameState.updatePlayers(updatedPlayers);
    }
}