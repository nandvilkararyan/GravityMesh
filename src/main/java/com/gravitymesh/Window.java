package com.gravitymesh;

import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private final int W = 1280, H = 800;
    private       long    handle;
    private       Mesh    mesh;
    private       UIPanel panel;
    private       Camera  camera;

    public void run() {
        init();
        loop();
        glfwDestroyWindow(handle);
        glfwTerminate();
    }

    private void init() {
        glfwInit();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_SAMPLES, 4);

        handle = glfwCreateWindow(W, H, "Gravity Mesh", NULL, NULL);
        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);
        GL.createCapabilities();

        glClearColor(0.02f, 0.02f, 0.08f, 1f);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

        mesh   = new Mesh(60, 60, 14f);
        panel  = new UIPanel(W, H);
        camera = new Camera();

        registerCallbacks();
    }

    private void registerCallbacks() {

        // ── Cursor position → camera orbit/pan ──────────────────────────
        final float[] mousePos = {0, 0};

        glfwSetCursorPosCallback(handle, (win, x, y) -> {
            float mx = (float) x;
            float my = (float) y;
            camera.onMouseMove(mx, my);
            mousePos[0] = mx;
            mousePos[1] = my;
        });

        // ── Mouse buttons ────────────────────────────────────────────────
        glfwSetMouseButtonCallback(handle, (win, button, action, mods) -> {
            float mx = mousePos[0];
            float my = mousePos[1];

            // Always pass right + middle buttons to camera
            if (button == GLFW_MOUSE_BUTTON_RIGHT ||
                    button == GLFW_MOUSE_BUTTON_MIDDLE) {
                camera.onMouseButton(button, action, mx, my);
                return;
            }

            // Left click: offer to UI panel first
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {

                if (panel.handleClick(mx, my)) return; // consumed by UI

                // Click on mesh — unproject screen → world
                float[] world = screenToWorld(mx, my);
                mesh.addPlanet(new Planet(panel.getSelected(), world[0], world[1]));
            }
        });

        // ── Scroll wheel → zoom ───────────────────────────────────────────
        glfwSetScrollCallback(handle, (win, xOffset, yOffset) -> {
            camera.onScroll((float) yOffset);
        });

        // ── Keyboard ──────────────────────────────────────────────────────
        glfwSetKeyCallback(handle, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(win, true);
            }
            // R → reset planets
            if (key == GLFW_KEY_R && action == GLFW_PRESS) {
                mesh.clearPlanets();
            }
            // F → reset camera to default view
            if (key == GLFW_KEY_F && action == GLFW_PRESS) {
                camera.yaw      = 0f;
                camera.pitch    = 38f;
                camera.distance = 16f;
                camera.panX     = 0f;
                camera.panY     = 0f;
            }
        });
    }

    /**
     * Converts screen pixel coordinates to approximate mesh world XZ coords.
     * Uses a ray-plane intersection against Y=0.
     */
    private float[] screenToWorld(float mx, float my) {
        // Normalized device coordinates
        float ndcX =  (mx / W) * 2f - 1f;
        float ndcY = -((my / H) * 2f - 1f);

        // Aspect & FOV
        float aspect = (float) W / H;
        float fovY   = (float) Math.toRadians(60.0);
        float tanHalf = (float) Math.tan(fovY / 2.0);

        // View-space ray direction
        float rayVx = ndcX * aspect * tanHalf;
        float rayVy = ndcY * tanHalf;
        float rayVz = -1f;

        // Reconstruct camera axes from current camera state
        float pitchRad = (float) Math.toRadians(camera.pitch);
        float yawRad   = (float) Math.toRadians(camera.yaw);

        float camX = camera.panX + camera.distance * (float)(Math.sin(yawRad) * Math.cos(pitchRad));
        float camY =               camera.distance * (float)(Math.sin(pitchRad));
        float camZ = camera.panY + camera.distance * (float)(Math.cos(yawRad) * Math.cos(pitchRad));

        // Forward
        float fx = camera.panX - camX;
        float fy =             - camY;
        float fz = camera.panY - camZ;
        float fLen = (float) Math.sqrt(fx*fx + fy*fy + fz*fz);
        fx /= fLen; fy /= fLen; fz /= fLen;

        // Right
        float rx = fy * 0 - fz * 1;
        float ry = fz * 0 - fx * 0;
        float rz = fx * 1 - fy * 0;
        float rLen = (float) Math.sqrt(rx*rx + ry*ry + rz*rz);
        rx /= rLen; ry /= rLen; rz /= rLen;

        // Up = right × forward
        float ux = ry*fz - rz*fy;
        float uy = rz*fx - rx*fz;
        float uz = rx*fy - ry*fx;

        // World-space ray direction
        float rdx = rayVx*rx + rayVy*ux + rayVz*fx;
        float rdy = rayVx*ry + rayVy*uy + rayVz*fy;
        float rdz = rayVx*rz + rayVy*uz + rayVz*fz;

        // Intersect ray with Y=0 plane
        // Ray: P = camPos + t * dir
        // At Y=0: camY + t * rdy = 0 → t = -camY / rdy
        float worldX, worldZ;
        if (Math.abs(rdy) > 0.0001f) {
            float t = -camY / rdy;
            worldX = camX + t * rdx;
            worldZ = camZ + t * rdz;
        } else {
            // Ray nearly parallel to plane — fall back to center
            worldX = 0;
            worldZ = 0;
        }

        // Clamp to mesh bounds
        float half = mesh.getSize() / 2f - 0.5f;
        worldX = Math.max(-half, Math.min(half, worldX));
        worldZ = Math.max(-half, Math.min(half, worldZ));

        return new float[]{worldX, worldZ};
    }

    private void loop() {
        while (!glfwWindowShouldClose(handle)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // ── 3D scene ─────────────────────────────────────────────────
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            float aspect = (float) W / H;
            double fovY = Math.toRadians(60.0);
            double f    = 1.0 / Math.tan(fovY / 2.0);
            float near  = 0.1f, far = 200f;
            float[] proj = {
                    (float)(f / aspect), 0, 0, 0,
                    0, (float)f, 0, 0,
                    0, 0, (far + near) / (near - far), -1,
                    0, 0, (2 * far * near) / (near - far), 0
            };
            glLoadMatrixf(proj);

            glMatrixMode(GL_MODELVIEW);
            camera.applyViewMatrix();

            mesh.update();
            mesh.render();

            // ── 2D overlay ────────────────────────────────────────────────
            glDisable(GL_DEPTH_TEST);
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glOrtho(0, W, H, 0, -1, 1);
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();

            panel.render();
            drawControlsHUD();

            glEnable(GL_DEPTH_TEST);

            glfwSwapBuffers(handle);
            glfwPollEvents();
        }
    }

    /** Draws a small controls legend in the top-left corner */
    private void drawControlsHUD() {
        int lines = 5;
        float panelW = 230, panelH = lines * 18 + 20;
        float px = 10, py = 10;

        // Background
        glColor4f(0.04f, 0.06f, 0.14f, 0.82f);
        glBegin(GL_QUADS);
        glVertex2f(px, py);
        glVertex2f(px + panelW, py);
        glVertex2f(px + panelW, py + panelH);
        glVertex2f(px, py + panelH);
        glEnd();

        // Border
        glColor3f(0.2f, 0.45f, 0.75f);
        glLineWidth(0.5f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(px, py);
        glVertex2f(px + panelW, py);
        glVertex2f(px + panelW, py + panelH);
        glVertex2f(px, py + panelH);
        glEnd();

        // Color-coded dots + line stubs as a legend
        float[][] dotColors = {
                {0.3f, 0.7f, 1.0f},  // left click   → blue
                {0.6f, 0.3f, 1.0f},  // right drag   → purple
                {0.3f, 1.0f, 0.6f},  // scroll       → green
                {1.0f, 0.75f, 0.2f}, // middle drag  → amber
                {1.0f, 0.35f, 0.3f}  // R / F keys   → red
        };
        float[] lineWidths = {55f, 75f, 40f, 60f, 70f}; // visual bar lengths

        float startY = py + 16;
        for (int i = 0; i < lines; i++) {
            float cy = startY + i * 18;
            float[] c = dotColors[i];

            // Dot
            glColor3f(c[0], c[1], c[2]);
            glBegin(GL_TRIANGLE_FAN);
            glVertex2f(px + 16, cy);
            for (int k = 0; k <= 12; k++) {
                double a = Math.PI * 2 * k / 12;
                glVertex2f(px + 16 + (float)Math.cos(a)*4,
                        cy       + (float)Math.sin(a)*4);
            }
            glEnd();

            // Text stub bar (stands in for text)
            glColor4f(c[0], c[1], c[2], 0.35f);
            glBegin(GL_QUADS);
            glVertex2f(px + 26, cy - 3);
            glVertex2f(px + 26 + lineWidths[i], cy - 3);
            glVertex2f(px + 26 + lineWidths[i], cy + 3);
            glVertex2f(px + 26, cy + 3);
            glEnd();
        }
    }
}