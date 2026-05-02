package com.gravitymesh;

import java.util.ArrayList;
import java.util.List;
import static org.lwjgl.opengl.GL11.*;

public class Mesh {

    private final int   cols, rows;
    private final float size;
    private final float cellSize;
    private       float[][] heights;

    private List<Planet> planets = new ArrayList<>();

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

    public float getSize() { return size; }

    public void addPlanet(Planet p)    { planets.add(p); }
    public void clearPlanets()         { planets.clear(); }
    public List<Planet> getPlanets()   { return planets; }

    public void update() {
        if (planets.isEmpty()) return;

        // Mutual gravity between planets
        for (int i = 0; i < planets.size(); i++) {
            Planet a  = planets.get(i);
            float ax = 0, az = 0;

            for (int j = 0; j < planets.size(); j++) {
                if (i == j) continue;
                Planet b   = planets.get(j);
                float dx   = b.x - a.x;
                float dz   = b.z - a.z;
                float dist2 = dx * dx + dz * dz + 0.8f;
                float dist  = (float) Math.sqrt(dist2);
                float force = G * b.mass / dist2;
                ax += force * dx / dist;
                az += force * dz / dist;
            }
            a.vx += ax * DT;
            a.vz += az * DT;
        }

        for (Planet p : planets) {
            p.x += p.vx * DT;
            p.z += p.vz * DT;
            // Soft boundary — bounce back gently at mesh edge
            float half = size / 2f - 0.5f;
            if (p.x >  half) { p.x =  half; p.vx *= -0.4f; }
            if (p.x < -half) { p.x = -half; p.vx *= -0.4f; }
            if (p.z >  half) { p.z =  half; p.vz *= -0.4f; }
            if (p.z < -half) { p.z = -half; p.vz *= -0.4f; }
        }

        // Recompute mesh deformation
        float half = size / 2f;
        for (int col = 0; col <= cols; col++) {
            for (int row = 0; row <= rows; row++) {
                float wx = -half + col * cellSize;
                float wz = -half + row * cellSize;
                float dip = 0f;
                for (Planet p : planets) {
                    float dx   = wx - p.x;
                    float dz   = wz - p.z;
                    float dist2 = dx * dx + dz * dz;
                    // Gaussian dip — sharper for small planets, wider for large
                    float spread = 0.25f + p.mass * 0.04f;
                    dip += -p.mass / (1f + dist2 * spread);
                }
                heights[col][row] = Math.max(dip, MAX_DIP);
            }
        }
    }

    public void render() {
        float half = size / 2f;

        // ── Wireframe grid ──────────────────────────────────────────────
        glLineWidth(1.0f);

        // Horizontal lines
        for (int row = 0; row <= rows; row++) {
            glBegin(GL_LINE_STRIP);
            for (int col = 0; col <= cols; col++) {
                float x = -half + col * cellSize;
                float y =  heights[col][row];
                float z = -half + row * cellSize;
                // Color: cyan at flat, shifts toward white near deep dip
                float depth = Math.abs(y / MAX_DIP);
                glColor3f(0.2f + depth * 0.6f,
                        0.7f + depth * 0.2f,
                        1.0f);
                glVertex3f(x, y, z);
            }
            glEnd();
        }

        // Vertical lines
        for (int col = 0; col <= cols; col++) {
            glBegin(GL_LINE_STRIP);
            for (int row = 0; row <= rows; row++) {
                float x = -half + col * cellSize;
                float y =  heights[col][row];
                float z = -half + row * cellSize;
                float depth = Math.abs(y / MAX_DIP);
                glColor3f(0.2f + depth * 0.6f,
                        0.7f + depth * 0.2f,
                        1.0f);
                glVertex3f(x, y, z);
            }
            glEnd();
        }

        // ── Planets ──────────────────────────────────────────────────────
        for (Planet p : planets) {
            float py = getPlanetSurfaceY(p);
            drawPlanet(p, py);
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

    private void drawPlanet(Planet p, float y) {
        float r = p.renderRadius;
        int stacks = 14, slices = 14;

        // Glow halo
        glColor4f(p.r, p.g, p.b, 0.12f);
        drawSphereAt(p.x, y + r, p.z, r * 1.8f, 10, 10);

        // Planet body
        glColor3f(p.r, p.g, p.b);
        drawSphereAt(p.x, y + r, p.z, r, stacks, slices);

        // Saturn ring
        if (p.name.equals("Saturn")) {
            glColor3f(0.88f, 0.80f, 0.58f);
            glLineWidth(2.0f);
            drawRing(p.x, y + r, p.z, r * 1.5f, r * 2.2f, 40);
        }
    }

    private void drawSphereAt(float cx, float cy, float cz,
                              float r, int stacks, int slices) {
        for (int i = 0; i < stacks; i++) {
            double lat0 = Math.PI * (-0.5 + (double) i       / stacks);
            double lat1 = Math.PI * (-0.5 + (double)(i + 1)  / stacks);
            double sl0  = Math.sin(lat0), cl0 = Math.cos(lat0);
            double sl1  = Math.sin(lat1), cl1 = Math.cos(lat1);
            glBegin(GL_QUAD_STRIP);
            for (int j = 0; j <= slices; j++) {
                double lng = 2 * Math.PI * j / slices;
                double cl  = Math.cos(lng), sl = Math.sin(lng);
                glVertex3f(cx+(float)(r*cl0*cl), cy+(float)(r*sl0), cz+(float)(r*cl0*sl));
                glVertex3f(cx+(float)(r*cl1*cl), cy+(float)(r*sl1), cz+(float)(r*cl1*sl));
            }
            glEnd();
        }
    }

    private void drawRing(float cx, float cy, float cz,
                          float innerR, float outerR, int segs) {
        glBegin(GL_LINES);
        for (int i = 0; i < segs; i++) {
            double a = Math.PI * 2 * i / segs;
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            glVertex3f(cx + innerR * cos, cy, cz + innerR * sin);
            glVertex3f(cx + outerR * cos, cy, cz + outerR * sin);
        }
        glEnd();
    }
}