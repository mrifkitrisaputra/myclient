package com.client.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleTestServer {

    private static final int PORT = 5000;
    private static final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private static final List<ClientHandler> lobbyClients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("SERVER STARTED ON PORT " + PORT + " [DEBUG MODE ON]");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler client = new ClientHandler(clientSocket);
                lobbyClients.add(client);
                new Thread(client).start();
                System.out.println("New Client connected: " + clientSocket.getInetAddress());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastRoomList() {
        if (lobbyClients.isEmpty()) return;
        StringBuilder sb = new StringBuilder("ROOM_LIST;");
        for (Room r : rooms.values()) {
            String type = r.isPrivate ? "(Private)" : "(Public)";
            sb.append(r.name).append(":").append(type).append(",");
        }
        String msg = sb.toString();
        for (ClientHandler c : lobbyClients) c.send(msg);
    }

    // ===================== CLASS ROOM =======================
    static class Room implements Runnable {
        String name;
        String password = ""; 
        boolean isPrivate = false;
        boolean gameStarted = false;
        boolean isRunning = true;

        List<PlayerState> players = new CopyOnWriteArrayList<>();
        List<ClientHandler> clients = new CopyOnWriteArrayList<>();
        ClientHandler host;

        private int[][] mapData; 
        private CollisionHandler collisionHandler;

        public Room(String name, String password) {
            this.name = name;
            if (password != null && !password.isEmpty()) {
                this.password = password;
                this.isPrivate = true;
            }
        }

        public synchronized int addPlayer(ClientHandler client) {
            clients.add(client);
            int id = clients.size() - 1;
            client.playerId = id; 
            
            double spawnX = (1 + id) * 32; 
            double spawnY = 32; 
            
            players.add(new PlayerState(id, spawnX, spawnY));
            
            if (clients.size() == 1) host = client;
            broadcastRoomInfo();
            return id;
        }

        public synchronized void removePlayer(ClientHandler client, int playerId) {
            clients.remove(client);
            if (client == host && !clients.isEmpty()) {
                host = clients.get(0);
                System.out.println("[ROOM " + name + "] Host migrated to ID " + host.playerId);
            }
            if (clients.isEmpty()) {
                isRunning = false;
                rooms.remove(name);
                SimpleTestServer.broadcastRoomList(); 
                System.out.println("[ROOM " + name + "] Closed (Empty)");
            } else {
                broadcastRoomInfo();
            }
        }
        
        public void broadcastRoomInfo() {
            StringBuilder sb = new StringBuilder("ROOM_UPDATE;");
            int hostId = (host != null) ? host.playerId : -1;
            sb.append(hostId).append(";");
            for(ClientHandler c : clients) sb.append(c.playerId).append(",");
            broadcast(sb.toString());
        }

        public void broadcast(String msg) {
            for (ClientHandler c : clients) c.send(msg);
        }

        @Override
        public void run() {
            while (isRunning) {
                try {
                    Thread.sleep(16); 
                    
                    if (!gameStarted || clients.isEmpty() || collisionHandler == null) continue;
                    
                    // --- PHYSICS UPDATE ---
                    for (PlayerState p : players) {
                        double speed = 3.0; 
                        
                        double nextX = p.x;
                        double nextY = p.y;
                        
                        if (p.left)  nextX -= speed;
                        if (p.right) nextX += speed;
                        if (p.up)    nextY -= speed;
                        if (p.down)  nextY += speed;
                        
                        if (!collisionHandler.checkCollision(nextX, p.y)) p.x = nextX;
                        if (!collisionHandler.checkCollision(p.x, nextY)) p.y = nextY;

                        // UPDATE ANIMASI DAN PRINT DEBUG JIKA BERUBAH
                        p.updateAnimLogic();
                    }

                    // --- BROADCAST STATE ---
                    StringBuilder sb = new StringBuilder("STATE;");
                    for (int i = 0; i < players.size(); i++) {
                        PlayerState p = players.get(i);
                        
                        // Periksa apakah pesan yang disusun mengandung kata WALK
                        // Ini memastikan server tidak mengirim hardcoded IDLE
                        sb.append(p.id).append(",")
                          .append((int)p.x).append(",")
                          .append((int)p.y).append(",")
                          .append(p.currentState).append(",")  // Dinamis
                          .append(p.currentDir);               // Dinamis
                        
                        if (i < players.size() - 1) sb.append("#");
                    }
                    sb.append("|||"); 
                    
                    // [DEBUG OPTIONAL] Uncomment baris di bawah ini jika ingin melihat setiap paket (SPAMMY!)
                    // System.out.println("Sending: " + sb.toString());
                    
                    broadcast(sb.toString());

                } catch (Exception e) {
                    System.err.println("Game Loop Error: " + e.getMessage());
                }
            }
        }
        
        public void initGameMap() {
            this.mapData = MapGenerator.generateMapArray(13, 13);
            this.collisionHandler = new CollisionHandler(this.mapData, 32);
        }
        
        public String getMapString() {
            return MapGenerator.convertToString(this.mapData);
        }
    }

    // ===================== CLIENT HANDLER =======================
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private Room currentRoom;
        public int playerId; 

        public ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                sendRoomList(); 

                String msg;
                while ((msg = in.readLine()) != null) {
                    handleMessage(msg);
                }
            } catch (IOException e) {
            } finally {
                lobbyClients.remove(this);
                if (currentRoom != null) currentRoom.removePlayer(this, playerId);
                System.out.println("Client disconnected.");
            }
        }

        public void send(String msg) { if (out != null) out.println(msg); }
        
        public void sendRoomList() {
            StringBuilder sb = new StringBuilder("ROOM_LIST;");
            for (Room r : rooms.values()) {
                String type = r.isPrivate ? "(Private)" : "(Public)";
                sb.append(r.name).append(":").append(type).append(",");
            }
            send(sb.toString());
        }

        private void handleMessage(String msg) {
            try {
                String[] parts = msg.split(";");
                String command = parts[0];

                if (command.equals("CREATE_ROOM")) {
                    String name = parts[1];
                    boolean isPrivate = Boolean.parseBoolean(parts[2]);
                    String pass = parts.length > 3 ? parts[3] : "";
                    synchronized (rooms) {
                        if (rooms.containsKey(name)) {
                            send("ERROR;Room Name Taken");
                            return;
                        }
                        Room newRoom = new Room(name, isPrivate ? pass : null);
                        rooms.put(name, newRoom);
                        new Thread(newRoom).start();
                        SimpleTestServer.broadcastRoomList();
                        joinRoom(newRoom);
                    }
                } 
                else if (command.equals("JOIN_ROOM")) {
                    String name = parts[1];
                    String passInput = parts.length > 2 ? parts[2] : "";
                    Room room = rooms.get(name);
                    if (room != null) {
                        if (room.isPrivate && !room.password.equals(passInput)) {
                            send("ERROR;Wrong Password");
                        } else {
                            joinRoom(room);
                        }
                    }
                } 
                else if (command.equals("LEAVE_ROOM")) {
                    if (currentRoom != null) {
                        currentRoom.removePlayer(this, playerId);
                        currentRoom = null;
                        lobbyClients.add(this);
                        sendRoomList();
                    }
                }
                else if (command.equals("START_GAME") && currentRoom != null) {
                    if(currentRoom.host == this && currentRoom.clients.size() >= 1) { 
                         new Thread(() -> {
                             System.out.println("[ROOM " + currentRoom.name + "] Starting game...");
                             currentRoom.initGameMap();
                             String mapData = currentRoom.getMapString();
                             currentRoom.gameStarted = true;
                             currentRoom.broadcast("GAME_STARTED");
                             currentRoom.broadcast("MAP;13;13;" + mapData);
                         }).start();
                    }
                }
                // --- INPUT HANDLER DENGAN DEBUG ---
                else if (command.equals("INPUT") && currentRoom != null) {
                     if (currentRoom.players.size() > playerId) {
                         PlayerState p = currentRoom.players.get(playerId);
                         String key = parts[1];
                         boolean pressed = Boolean.parseBoolean(parts[2]); // FIX Input
                         
                         // Debug Print Input Masuk
                         // System.out.println("[INPUT] Player " + playerId + " " + key + " = " + pressed);

                         switch (key) {
                            case "UP"    -> p.up = pressed;
                            case "DOWN"  -> p.down = pressed;
                            case "LEFT"  -> p.left = pressed;
                            case "RIGHT" -> p.right = pressed;
                        }
                     }
                }
                else if (command.equals("ACTION") && parts.length > 1 && parts[1].equals("PLACE_BOMB") && currentRoom != null) {
                    if (playerId < currentRoom.players.size()) {
                        PlayerState p = currentRoom.players.get(playerId);
                        p.place = true;
                        new Thread(() -> {
                            try { Thread.sleep(200); } catch (Exception ignored) {}
                            p.place = false;
                        }).start();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing message: " + msg);
            }
        }

        private void joinRoom(Room room) {
            lobbyClients.remove(this); 
            if (currentRoom != null) currentRoom.removePlayer(this, playerId);
            currentRoom = room;
            int newId = room.addPlayer(this);
            send("YOUR_ID;" + newId);
        }
    }

    // ===================== COLLISION HANDLER =======================
    static class CollisionHandler {
        private final int[][] map;
        private final int tileSize;
        public final double hitboxSize = 20.5;
        public final double offset;

        public CollisionHandler(int[][] map, int tileSize) {
            this.map = map;
            this.tileSize = tileSize;
            this.offset = (tileSize - hitboxSize) / 2.0; 
        }

        public boolean checkCollision(double x, double y) {
            double left = x + offset;
            double top = y + offset;
            double right = left + hitboxSize;
            double bottom = top + hitboxSize;

            return isSolid(left, top) || isSolid(right, top) || isSolid(left, bottom) || isSolid(right, bottom);
        }

        private boolean isSolid(double px, double py) {
            int gridX = (int) (px / tileSize);
            int gridY = (int) (py / tileSize);
            if (gridX < 0 || gridY < 0 || gridX >= map.length || gridY >= map[0].length) return true;
            int t = map[gridX][gridY];
            return (t == 1 || t == 2);
        }
    }

    // ===================== MAP GENERATOR =======================
    static class MapGenerator {
        public static int[][] generateMapArray(int w, int h) {
            int[][] map = new int[w][h];
            Random rand = new Random();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (x == 0 || y == 0 || x == w - 1 || y == h - 1) { map[x][y] = 1; continue; }
                    if (x % 2 == 0 && y % 2 == 0) { map[x][y] = 1; continue; }
                    if (rand.nextDouble() < 0.70) { map[x][y] = 2; } else { map[x][y] = 0; }
                }
            }
            if (w > 2 && h > 3) { 
                map[1][1] = 0; map[1][2] = 0; map[1][3] = 0; 
                map[2][1] = 0; map[3][1] = 0;
            }
            return map;
        }
        
        public static String convertToString(int[][] map) {
            StringBuilder sb = new StringBuilder();
            for (int y = 0; y < map[0].length; y++) {
                for (int x = 0; x < map.length; x++) {
                    sb.append(map[x][y]).append(",");
                }
            }
            return sb.toString();
        }
    }
    
    // ===================== PLAYER STATE (WITH DEBUG) =======================
    static class PlayerState {
        int id; 
        double x, y;
        boolean up, down, left, right;
        boolean place; 
        
        String currentState = "IDLE";
        String currentDir = "DOWN";

        public PlayerState(int id, double x, double y) { 
            this.id=id; this.x=x; this.y=y; 
        }
        
        public void updateAnimLogic() {
            boolean isMoving = up || down || left || right;
            String oldState = currentState; // Simpan state lama
            
            if (isMoving) {
                currentState = "WALK";
                
                if (down)      currentDir = "DOWN";
                else if (up)   currentDir = "UP";
                else if (left) currentDir = "LEFT";
                else if (right)currentDir = "RIGHT";
            } else {
                currentState = "IDLE";
            }

            // --- DEBUG PRINT: HANYA MUNCUL JIKA STATUS BERUBAH ---
            if (!currentState.equals(oldState)) {
                System.out.println("[DEBUG P" + id + "] Changed to: " + currentState + " (" + currentDir + ")");
                System.out.println("   --> Input: U=" + up + " D=" + down + " L=" + left + " R=" + right);
            }
        }
    }
}