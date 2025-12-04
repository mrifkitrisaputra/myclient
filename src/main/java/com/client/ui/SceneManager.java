package com.client.ui;

import com.client.render.GameCanvas;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.util.function.Consumer;

public class SceneManager {
    private static Stage stage;

    public static void setStage(Stage s) {
        stage = s;
    }

    public static void toLogin(Consumer<String> onConnect) {
        LoginScene login = new LoginScene(onConnect);
        stage.setScene(login.getScene());
        stage.setTitle("Bomber Client - Login");
    }

    public static void toLobby() {
        LobbyScene lobby = new LobbyScene();
        stage.setScene(lobby.getScene());
        stage.setTitle("Bomber Game - Lobby");
    }

    public static void toRoom(String roomName) {
        RoomScene room = new RoomScene(roomName);
        stage.setScene(room.getScene());
        stage.setTitle("Room: " + roomName);
    }

    public static void toGame(GameCanvas canvas) {
        // Bungkus canvas dalam StackPane biar rapi
        StackPane root = new StackPane(canvas);
        
        // Listener keyboard harus dipasang di Scene barunya
        Scene gameScene = new Scene(root);
        
        // Hubungkan InputHandler (Logic ini nanti diintegrasikan di App.java / InputSender)
        // gameScene.setOnKeyPressed(...) 
        
        // Resize Canvas otomatis
        gameScene.widthProperty().addListener((obs, oldV, newV) -> canvas.setWidth(newV.doubleValue()));
        gameScene.heightProperty().addListener((obs, oldV, newV) -> canvas.setHeight(newV.doubleValue()));

        stage.setScene(gameScene);
        stage.setTitle("Bomber Game - Playing");
        canvas.requestFocus(); // Penting biar bisa terima input keyboard
    }
}