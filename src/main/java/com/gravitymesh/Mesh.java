package com.gravitymesh;

import java.util.ArrayList;
import java.util.List;
import static org.lwjgl.opengl.GL11.*;

public class Mesh {

    private final int   cols, rows;
    private final float size;
    private final float cellSize;
    float[][] heights;   // package-visible for PlanetRenderer if needed

    private List<Planet>      planets        = new ArrayList<>();
    private PlanetRenderer    planetRenderer = new PlanetRenderer();  // NEW

    private static final float G       = 1.2f;
    private static final float MAX_DIP = -5.5f;
    private static final float DT      = 0.016f;

    public Mesh(int cols, int rows, float size) {
        this.cols     = cols;
        this.rows     = rows;
        this.size     = size;
        this.cellSize = size / cols;
        this.heights  = new float[cols + 1][rows + 1];
    }

    public float getSize()           { return size; }
    public void  addPlanet(Planet p) { planets.add(p); }
    public void  clearPlanets()      { planets.clear(); }
    public List<Planet> getPlanets() { return planets; }

    public void update() {
        if (planets.isEmpty()) return;

        // ── Mutual gravity (N-body) ────────────────────────────────────
        for (int i = 0; i < planets.size(); i++) {
            Planet a = planets.get(i);
            float  ax = 0, az = 0;

            for (int j = 0; j < planets.size(); j++) {
                if (i == j) continue;
                Planet b    = planets.get(j);
                float  dx   = b.x - a.x;
                float  dz   = b.z - a.z;
                float  d2   = dx*dx + dz*dz + 0.8f;
                float  dist = (float) Math.sqrt(d2);

                // Gravity scaled by BOTH mass and surface gravity
                // Surface gravity captures how strongly the body curves space
                // around itself beyond just raw mass
                float force = G * b.mass * b.surfaceGravity / d2;
                ax += force * dx / dist;
                az += force * dz / dist;
            }
            a.vx += ax * DT;
            a.vz += az * DT;
        }

        // ── Position integration ───────────────────────────────────────
        for (Planet p : planets) {
            p.x += p.vx * DT;
            p.z += p.vz * DT;

            // Soft boundary bounce
            float half = size / 2f - 0.5f;
            if (p.x >  half) { p.x =  half; p.vx *= -0.4f; }
            if (p.x < -half) { p.x = -half; p.vx *= -0.4f; }
            if (p.z >  half) { p.z =  half; p.vz *= -0.4f; }
            if (p.z < -half) { p.z = -half; p.vz *= -0.4f; }

            // ── Axial rotation update ──────────────────────────────────
            p.updateRotation();  // NEW — spin each planet every tick
        }

        // ── Mesh deformation ───────────────────────────────────────────
        // Now factors in surfaceGravity for warp depth
        float half = size / 2f;
        for (int col = 0; col <= cols; col++) {
            for (int row = 0; row <= rows; row++) {
                float wx  = -half + col * cellSize;
                float wz  = -half + row * cellSize;
                float dip = 0f;

                for (Planet p : planets) {
                    float dx   = wx - p.x;
                    float dz   = wz - p.z;
                    float d2   = dx*dx + dz*dz;

                    // spread: wider for massive + high-gravity bodies
                    float spread = 0.20f + (p.mass * 0.03f) + (p.surfaceGravity * 0.015f);

                    // depth: driven by both mass and surface gravity
                    float depth  = p.mass * (0.7f + p.surfaceGravity * 0.3f);

                    dip += -depth / (1f + d2 * spread);
                }

                heights[col][row] = Math.max(dip, MAX_DIP);
            }
        }
    }

    public void render() {
        float half = size / 2f;

        // ── Wireframe grid ─────────────────────────────────────────────
        glLineWidth(1.0f);

        for (int row = 0; row <= rows; row++) {
            glBegin(GL_LINE_STRIP);
            for (int col = 0; col <= cols; col++) {
                float x     = -half + col * cellSize;
                float y     = heights[col][row];
                float z     = -half + row * cellSize;
                float depth = Math.abs(y / MAX_DIP);
                glColor3f(0.2f + depth * 0.6f, 0.7f + depth * 0.2f, 1.0f);
                glVertex3f(x, y, z);
            }
            glEnd();
        }

        for (int col = 0; col <= cols; col++) {
            glBegin(GL_LINE_STRIP);
            for (int row = 0; row <= rows; row++) {
                float x     = -half + col * cellSize;
                float y     = heights[col][row];
                float z     = -half + row * cellSize;
                float depth = Math.abs(y / MAX_DIP);
                glColor3f(0.2f + depth * 0.6f, 0.7f + depth * 0.2f, 1.0f);
                glVertex3f(x, y, z);
            }
            glEnd();
        }

        // ── Planets ────────────────────────────────────────────────────
        for (Planet p : planets) {
            float surfaceY = getPlanetSurfaceY(p);
            planetRenderer.draw(p, surfaceY);   // delegates to PlanetRenderer
        }
    }

    private float getPlanetSurfaceY(Planet p) {
        float half = size / 2f;
        float fx   = (p.x + half) / cellSize;
        float fz   = (p.z + half) / cellSize;
        int   col  = Math.min(Math.max((int) fx, 0), cols);
        int   row  = Math.min(Math.max((int) fz, 0), rows);
        return heights[col][row];
    }
}