package com.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.client.ClientGameState;

public class ClientNetworkManager {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isRunning = false;

    private final String serverIp;
    private final int serverPort;
    private final ClientGameState gameState;
    private final PacketParser parser;

    // Callback untuk notifikasi UI jika koneksi putus tiba-tiba
    private Runnable onDisconnect;

    public ClientNetworkManager(String ip, int port, ClientGameState gameState) {
        this.serverIp = ip;
        this.serverPort = port;
        this.gameState = gameState;
        this.parser = new PacketParser(gameState);
    }

    /**
     * Mencoba connect ke server. Blocking method (akan diam sampai connect/gagal).
     * @return true jika berhasil, throw Exception jika gagal.
     */
    public void connect() throws IOException {
        System.out.println("Connecting to " + serverIp + ":" + serverPort + "...");
        
        // 1. Buka Socket TCP
        socket = new Socket(serverIp, serverPort);
        
        // 2. Setup Input/Output Streams
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        isRunning = true;
        System.out.println("Connected to Server!");

        // 3. Mulai Thread untuk mendengarkan pesan dari Server terus menerus
        startListenThread();
    }

    private void startListenThread() {
        new Thread(() -> {
            try {
                String message;
                // Loop: Baca pesan baris per baris dari Server
                while (isRunning && (message = in.readLine()) != null) {
                    // System.out.println("RECV: " + message); // Debug
                    parser.parse(message);
                }
            } catch (IOException e) {
                System.err.println("Connection lost: " + e.getMessage());
            } finally {
                close();
            }
        }).start();
    }

    // Mengirim pesan String ke Server
    public void send(String message) {
        if (socket != null && !socket.isClosed()) {
            out.println(message);
        }
    }

    public void close() {
        isRunning = false;
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (onDisconnect != null) {
            onDisconnect.run();
        }
    }

    public void setOnDisconnect(Runnable onDisconnect) {
        this.onDisconnect = onDisconnect;
    }
}