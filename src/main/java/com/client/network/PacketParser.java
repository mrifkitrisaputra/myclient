package com.client.network; 

import java.util.ArrayList;
import java.util.List;

import com.client.ClientGameState;
import com.client.entities.VisualBomb;
import com.client.entities.VisualExplosion;
import com.client.entities.VisualItem;
import com.client.entities.VisualPlayer;
import com.client.render.SpriteLoader;

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
            case "MAP" -> parseMap(data);
            case "STATE" -> parseState(data);
            case "TIME" -> {
                try { gameState.setGameTime(Float.parseFloat(data)); } catch (Exception e) {}
            }
            case "GAMEOVER" -> gameState.setGameOver(true);
        }
    }

    private void parseMap(String data) {
        try {
            String[] tokens = data.split(";");
            int rows = Integer.parseInt(tokens[0]);
            int cols = Integer.parseInt(tokens[1]);
            String[] tiles = tokens[2].split(",");

            int[][] map = new int[rows][cols];
            for (int i = 0; i < tiles.length; i++) {
                int x = i % rows; 
                int y = i / rows;
                map[x][y] = Integer.parseInt(tiles[i]);
            }
            gameState.setMap(map);
            System.out.println("Map Received: " + rows + "x" + cols);
        } catch (Exception e) {
            System.err.println("Error parsing MAP: " + e.getMessage());
        }
    }

    private void parseState(String data) {
        String[] sections = data.split("\\|", -1);
        if (sections.length >= 1) parsePlayers(sections[0]);
        if (sections.length >= 2) parseBombs(sections[1]);
        if (sections.length >= 3) parseExplosions(sections[2]);
        if (sections.length >= 4) parseItems(sections[3]);
    }

    private void parsePlayers(String section) {
        if (section.isEmpty() || section.equals("NP")) { 
            gameState.clearPlayers();
            return;
        }

        List<VisualPlayer> currentPlayers = gameState.getPlayers();
        List<VisualPlayer> updatedPlayers = new ArrayList<>();
        
        String[] playersData = section.split("#");
        
        for (String pStr : playersData) {
            if (pStr.isEmpty()) continue;
            try {
                String[] pVal = pStr.split(",");
                // Format: id,x,y,state,dir
                int id = Integer.parseInt(pVal[0]); 
                double x = Double.parseDouble(pVal[1]);
                double y = Double.parseDouble(pVal[2]);
                String stateStr = pVal[3]; 
                String dirStr = pVal[4];   

                // --- LOGIC BARU: CARI EXISTING PLAYER ---
                VisualPlayer vp = null;
                for (VisualPlayer existing : currentPlayers) {
                    if (existing.id == id) {
                        vp = existing;
                        break;
                    }
                }

                // Jika tidak ada, buat baru
                if (vp == null) {
                    vp = new VisualPlayer(id, spriteLoader);
                }

                // Update data networknya
                vp.setNetworkState(x, y, VisualPlayer.State.valueOf(stateStr), VisualPlayer.Direction.valueOf(dirStr));
                
                updatedPlayers.add(vp);

            } catch (Exception e) {
                System.err.println("Error parsing player: " + pStr);
            }
        }
        // Ganti list lama dengan list yang sudah diupdate
        gameState.updatePlayers(updatedPlayers);
    }

    private void parseBombs(String section) {
        if (section.isEmpty()) { gameState.updateBombs(new ArrayList<>()); return; }
        List<VisualBomb> newBombs = new ArrayList<>();
        String[] bombsData = section.split("#");
        for (String bStr : bombsData) {
            if (bStr.isEmpty()) continue;
            try {
                String[] val = bStr.split(",");
                newBombs.add(new VisualBomb(Integer.parseInt(val[0]), Integer.parseInt(val[1])));
            } catch (Exception e) {}
        }
        gameState.updateBombs(newBombs);
    }

    private void parseExplosions(String section) {
        if (section.isEmpty()) { gameState.updateExplosions(new ArrayList<>()); return; }
        List<VisualExplosion> newExplosions = new ArrayList<>();
        String[] expData = section.split("#");
        for (String eStr : expData) {
            if (eStr.isEmpty()) continue;
            try {
                String[] mainParts = eStr.split(",", 3);
                int cx = Integer.parseInt(mainParts[0]);
                int cy = Integer.parseInt(mainParts[1]);
                List<VisualExplosion.ExplosionPart> parts = new ArrayList<>();
                if (mainParts.length > 2 && !mainParts[2].isEmpty()) {
                    String[] partsStr = mainParts[2].split("\\+");
                    for (String part : partsStr) {
                        String[] pVal = part.split(":"); 
                        parts.add(new VisualExplosion.ExplosionPart(Integer.parseInt(pVal[0]), Integer.parseInt(pVal[1]), Boolean.parseBoolean(pVal[2])));
                    }
                }
                newExplosions.add(new VisualExplosion(cx, cy, parts));
            } catch (Exception e) {}
        }
        gameState.updateExplosions(newExplosions);
    }
    
    private void parseItems(String section) {
        if (section.isEmpty()) { gameState.updateItems(new ArrayList<>()); return; }
        List<VisualItem> newItems = new ArrayList<>();
        String[] itemsData = section.split("#");
        for (String iStr : itemsData) {
            if (iStr.isEmpty()) continue;
            try {
                String[] val = iStr.split(",");
                newItems.add(new VisualItem(Integer.parseInt(val[0]), Integer.parseInt(val[1]), VisualItem.Type.valueOf(val[2])));
            } catch (Exception e) {}
        }
        gameState.updateItems(newItems);
    }
}