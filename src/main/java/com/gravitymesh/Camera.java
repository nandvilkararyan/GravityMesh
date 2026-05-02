package com.gravitymesh;

import static org.lwjgl.opengl.GL11.*;

public class Camera {

    // Orbit angles (degrees)
    public float yaw   = 0f;     // left/right rotation
    public float pitch = 38f;    // up/down tilt (start matching original view)

    // Zoom
    public float distance = 16f;  // distance from target

    // Pan offset (target point the camera orbits around)
    public float panX = 0f;
    public float panY = 0f;

    // Constraints
    private static final float MIN_PITCH    =  5f;
    private static final float MAX_PITCH    = 89f;
    private static final float MIN_DISTANCE =  3f;
    private static final float MAX_DISTANCE = 60f;

    // Sensitivity
    private static final float ORBIT_SENSITIVITY = 0.35f;
    private static final float PAN_SENSITIVITY   = 0.012f;
    private static final float ZOOM_SENSITIVITY  = 1.2f;

    // Drag tracking
    private boolean rightDragging  = false;
    private boolean middleDragging = false;
    private float   lastX, lastY;

    // ── Input handlers ────────────────────────────────────────────────────

    public void onMouseButton(int button, int action, float mx, float my) {
        // GLFW button constants: 1 = right, 2 = middle
        if (button == 1) {
            rightDragging  = (action == 1);
            lastX = mx; lastY = my;
        }
        if (button == 2) {
            middleDragging = (action == 1);
            lastX = mx; lastY = my;
        }
    }

    public void onMouseMove(float mx, float my) {
        float dx = mx - lastX;
        float dy = my - lastY;

        if (rightDragging) {
            // Orbit: horizontal drag → yaw, vertical drag → pitch
            yaw   -= dx * ORBIT_SENSITIVITY;
            pitch += dy * ORBIT_SENSITIVITY;
            pitch  = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch));
        }

        if (middleDragging) {
            // Pan: move target point in camera-local XZ plane
            float yawRad = (float) Math.toRadians(yaw);

            // Camera right and forward vectors (flat, no Y)
            float rightX  =  (float) Math.cos(yawRad);
            float rightZ  =  (float) Math.sin(yawRad);
            float forwardX = (float) Math.sin(yawRad);
            float forwardZ = -(float) Math.cos(yawRad);

            panX -= (rightX * dx - forwardX * dy) * PAN_SENSITIVITY * (distance / 10f);
            panY -= (rightZ * dx - forwardZ * dy) * PAN_SENSITIVITY * (distance / 10f);
        }

        lastX = mx;
        lastY = my;
    }

    public void onScroll(float yOffset) {
        distance -= yOffset * ZOOM_SENSITIVITY;
        distance = Math.max(MIN_DISTANCE, Math.min(MAX_DISTANCE, distance));
    }

    // ── Apply to OpenGL modelview matrix ─────────────────────────────────

    /**
     * Call this every frame after glLoadIdentity().
     * Sets up the view transform for the 3D scene.
     */
    public void applyViewMatrix() {
        float pitchRad = (float) Math.toRadians(pitch);
        float yawRad   = (float) Math.toRadians(yaw);

        // Camera position in spherical coordinates around target
        float camX = panX + distance * (float)(Math.sin(yawRad) * Math.cos(pitchRad));
        float camY =        distance * (float)(Math.sin(pitchRad));
        float camZ = panY + distance * (float)(Math.cos(yawRad) * Math.cos(pitchRad));

        // Compute forward (from camera to target)
        float fx = panX - camX;
        float fy =      - camY;
        float fz = panY - camZ;
        float fLen = (float) Math.sqrt(fx*fx + fy*fy + fz*fz);
        fx /= fLen; fy /= fLen; fz /= fLen;

        // World up
        float ux = 0, uy = 1, uz = 0;

        // Right = forward × up
        float rx = fy*uz - fz*uy;
        float ry = fz*ux - fx*uz;
        float rz = fx*uy - fy*ux;
        float rLen = (float) Math.sqrt(rx*rx + ry*ry + rz*rz);
        rx /= rLen; ry /= rLen; rz /= rLen;

        // Recompute up = right × forward
        ux = ry*fz - rz*fy;
        uy = rz*fx - rx*fz;
        uz = rx*fy - ry*fx;

        // Build column-major 4x4 view matrix (OpenGL format)
        float[] m = {
                rx,  ux, -fx, 0,
                ry,  uy, -fy, 0,
                rz,  uz, -fz, 0,
                -(rx*camX + ry*camY + rz*camZ),
                -(ux*camX + uy*camY + uz*camZ),
                (fx*camX + fy*camY + fz*camZ),
                1
        };

        glLoadMatrixf(m);
    }

    // ── Cursor hint ───────────────────────────────────────────────────────

    /** Returns a short string describing what will happen on drag */
    public String getCursorHint() {
        if (rightDragging)  return "Orbiting";
        if (middleDragging) return "Panning";
        return "Idle";
    }
}