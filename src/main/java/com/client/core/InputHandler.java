package com.client.core;

import java.util.HashSet;
import java.util.Set;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class InputHandler {

    private final Set<KeyCode> pressedKeys = new HashSet<>();

    public void keyDown(KeyEvent e) {
        pressedKeys.add(e.getCode());
    }

    public void keyUp(KeyEvent e) {
        pressedKeys.remove(e.getCode());
    }

    // Getter
    public boolean isUp() {
        return pressedKeys.contains(KeyCode.W) || pressedKeys.contains(KeyCode.UP);
    }

    public boolean isDown() {
        return pressedKeys.contains(KeyCode.S) || pressedKeys.contains(KeyCode.DOWN);
    }

    public boolean isLeft() {
        return pressedKeys.contains(KeyCode.A) || pressedKeys.contains(KeyCode.LEFT);
    }

    public boolean isRight() {
        return pressedKeys.contains(KeyCode.D) || pressedKeys.contains(KeyCode.RIGHT);
    }

    public boolean isPlace() {
        return pressedKeys.contains(KeyCode.SPACE);
    }
    
    public boolean isESC() {
        return pressedKeys.contains(KeyCode.ESCAPE);
    }
}