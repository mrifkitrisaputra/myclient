package com.client.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
// [IMPORT BARU UNTUK EXECUTOR]
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimpleTestServer {

    private static final int PORT = 5000;
    private static final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private static final List<ClientHandler> lobbyClients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("SERVER STARTED ON PORT " + PORT + " [MULTI-THREADED EXECUTOR MODE]");

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
        if (lobbyClients.isEmpty())
            return;
        StringBuilder sb = new StringBuilder("ROOM_LIST;");
        for (Room r : rooms.values()) {
            if (r.isRunning && !r.gameStarted) {
                String type = r.isPrivate ? "(Private)" : "(Public)";
                sb.append(r.name).append(":").append(type).append(",");
            }
        }
        String msg = sb.toString();
        for (ClientHandler c : lobbyClients)
            c.send(msg);
    }

    // ===================== CLASS ROOM (EXECUTOR IMPLEMENTATION)
    // =======================
    static class Room { // Hapus implements Runnable
        String name;
        String password = "";
        boolean isPrivate = false;
        boolean gameStarted = false;
        boolean isRunning = true;

        private double gameTime = 60.0;
        private boolean isGameOver = false;

        private double gameOverDelayTimer = -1;
        private String pendingGameOverMsg = "";

        private long physicsTick = 0;
        private long bombTick = 0;
        private long broadcastTick = 0;

        Set<Integer> rematchVotes = new HashSet<>();

        // Thread-safe Lists
        List<PlayerState> players = new CopyOnWriteArrayList<>();
        List<ClientHandler> clients = new CopyOnWriteArrayList<>();
        List<Bomb> bombs = new CopyOnWriteArrayList<>();
        List<Item> items = new CopyOnWriteArrayList<>();

        ClientHandler host;
        private int[][] mapData;
        private CollisionHandler collisionHandler;
        private Arena arena;

        // [BARU] EXECUTOR SERVICE
        private ScheduledExecutorService executor;

        public Room(String name, String password) {
            this.name = name;
            if (password != null && !password.isEmpty()) {
                this.password = password;
                this.isPrivate = true;
            }
        }

        // --- METHOD UNTUK MENJALANKAN LOOP (PENGGANTI RUN) ---
        public void startGameLoop() {
            if (executor != null && !executor.isShutdown())
                return; // Sudah jalan

            System.out.println("[ROOM " + name + "] Starting Executor Threads...");
            executor = Executors.newScheduledThreadPool(3);

            // Thread 1: Physics & Game Logic (Player Move, Win Condition, Arena)
            executor.scheduleAtFixedRate(this::updatePhysics, 0, 16, TimeUnit.MILLISECONDS);

            // Thread 2: Bomb Logic (Tick, Explode)
            executor.scheduleAtFixedRate(this::updateBombs, 0, 16, TimeUnit.MILLISECONDS);

            // Thread 3: Broadcast State (Send data to client)
            executor.scheduleAtFixedRate(this::broadcastState, 0, 16, TimeUnit.MILLISECONDS);
        }

        public void stopGameLoop() {
            isRunning = false;
            if (executor != null) {
                System.out.println("[ROOM " + name + "] Shutting down Executor...");
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                }
            }
        }

        // --- TASK 1: UPDATE PHYSICS & GAME RULES ---
        private void updatePhysics() {
            long start = System.nanoTime();
            physicsTick++;
            if (!gameStarted || clients.isEmpty() || collisionHandler == null)
                return;

            double dt = 0.016; // Fixed Time Step 16ms

            // 1. Game Over Delay Logic
            if (isGameOver && gameOverDelayTimer > 0) {
                gameOverDelayTimer -= dt;
                if (gameOverDelayTimer <= 0) {
                    broadcast(pendingGameOverMsg);
                    gameOverDelayTimer = -1;
                }
            }

            // 2. Main Rules
            if (!isGameOver) {
                gameTime -= dt;

                // Update Arena
                if (arena != null)
                    arena.update(dt, gameTime, players);

                long aliveCount = players.stream().filter(p -> !p.dead).count();

                if (gameTime <= 0) {
                    gameTime = 0;
                    triggerGameOver("SURVIVORS");
                } else if (gameStarted && players.size() > 1 && aliveCount <= 1) {
                    triggerGameOver("WINNER");
                }
            }

            // 3. Player Movement & Pickup
            // Karena fixed step, multiplier selalu 1.0 (speed 100 px/detik * 0.016 = 1.6
            // px/frame)
            // Atau balikin ke logic sebelumnya: moveAmt = p.speed * dt

            for (PlayerState p : players) {
                if (p.dead) {
                    p.deadTimer += dt;
                    continue;
                }

                if (p.speedTimer > 0) {
                    p.speedTimer -= dt;
                    if (p.speedTimer <= 0) {
                        p.speed = p.DEFAULT_SPEED;
                        p.speedTimer = 0;
                    }
                }

                double moveAmt = p.speed * dt; // FIXED SPEED CALCULATION

                double nextX = p.x;
                double nextY = p.y;

                if (p.left)
                    nextX -= moveAmt;
                if (p.right)
                    nextX += moveAmt;
                if (p.up)
                    nextY -= moveAmt;
                if (p.down)
                    nextY += moveAmt;

                boolean collideMapX = collisionHandler.checkCollision(nextX, p.y);
                boolean collidePlayerX = collisionHandler.checkPlayerCollision(nextX, p.y, p, players);
                if (!collideMapX && !collidePlayerX)
                    p.x = nextX;

                boolean collideMapY = collisionHandler.checkCollision(p.x, nextY);
                boolean collidePlayerY = collisionHandler.checkPlayerCollision(p.x, nextY, p, players);
                if (!collideMapY && !collidePlayerY)
                    p.y = nextY;

                p.updateAnimLogic();
                checkItemPickup(p);
            }
            long end = System.nanoTime();
            double elapsed = (end - start) / 1_000_000.0;
            System.out.println("[Physics] Tick " + physicsTick + 
                             " | Thread: " + Thread.currentThread().getName() + 
                             " | Elapsed(ms): " + String.format("%.4f", elapsed));
        }

        // --- TASK 2: UPDATE BOMBS ---
        private void updateBombs() {
            long start = System.nanoTime();
            bombTick++;
            if (!gameStarted || isGameOver)
                return;

            double dt = 0.016;

            // Loop thread-safe karena pakai CopyOnWriteArrayList
            for (Bomb b : bombs) {
                b.tick(dt);
            }
            bombs.removeIf(b -> b.exploded);
            long end = System.nanoTime();
            double elapsed = (end - start) / 1_000_000.0;
            System.out.println("[Bomb]    Tick " + bombTick + 
                             " | Thread: " + Thread.currentThread().getName() + 
                             " | Elapsed(ms): " + String.format("%.4f", elapsed));
        }

        // --- TASK 3: BROADCAST STATE ---
        private void broadcastState() {
            long start = System.nanoTime();
            broadcastTick++;
            if (!gameStarted || clients.isEmpty())
                return;

            try {
                StringBuilder sb = new StringBuilder("STATE;");
                sb.append((int) Math.ceil(gameTime)).append(";");

                for (int i = 0; i < players.size(); i++) {
                    PlayerState p = players.get(i);
                    // Filter: Jika mati > 1.5s jangan kirim
                    if (p.dead && p.deadTimer > 1.5)
                        continue;

                    sb.append(p.id).append(",")
                            .append((int) p.x).append(",")
                            .append((int) p.y).append(",")
                            .append(p.currentState).append(",")
                            .append(p.currentDir);
                    if (i < players.size() - 1)
                        sb.append("#");
                }
                sb.append("|||");
                broadcast(sb.toString());

            } catch (Exception e) {
                System.err.println("Broadcast Error: " + e.getMessage());
            }
            long end = System.nanoTime();
            double elapsed = (end - start) / 1_000_000.0;
            System.out.println("[Network] Tick " + broadcastTick + 
                             " | Thread: " + Thread.currentThread().getName() + 
                             " | Elapsed(ms): " + String.format("%.4f", elapsed));
        }

        // --- SISA METHOD ROOM (LOGIC TIDAK BERUBAH) ---

        private double[] getSpawnPosition(int index) {
            int gridX = 1;
            int gridY = 1;
            switch (index % 4) {
                case 0 -> {
                    gridX = 1;
                    gridY = 1;
                }
                case 1 -> {
                    gridX = 11;
                    gridY = 1;
                }
                case 2 -> {
                    gridX = 1;
                    gridY = 11;
                }
                case 3 -> {
                    gridX = 11;
                    gridY = 11;
                }
            }
            return new double[] { gridX * 32, gridY * 32 };
        }

        public synchronized int addPlayer(ClientHandler client) {
            clients.add(client);
            int id = clients.size() - 1;
            client.playerId = id;
            double[] pos = getSpawnPosition(id);
            players.add(new PlayerState(id, pos[0], pos[1]));
            if (clients.size() == 1)
                host = client;
            broadcastRoomInfo();
            return id;
        }

        public synchronized void removePlayer(ClientHandler client, int playerId) {
            clients.remove(client);
            rematchVotes.remove(playerId);
            if (client == host && !clients.isEmpty()) {
                host = clients.get(0);
                System.out.println("[ROOM " + name + "] Host migrated to ID " + host.playerId);
            }
            if (clients.isEmpty()) {
                System.out.println("[ROOM " + name + "] Empty. Stopping Executor...");
                stopGameLoop(); // STOP EXECUTOR
                rooms.remove(name);
                SimpleTestServer.broadcastRoomList();
            } else {
                broadcastRoomInfo();
            }
        }

        public synchronized void restartGame() {
            System.out.println("[ROOM " + name + "] RESTARTING...");
            this.gameTime = 60.0;
            this.isGameOver = false;
            this.gameOverDelayTimer = -1;
            this.pendingGameOverMsg = "";
            this.gameStarted = true;

            this.rematchVotes.clear();
            this.bombs.clear();
            this.items.clear();
            this.players.clear();

            initGameMap();

            for (ClientHandler c : clients) {
                double[] pos = getSpawnPosition(c.playerId);
                this.players.add(new PlayerState(c.playerId, pos[0], pos[1]));
            }

            broadcast("RESET_GAME_STATE");
            broadcast("GAME_STARTED");
            String mapStr = getMapString();
            broadcast("MAP;13;13;" + mapStr);
            broadcastRoomInfo();
        }

        // ... (broadcastRoomInfo, broadcast, triggerGameOver, checkItemPickup,
        // breakTile, isSolidTile, isBreakableTile, initGameMap, getMapString)
        // ... (SAMA PERSIS DENGAN KODE SEBELUMNYA, TIDAK PERLU DIUBAH) ...

        public void broadcastRoomInfo() {
            StringBuilder sb = new StringBuilder("ROOM_UPDATE;");
            int hostId = (host != null) ? host.playerId : -1;
            sb.append(hostId).append(";");
            for (ClientHandler c : clients)
                sb.append(c.playerId).append(",");
            broadcast(sb.toString());
        }

        public void broadcast(String msg) {
            for (ClientHandler c : clients)
                c.send(msg);
        }

        private void triggerGameOver(String type) {
            isGameOver = true;
            gameOverDelayTimer = 2.0;
            if (type.equals("SURVIVORS")) {
                StringBuilder survivors = new StringBuilder();
                for (PlayerState p : players) {
                    if (!p.dead) {
                        if (survivors.length() > 0)
                            survivors.append(",");
                        survivors.append(p.id);
                    }
                }
                if (survivors.length() > 0)
                    pendingGameOverMsg = "GAME_OVER;SURVIVORS;" + survivors.toString();
                else
                    pendingGameOverMsg = "GAME_OVER;DRAW";
            } else if (type.equals("WINNER")) {
                int winnerId = -1;
                for (PlayerState p : players) {
                    if (!p.dead) {
                        winnerId = p.id;
                        break;
                    }
                }
                if (winnerId != -1)
                    pendingGameOverMsg = "GAME_OVER;WINNER;" + winnerId;
                else
                    pendingGameOverMsg = "GAME_OVER;DRAW";
            }
        }

        private void checkItemPickup(PlayerState p) {
            int tileSize = 32;
            int pGridX = (int) ((p.x + tileSize / 2) / tileSize);
            int pGridY = (int) ((p.y + tileSize / 2) / tileSize);
            for (Item item : items) {
                if (item.x == pGridX && item.y == pGridY) {
                    switch (item.type) {
                        case BOMB_UP -> p.bonusBombStock++;
                        case FIRE_UP -> p.hasFirePowerUp = true;
                        case SPEED_UP -> {
                            if (p.speedTimer <= 0) {
                                p.speed = 5.0;
                                p.speedTimer = 5.0;
                            }
                        }
                    }
                    items.remove(item);
                    broadcast("ITEM_PICKED;" + p.id + "," + item.x + "," + item.y + "," + item.type.name());
                    return;
                }
            }
        }

        public void breakTile(int tx, int ty) {
            mapData[tx][ty] = 0;
            broadcast("BREAK_TILE;" + tx + "," + ty);
            Random random = new Random();
            if (random.nextDouble() < 0.3) {
                double r = random.nextDouble();
                Item.ItemType type;
                if (r < 0.4)
                    type = Item.ItemType.BOMB_UP;
                else if (r < 0.8)
                    type = Item.ItemType.FIRE_UP;
                else
                    type = Item.ItemType.SPEED_UP;
                items.add(new Item(tx, ty, type));
                broadcast("SPAWN_ITEM;" + tx + "," + ty + "," + type);
            }
        }

        public boolean isSolidTile(int tx, int ty) {
            if (tx < 0 || ty < 0 || tx >= mapData.length || ty >= mapData[0].length)
                return true;
            return mapData[tx][ty] == 1;
        }

        public boolean isBreakableTile(int tx, int ty) {
            if (tx < 0 || ty < 0 || tx >= mapData.length || ty >= mapData[0].length)
                return false;
            return mapData[tx][ty] == 2;
        }

        public void initGameMap() {
            this.mapData = MapGenerator.generateMapArray(13, 13);
            this.collisionHandler = new CollisionHandler(this.mapData, 32, this.bombs);
            this.arena = new Arena(this.mapData, this);
        }

        public String getMapString() {
            return MapGenerator.convertToString(this.mapData);
        }
    }

    // ===================== CLASS HELPER (Arena, Bomb, Item, Collision, Map,
    // Player) =======================
    // --- SAMA PERSIS DENGAN SEBELUMNYA, TIDAK DIUBAH ---

    static class Arena {
        private final int[][] map;
        private final int mapCols;
        private final int mapRows;
        private final Room room;
        private int left = 1;
        private int right;
        private int top = 1;
        private int bottom;
        private final double[] shrinkTimes = { 30.0, 20.0, 10.0, 5.0, 2.0 };
        private final String[] shrinkPattern = { "LR", "TB", "LR", "TB", "LR" };
        private int currentStep = 0;
        private boolean isShrinking = false;
        private double shrinkAnimTimer = 0;
        private static final double SHRINK_WARNING_DURATION = 5.0;

        public Arena(int[][] map, Room room) {
            this.map = map;
            this.room = room;
            this.mapCols = map.length;
            this.mapRows = map[0].length;
            this.right = mapCols - 2;
            this.bottom = mapRows - 2;
        }

        public void update(double dt, double gameTime, List<PlayerState> players) {
            if (!isShrinking && currentStep < shrinkTimes.length) {
                if (gameTime <= shrinkTimes[currentStep])
                    startShrinkSequence();
            }
            if (isShrinking) {
                shrinkAnimTimer += dt;
                if (shrinkAnimTimer < dt * 2) {
                    String pattern = shrinkPattern[currentStep];
                    room.broadcast("ARENA_WARNING;" + pattern + ";" + left + ";" + right + ";" + top + ";" + bottom);
                }
                if (shrinkAnimTimer >= SHRINK_WARNING_DURATION) {
                    executeShrink(players);
                    isShrinking = false;
                }
            }
        }

        private void startShrinkSequence() {
            isShrinking = true;
            shrinkAnimTimer = 0;
            System.out.println("[ARENA] Shrinking Warning Started! Step: " + currentStep);
        }

        private void executeShrink(List<PlayerState> players) {
            if (currentStep >= shrinkPattern.length)
                return;
            String step = shrinkPattern[currentStep];
            for (PlayerState p : players) {
                if (p.dead)
                    continue;
                int px = (int) ((p.x + 16) / 32);
                int py = (int) ((p.y + 16) / 32);
                boolean kill = false;
                if (step.equals("LR")) {
                    if (px <= left || px >= right)
                        kill = true;
                } else {
                    if (py <= top || py >= bottom)
                        kill = true;
                }
                if (kill) {
                    p.dead = true;
                    p.currentState = "DEAD";
                    room.broadcast("PLAYER_DIED;" + p.id);
                }
            }
            if (step.equals("LR")) {
                for (int y = 0; y < mapRows; y++) {
                    map[left][y] = 1;
                    map[right][y] = 1;
                }
                left++;
                right--;
            } else {
                for (int x = 0; x < mapCols; x++) {
                    map[x][top] = 1;
                    map[x][bottom] = 1;
                }
                top++;
                bottom--;
            }
            currentStep++;
            String mapStr = MapGenerator.convertToString(map);
            room.broadcast("MAP_UPDATE;" + mapCols + ";" + mapRows + ";" + mapStr);
        }
    }

    static class Bomb {
        int x, y, ownerId, range;
        double timer = 2.0;
        boolean exploded = false;
        boolean isSolid = false;
        double solidDelay = 0.5;
        Room room;

        public Bomb(int x, int y, int ownerId, int range, Room room) {
            this.x = x;
            this.y = y;
            this.ownerId = ownerId;
            this.range = range;
            this.room = room;
        }

        public void tick(double dt) {
            if (exploded)
                return;
            if (solidDelay > 0) {
                solidDelay -= dt;
                if (solidDelay <= 0) {
                    isSolid = true;
                    solidDelay = 0;
                }
            }
            timer -= dt;
            if (timer <= 0)
                explode();
        }

        private void explode() {
            exploded = true;
            List<String> parts = new ArrayList<>();
            parts.add(x + "," + y + ",false");
            checkPlayerHit(x, y);
            calculateRay(1, 0, parts);
            calculateRay(-1, 0, parts);
            calculateRay(0, 1, parts);
            calculateRay(0, -1, parts);
            StringBuilder sb = new StringBuilder("EXPLOSION;").append(x).append(",").append(y);
            for (String p : parts)
                sb.append(";").append(p);
            room.broadcast(sb.toString());
            for (PlayerState p : room.players) {
                if (p.id == ownerId) {
                    p.activeBombs = Math.max(0, p.activeBombs - 1);
                    break;
                }
            }
        }

        private void checkPlayerHit(int tx, int ty) {
            for (PlayerState p : room.players) {
                int px = (int) ((p.x + 16) / 32);
                int py = (int) ((p.y + 16) / 32);
                if (px == tx && py == ty && !p.dead) {
                    p.dead = true;
                    p.currentState = "DEAD";
                    room.broadcast("PLAYER_DIED;" + p.id);
                }
            }
        }

        private void calculateRay(int dx, int dy, List<String> parts) {
            for (int i = 1; i < range; i++) {
                int tx = x + (dx * i);
                int ty = y + (dy * i);
                if (room.isSolidTile(tx, ty))
                    break;
                parts.add(tx + "," + ty + "," + (dy != 0));
                checkPlayerHit(tx, ty);
                if (room.isBreakableTile(tx, ty)) {
                    room.breakTile(tx, ty);
                    break;
                }
            }
        }
    }

    static class Item {
        int x, y;
        ItemType type;

        enum ItemType {
            BOMB_UP, FIRE_UP, SPEED_UP
        }

        public Item(int x, int y, ItemType type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    static class CollisionHandler {
        private final int[][] map;
        private final int tileSize;
        public final double hitboxSize = 25.5;
        public final double offset;
        private final List<Bomb> bombs;

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
                if (other == self || other.dead)
                    continue;
                double otherL = other.x + offset;
                double otherT = other.y + offset;
                double otherR = otherL + hitboxSize;
                double otherB = otherT + hitboxSize;
                if (selfL < otherR && selfR > otherL && selfT < otherB && selfB > otherT)
                    return true;
            }
            return false;
        }

        private boolean isSolid(double px, double py) {
            int gridX = (int) (px / tileSize);
            int gridY = (int) (py / tileSize);
            if (gridX < 0 || gridY < 0 || gridX >= map.length || gridY >= map[0].length)
                return true;
            int t = map[gridX][gridY];
            if (t == 1 || t == 2)
                return true;
            if (bombs != null) {
                for (Bomb b : bombs) {
                    if (!b.exploded && b.isSolid && b.x == gridX && b.y == gridY)
                        return true;
                }
            }
            return false;
        }
    }

    static class MapGenerator {
        public static int[][] generateMapArray(int w, int h) {
            int[][] map = new int[w][h];
            Random rand = new Random();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
                        map[x][y] = 1;
                        continue;
                    }
                    if (x % 2 == 0 && y % 2 == 0) {
                        map[x][y] = 1;
                        continue;
                    }
                    if (rand.nextDouble() < 0.70) {
                        map[x][y] = 2;
                    } else {
                        map[x][y] = 0;
                    }
                }
            }
            map[1][1] = 0;
            map[1][2] = 0;
            map[2][1] = 0;
            map[w - 2][1] = 0;
            map[w - 2][2] = 0;
            map[w - 3][1] = 0;
            map[1][h - 2] = 0;
            map[1][h - 3] = 0;
            map[2][h - 2] = 0;
            map[w - 2][h - 2] = 0;
            map[w - 2][h - 3] = 0;
            map[w - 3][h - 2] = 0;
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

    static class PlayerState {
        int id;
        double x, y;
        boolean up, down, left, right;
        boolean place;
        String currentState = "IDLE";
        String currentDir = "DOWN";
        boolean dead = false;
        double deadTimer = 0;
        final int DEFAULT_MAX_BOMBS = 1;
        final int DEFAULT_RANGE = 2;
        final double DEFAULT_SPEED = 100.0;
        int activeBombs = 0;
        double speed = DEFAULT_SPEED;
        int bonusBombStock = 0;
        boolean hasFirePowerUp = false;
        double speedTimer = 0;

        public PlayerState(int id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }

        public void updateAnimLogic() {
            if (dead) {
                currentState = "DEAD";
                return;
            }
            boolean isMoving = up || down || left || right;
            if (isMoving) {
                currentState = "WALK";
                if (down)
                    currentDir = "DOWN";
                else if (up)
                    currentDir = "UP";
                else if (left)
                    currentDir = "LEFT";
                else if (right)
                    currentDir = "RIGHT";
            } else {
                currentState = "IDLE";
            }
        }
    }

    // ===================== CLIENT HANDLER (UPDATED) =======================
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private Room currentRoom;
        public int playerId;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                sendRoomList();
                String msg;
                while ((msg = in.readLine()) != null)
                    handleMessage(msg);
            } catch (IOException e) {
            } finally {
                lobbyClients.remove(this);
                if (currentRoom != null)
                    currentRoom.removePlayer(this, playerId);
                System.out.println("Client disconnected.");
            }
        }

        public void send(String msg) {
            if (out != null)
                out.println(msg);
        }

        public void sendRoomList() {
            SimpleTestServer.broadcastRoomList();
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
                            Room r = rooms.get(name);
                            if (r.clients.isEmpty()) {
                                r.isRunning = false;
                                rooms.remove(name);
                            } else {
                                send("ERROR;Room Name Taken");
                                return;
                            }
                        }
                        Room newRoom = new Room(name, isPrivate ? pass : null);
                        rooms.put(name, newRoom);
                        // [MODIFIKASI] Gunakan startGameLoop() bukan thread biasa
                        newRoom.startGameLoop();
                        SimpleTestServer.broadcastRoomList();
                        joinRoom(newRoom);
                    }
                } else if (command.equals("JOIN_ROOM")) {
                    String name = parts[1];
                    String passInput = parts.length > 2 ? parts[2] : "";
                    Room room = rooms.get(name);
                    if (room != null) {
                        if (room.clients.size() >= 4)
                            send("ERROR;Room Full");
                        else if (room.isPrivate && !room.password.equals(passInput))
                            send("ERROR;Wrong Password");
                        else
                            joinRoom(room);
                    }
                } else if (command.equals("KICK_PLAYER") && currentRoom != null) {
                    // Logic Kick Player (Sama seperti sebelumnya)
                    try {
                        int targetId = Integer.parseInt(parts[1]);
                        if (currentRoom.host == this) {
                            ClientHandler target = null;
                            for (ClientHandler c : currentRoom.clients) {
                                if (c.playerId == targetId) {
                                    target = c;
                                    break;
                                }
                            }
                            if (target != null && target != this) {
                                target.send("KICKED");
                                currentRoom.removePlayer(target, targetId);
                                target.currentRoom = null;
                                lobbyClients.add(target);
                                target.sendRoomList();
                            }
                        }
                    } catch (Exception e) {
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
                        // Thread pool sudah jalan di startGameLoop, jadi panggil restartGame langsung
                        // tapi karena restartGame itu synchronized dan berat, sebaiknya tetap di thread
                        // terpisah atau di executor
                        // Tapi karena executor loop-nya periodic, kita jalankan di thread baru biar ga
                        // blocking loop utama handler
                        new Thread(() -> currentRoom.restartGame()).start();
                    }
                }
                // ... Sisa command (VOTE_REMATCH, INPUT, ACTION) SAMA PERSIS ...
                else if (command.equals("VOTE_REMATCH") && currentRoom != null) {
                    synchronized (currentRoom) {
                        currentRoom.rematchVotes.add(playerId);
                        int currentVotes = currentRoom.rematchVotes.size();
                        int totalPlayers = currentRoom.clients.size();
                        currentRoom.broadcast("REMATCH_UPDATE;" + currentVotes + ";" + totalPlayers);
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
                } else if (command.equals("ACTION") && parts.length > 1 && parts[1].equals("PLACE_BOMB")
                        && currentRoom != null) {
                    if (playerId < currentRoom.players.size()) {
                        PlayerState p = currentRoom.players.get(playerId);
                        int currentCapacity = p.DEFAULT_MAX_BOMBS + p.bonusBombStock;
                        if (p.activeBombs < currentCapacity && !p.dead) {
                            int tx = (int) ((p.x + 16) / 32);
                            int ty = (int) ((p.y + 16) / 32);
                            boolean canPlace = !currentRoom.isSolidTile(tx, ty);
                            for (Bomb b : currentRoom.bombs) {
                                if (b.x == tx && b.y == ty) {
                                    canPlace = false;
                                    break;
                                }
                            }
                            if (canPlace) {
                                int range = p.DEFAULT_RANGE;
                                if (p.hasFirePowerUp) {
                                    range += 1;
                                    p.hasFirePowerUp = false;
                                }
                                Bomb bomb = new Bomb(tx, ty, playerId, range, currentRoom);
                                currentRoom.bombs.add(bomb);
                                p.activeBombs++;
                                if (p.activeBombs > p.DEFAULT_MAX_BOMBS && p.bonusBombStock > 0)
                                    p.bonusBombStock--;
                                currentRoom.broadcast("BOMB_PLACED;" + tx + "," + ty + ";" + playerId);
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        private void joinRoom(Room room) {
            lobbyClients.remove(this);
            if (currentRoom != null)
                currentRoom.removePlayer(this, playerId);
            currentRoom = room;
            int newId = room.addPlayer(this);
            send("RESET_GAME_STATE");
            send("YOUR_ID;" + newId);
        }
    }
}