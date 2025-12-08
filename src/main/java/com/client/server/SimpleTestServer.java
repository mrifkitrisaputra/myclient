package com.client.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
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
        System.out.println("SERVER STARTED ON PORT " + PORT + " [FULL LOGIC MODE]");

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
            if (!r.gameStarted) {
                String type = r.isPrivate ? "(Private)" : "(Public)";
                sb.append(r.name).append(":").append(type).append(",");
            }
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

        private double gameTime = 180.0; // 3 Menit
        private boolean isGameOver = false;

        List<PlayerState> players = new CopyOnWriteArrayList<>();
        List<ClientHandler> clients = new CopyOnWriteArrayList<>();
        
        // --- DATA BARU ---
        List<Bomb> bombs = new CopyOnWriteArrayList<>();
        List<Item> items = new CopyOnWriteArrayList<>();
        
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
            for (ClientHandler c : clients) sb.append(c.playerId).append(",");
            broadcast(sb.toString());
        }

        public void broadcast(String msg) {
            for (ClientHandler c : clients) c.send(msg);
        }

        @Override
        public void run() {
            long lastTime = System.nanoTime();

            while (isRunning) {
                try {
                    long now = System.nanoTime();
                    double dt = (now - lastTime) / 1_000_000_000.0;
                    lastTime = now;

                    Thread.sleep(16); // ~60 FPS

                    if (!gameStarted || clients.isEmpty() || collisionHandler == null || isGameOver) continue;

                    // --- TIMER ---
                    gameTime -= dt;
                    if (gameTime <= 0) {
                        gameTime = 0;
                        isGameOver = true;
                        broadcast("GAME_OVER;TIME_UP");
                    }

                    // --- BOMB TICK ---
                    for (Bomb b : bombs) {
                        b.tick();
                    }
                    bombs.removeIf(b -> b.exploded);

                    // --- PHYSICS UPDATE ---
                    for (PlayerState p : players) {
                        if (p.dead) continue;

                        double nextX = p.x;
                        double nextY = p.y;

                        if (p.left)  nextX -= p.speed;
                        if (p.right) nextX += p.speed;
                        if (p.up)    nextY -= p.speed;
                        if (p.down)  nextY += p.speed;

                        // Cek collision MAP dan PLAYER dan BOMB
                        boolean collideMapX = collisionHandler.checkCollision(nextX, p.y);
                        boolean collidePlayerX = collisionHandler.checkPlayerCollision(nextX, p.y, p, players);

                        if (!collideMapX && !collidePlayerX) {
                            p.x = nextX;
                        }

                        boolean collideMapY = collisionHandler.checkCollision(p.x, nextY);
                        boolean collidePlayerY = collisionHandler.checkPlayerCollision(p.x, nextY, p, players);

                        if (!collideMapY && !collidePlayerY) {
                            p.y = nextY;
                        }

                        p.updateAnimLogic();
                        
                        // --- CHECK ITEM PICKUP ---
                        checkItemPickup(p);
                    }

                    // --- BROADCAST STATE ---
                    StringBuilder sb = new StringBuilder("STATE;");
                    sb.append((int) Math.ceil(gameTime)).append(";");

                    for (int i = 0; i < players.size(); i++) {
                        PlayerState p = players.get(i);
                        sb.append(p.id).append(",")
                                .append((int) p.x).append(",")
                                .append((int) p.y).append(",")
                                .append(p.currentState).append(",")
                                .append(p.currentDir);
                        if (i < players.size() - 1) sb.append("#");
                    }
                    sb.append("|||");
                    broadcast(sb.toString());

                } catch (Exception e) {
                    System.err.println("Game Loop Error: " + e.getMessage());
                }
            }
        }
        
        // --- LOGIC ITEM PICKUP ---
        private void checkItemPickup(PlayerState p) {
            int tileSize = 32;
            int pGridX = (int) ((p.x + tileSize / 2) / tileSize);
            int pGridY = (int) ((p.y + tileSize / 2) / tileSize);

            for (Item item : items) {
                if (item.x == pGridX && item.y == pGridY) {
                    // Apply Effect
                    switch (item.type) {
                        case BOMB_UP -> p.maxBombs++;
                        case FIRE_UP -> p.fireRange++;
                        case SPEED_UP -> p.speed += 1.0;
                    }
                    items.remove(item);
                    broadcast("ITEM_PICKED;" + p.id + "," + item.x + "," + item.y + "," + item.type.name());
                    return; 
                }
            }
        }

        // --- LOGIC HANCURKAN TILE ---
        public void breakTile(int tx, int ty) {
            mapData[tx][ty] = 0; // Jadi lantai kosong
            broadcast("BREAK_TILE;" + tx + "," + ty);

            // Random Drop Item (30% chance)
            Random random = new Random();
            if (random.nextDouble() < 0.3) {
                double r = random.nextDouble();
                Item.ItemType type;
                if (r < 0.4) type = Item.ItemType.BOMB_UP;
                else if (r < 0.8) type = Item.ItemType.FIRE_UP;
                else type = Item.ItemType.SPEED_UP;

                items.add(new Item(tx, ty, type));
                broadcast("SPAWN_ITEM;" + tx + "," + ty + "," + type);
            }
        }
        
        public boolean isSolidTile(int tx, int ty) {
             if (tx < 0 || ty < 0 || tx >= mapData.length || ty >= mapData[0].length) return true;
             return mapData[tx][ty] == 1; // Wall keras
        }

        public boolean isBreakableTile(int tx, int ty) {
            if (tx < 0 || ty < 0 || tx >= mapData.length || ty >= mapData[0].length) return false;
            return mapData[tx][ty] == 2; // Wall hancur
        }

        public void initGameMap() {
            this.mapData = MapGenerator.generateMapArray(13, 13);
            // Pass bombs ke CollisionHandler
            this.collisionHandler = new CollisionHandler(this.mapData, 32, this.bombs);
        }

        public String getMapString() {
            return MapGenerator.convertToString(this.mapData);
        }
    }
    
    // ===================== CLASS BOMB =======================
    static class Bomb {
        int x, y, ownerId, range;
        int timer = 60; // ~3 detik
        boolean exploded = false;
        
        // Logic Solid: Bomb bisa dilewati 0.5 detik pertama
        boolean isSolid = false;
        int solidDelay = 15; 
        
        Room room;

        public Bomb(int x, int y, int ownerId, int range, Room room) {
            this.x = x; this.y = y; this.ownerId = ownerId; this.range = range; this.room = room;
        }

        public void tick() {
            if (exploded) return;
            
            // Update status solid
            if (solidDelay > 0) {
                solidDelay--;
                if (solidDelay <= 0) isSolid = true;
            }

            timer--;
            if (timer <= 0) explode();
        }

        private void explode() {
            exploded = true;
            List<String> parts = new ArrayList<>(); // Simpan string part ledakan
            parts.add(x + "," + y + ",false");

            calculateRay(1, 0, parts);  // Kanan
            calculateRay(-1, 0, parts); // Kiri
            calculateRay(0, 1, parts);  // Bawah
            calculateRay(0, -1, parts); // Atas
            
            // Broadcast Visual
            StringBuilder sb = new StringBuilder("EXPLOSION;").append(x).append(",").append(y);
            for(String p : parts) sb.append(";").append(p);
            room.broadcast(sb.toString());

            // Reduce ammo player
            for(PlayerState p : room.players) {
                if(p.id == ownerId) {
                    p.activeBombs = Math.max(0, p.activeBombs - 1);
                    break;
                }
            }
        }
        
        private void calculateRay(int dx, int dy, List<String> parts) {
            for (int i = 1; i <= range; i++) {
                int tx = x + (dx * i);
                int ty = y + (dy * i);

                // Kena tembok keras -> stop
                if (room.isSolidTile(tx, ty)) break;

                parts.add(tx + "," + ty + "," + (dy!=0));

                // Kena player logic
                for (PlayerState p : room.players) {
                    int px = (int)((p.x + 16)/32);
                    int py = (int)((p.y + 16)/32);
                    if (px == tx && py == ty && !p.dead) {
                        p.dead = true;
                        p.currentState = "DEAD";
                        room.broadcast("PLAYER_DIED;" + p.id);
                    }
                }
                
                // Kena tembok hancur -> hancurkan & stop
                if (room.isBreakableTile(tx, ty)) {
                    room.breakTile(tx, ty);
                    break; 
                }
            }
        }
    }
    
    // ===================== CLASS ITEM =======================
    static class Item {
        int x, y;
        ItemType type;
        enum ItemType { BOMB_UP, FIRE_UP, SPEED_UP }

        public Item(int x, int y, ItemType type) {
            this.x = x; this.y = y; this.type = type;
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
        public void sendRoomList() { SimpleTestServer.broadcastRoomList(); }

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
                } else if (command.equals("JOIN_ROOM")) {
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
                } else if (command.equals("LEAVE_ROOM")) {
                    if (currentRoom != null) {
                        currentRoom.removePlayer(this, playerId);
                        currentRoom = null;
                        lobbyClients.add(this);
                        sendRoomList();
                    }
                } else if (command.equals("START_GAME") && currentRoom != null) {
                    if (currentRoom.host == this && currentRoom.clients.size() >= 1) {
                        new Thread(() -> {
                            System.out.println("[ROOM " + currentRoom.name + "] Starting game...");
                            currentRoom.initGameMap();
                            currentRoom.gameStarted = true;
                            SimpleTestServer.broadcastRoomList();
                            currentRoom.broadcast("GAME_STARTED");
                            String mapData = currentRoom.getMapString();
                            currentRoom.broadcast("MAP;13;13;" + mapData);
                        }).start();
                    }
                } else if (command.equals("INPUT") && currentRoom != null) {
                    if (currentRoom.players.size() > playerId) {
                        PlayerState p = currentRoom.players.get(playerId);
                        String key = parts[1];
                        boolean pressed = Boolean.parseBoolean(parts[2]);
                        switch (key) {
                            case "UP" -> p.up = pressed;
                            case "DOWN" -> p.down = pressed;
                            case "LEFT" -> p.left = pressed;
                            case "RIGHT" -> p.right = pressed;
                        }
                    }
                } else if (command.equals("ACTION") && parts.length > 1 && parts[1].equals("PLACE_BOMB") && currentRoom != null) {
                    if (playerId < currentRoom.players.size()) {
                        PlayerState p = currentRoom.players.get(playerId);
                        // Cek max bombs
                        if (p.activeBombs < p.maxBombs && !p.dead) {
                            int tx = (int) ((p.x + 16) / 32);
                            int ty = (int) ((p.y + 16) / 32);
                            
                            // Cek apakah tile kosong dan tidak ada bom lain
                            boolean canPlace = !currentRoom.isSolidTile(tx, ty);
                            for(Bomb b : currentRoom.bombs) {
                                if(b.x == tx && b.y == ty) { canPlace = false; break; }
                            }
                            
                            if(canPlace) {
                                Bomb bomb = new Bomb(tx, ty, playerId, p.fireRange, currentRoom);
                                currentRoom.bombs.add(bomb);
                                p.activeBombs++;
                                currentRoom.broadcast("BOMB_PLACED;" + tx + "," + ty + ";" + playerId);
                            }
                        }
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
        private final List<Bomb> bombs; // Reference ke bombs room

        public CollisionHandler(int[][] map, int tileSize, List<Bomb> bombs) {
            this.map = map;
            this.tileSize = tileSize;
            this.bombs = bombs;
            this.offset = (tileSize - hitboxSize) / 2.0;
        }

        public boolean checkCollision(double x, double y) {
            double left = x + offset;
            double top = y + offset;
            double right = left + hitboxSize;
            double bottom = top + hitboxSize;

            return isSolid(left, top) || isSolid(right, top) || isSolid(left, bottom) || isSolid(right, bottom);
        }

        public boolean checkPlayerCollision(double newX, double newY, PlayerState self, List<PlayerState> allPlayers) {
            double selfL = newX + offset;
            double selfT = newY + offset;
            double selfR = selfL + hitboxSize;
            double selfB = selfT + hitboxSize;

            for (PlayerState other : allPlayers) {
                if (other == self || other.dead) continue; // Jangan tabrak mayat

                double otherL = other.x + offset;
                double otherT = other.y + offset;
                double otherR = otherL + hitboxSize;
                double otherB = otherT + hitboxSize;

                if (selfL < otherR && selfR > otherL && selfT < otherB && selfB > otherT) {
                    return true;
                }
            }
            return false;
        }

        private boolean isSolid(double px, double py) {
            int gridX = (int) (px / tileSize);
            int gridY = (int) (py / tileSize);
            if (gridX < 0 || gridY < 0 || gridX >= map.length || gridY >= map[0].length) return true;
            
            // 1. Cek Map (1=Wall, 2=Breakable)
            int t = map[gridX][gridY];
            if (t == 1 || t == 2) return true;
            
            // 2. Cek Bomb Solid
            if(bombs != null) {
                for(Bomb b : bombs) {
                    // Bomb solid jika isSolid=true dan belum meledak
                    if(!b.exploded && b.isSolid && b.x == gridX && b.y == gridY) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    // ===================== MAP GENERATOR =======================
    static class MapGenerator {
        public static int[][] generateMapArray(int w, int h) {
            int[][] map = new int[w][h];
            Random rand = new Random();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
                        map[x][y] = 1; continue;
                    }
                    if (x % 2 == 0 && y % 2 == 0) {
                        map[x][y] = 1; continue;
                    }
                    if (rand.nextDouble() < 0.70) {
                        map[x][y] = 2; // Breakable
                    } else {
                        map[x][y] = 0;
                    }
                }
            }
            // Clear spawn area
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

    // ===================== PLAYER STATE =======================
    static class PlayerState {
        int id;
        double x, y;
        boolean up, down, left, right;
        boolean place;

        String currentState = "IDLE";
        String currentDir = "DOWN";
        boolean dead = false;
        
        // --- STATS ---
        int maxBombs = 1;
        int activeBombs = 0;
        int fireRange = 1;
        double speed = 3.0;

        public PlayerState(int id, double x, double y) {
            this.id = id; this.x = x; this.y = y;
        }

        public void updateAnimLogic() {
            if (dead) {
                currentState = "DEAD";
                return;
            }
            boolean isMoving = up || down || left || right;

            if (isMoving) {
                currentState = "WALK";
                if (down)      currentDir = "DOWN";
                else if (up)   currentDir = "UP";
                else if (left) currentDir = "LEFT";
                else if (right)currentDir = "RIGHT";
            } else {
                currentState = "IDLE";
            }
        }
    }
}