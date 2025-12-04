package com.client.network;

public class InputSender {

    private final ClientNetworkManager network;

    public InputSender(ClientNetworkManager network) {
        this.network = network;
    }

    // Format Protocol: INPUT;KEY;IS_PRESSED
    // Contoh: INPUT;UP;TRUE  (Tombol atas ditekan)
    // Contoh: INPUT;UP;FALSE (Tombol atas dilepas)
    
    public void sendInput(String key, boolean isPressed) {
        String status = isPressed ? "TRUE" : "FALSE";
        network.send("INPUT;" + key + ";" + status);
    }
    
    public void sendPlaceBomb() {
        network.send("ACTION;PLACE_BOMB");
    }
}