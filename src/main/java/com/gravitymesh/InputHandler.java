package com.gravitymesh;

import org.lwjgl.glfw.*;
import static org.lwjgl.glfw.GLFW.*;

public class InputHandler {

    private final long   window;
    private final UIPanel panel;
    private final int    screenW, screenH;

    // The mesh world spans from -meshHalf to +meshHalf on X and Z
    private final float meshHalf;

    // Callback storage
    private float  lastMouseX, lastMouseY;
    private boolean clickConsumedByUI = false;

    // Listener that Window.java registers to receive placed planets
    public interface PlacementListener {
        void onPlanetPlaced(PlanetType type, float worldX, float worldZ);
    }

    private PlacementListener listener;

    public InputHandler(long windowHandle, UIPanel panel,
                        int screenW, int screenH, float meshHalf) {
        this.window   = windowHandle;
        this.panel    = panel;
        this.screenW  = screenW;
        this.screenH  = screenH;
        this.meshHalf = meshHalf;
    }

    public void setPlacementListener(PlacementListener l) {
        this.listener = l;
    }

    public void register() {

        // Track cursor position
        glfwSetCursorPosCallback(window, (win, x, y) -> {
            lastMouseX = (float) x;
            lastMouseY = (float) y;
        });

        // Mouse button callback
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {

                float mx = lastMouseX;
                float my = lastMouseY;

                // First offer to panel — if it consumes the click, stop
                if (panel.handleClick(mx, my)) {
                    clickConsumedByUI = true;
                    return;
                }
                clickConsumedByUI = false;

                // Click was on the mesh viewport — convert to world coords
                // The mesh is rendered with a perspective+rotation transform.
                // We do a simplified top-down unprojection here:
                // map screen X → world X, screen Y → world Z
                // (works well for the fixed 40° tilt camera we set up)
                float normX = (mx / screenW) * 2f - 1f;        // -1 to +1
                float normY = 1f - (my / screenH) * 2f;        // -1 to +1 (flipped)

                // Empirical scale for our fixed camera (adjust if camera changes)
                float worldX =  normX * meshHalf * 1.05f;
                float worldZ = -normY * meshHalf * 1.35f + meshHalf * 0.15f;

                // Clamp to mesh bounds
                worldX = Math.max(-meshHalf + 0.5f, Math.min(meshHalf - 0.5f, worldX));
                worldZ = Math.max(-meshHalf + 0.5f, Math.min(meshHalf - 0.5f, worldZ));

                if (listener != null) {
                    listener.onPlanetPlaced(panel.getSelected(), worldX, worldZ);
                }
            }
        });

        // ESC to close
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(win, true);
            }
            // R to reset all planets
            if (key == GLFW_KEY_R && action == GLFW_PRESS) {
                if (listener != null) listener.onPlanetPlaced(null, 0, 0);
            }
        });
    }
}