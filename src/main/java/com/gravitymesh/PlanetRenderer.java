package com.gravitymesh;

import static org.lwjgl.opengl.GL11.*;

public class PlanetRenderer {

    public void draw(Planet p, float surfaceY) {
        float cy = surfaceY + p.renderRadius;

        glPushMatrix();
        glTranslatef(p.x, cy, p.z);

        // ── Draw order matters ──────────────────────────────────────────
        // 1. Solid opaque body FIRST (writes to depth buffer)
        // 2. Wireframe lines on top (no depth write issues, fully opaque)
        // 3. Atmosphere LAST (translucent, behind nothing solid)
        // 4. Special features (rings, corona) last

        drawSpinningBody(p);

        if (p.name.equals("Saturn"))  drawSaturnRings(p);
        if (p.name.equals("Sun"))     drawSolarCorona(p);

        drawRotationAxis(p);
        drawAtmosphere(p);   // drawn LAST so it layers over, not under

        glPopMatrix();
    }

    // ── Atmosphere drawn AFTER body ───────────────────────────────────────

    private void drawAtmosphere(Planet p) {
        // Disable depth writes so atmosphere doesn't block anything behind it
        glDepthMask(false);

        float r = p.renderRadius;
        // Only 2 layers, lower alpha, only slightly larger than body
        glColor4f(p.r, p.g, p.b, 0.08f);
        drawSphere(r * 1.20f, 10, 10);
        glColor4f(p.r, p.g, p.b, 0.04f);
        drawSphere(r * 1.40f, 10, 10);

        // Special: Sun gets a brighter, wider corona glow
        if (p.name.equals("Sun")) {
            glColor4f(1.0f, 0.8f, 0.2f, 0.10f);
            drawSphere(r * 1.60f, 10, 10);
        }

        // Special: Venus gets thick atmosphere glow
        if (p.name.equals("Venus")) {
            glColor4f(0.95f, 0.85f, 0.55f, 0.10f);
            drawSphere(r * 1.35f, 10, 10);
        }

        glDepthMask(true);  // re-enable depth writes
    }

    // ── Solid spinning body ───────────────────────────────────────────────

    private void drawSpinningBody(Planet p) {
        glPushMatrix();
        glRotatef(p.axialTilt,     0f, 0f, 1f);  // tilt axis
        glRotatef(p.rotationAngle, 0f, 1f, 0f);  // spin

        drawPlanetSurface(p);   // opaque bands — fully solid
        drawLongitudeLines(p);  // wireframe on top — fully opaque

        glPopMatrix();
    }

    private void drawPlanetSurface(Planet p) {
        int stacks = 20, slices = 20;
        float r = p.renderRadius;

        for (int i = 0; i < stacks; i++) {
            double lat0 = Math.PI * (-0.5 + (double) i      / stacks);
            double lat1 = Math.PI * (-0.5 + (double)(i + 1) / stacks);
            double sl0  = Math.sin(lat0), cl0 = Math.cos(lat0);
            double sl1  = Math.sin(lat1), cl1 = Math.cos(lat1);

            float[] c = getBandColor(p, i, stacks);

            // Full alpha = 1.0 — completely solid, no transparency
            glColor3f(c[0], c[1], c[2]);

            glBegin(GL_QUAD_STRIP);
            for (int j = 0; j <= slices; j++) {
                double lng = 2 * Math.PI * j / slices;
                double cl  = Math.cos(lng), sl = Math.sin(lng);
                glVertex3f((float)(r*cl0*cl), (float)(r*sl0), (float)(r*cl0*sl));
                glVertex3f((float)(r*cl1*cl), (float)(r*sl1), (float)(r*cl1*sl));
            }
            glEnd();
        }
    }

    private void drawLongitudeLines(Planet p) {
        float r = p.renderRadius * 1.001f; // tiny offset so lines sit on surface
        int stacks = 20, lineSlices = 8;

        // Use a darker tint of the planet color — fully opaque, no alpha
        glColor3f(p.r * 0.45f, p.g * 0.45f, p.b * 0.45f);
        glLineWidth(0.6f);

        for (int j = 0; j < lineSlices; j++) {
            double lng = 2 * Math.PI * j / lineSlices;
            float cl   = (float) Math.cos(lng);
            float sl   = (float) Math.sin(lng);

            glBegin(GL_LINE_STRIP);
            for (int i = 0; i <= stacks; i++) {
                double lat  = Math.PI * (-0.5 + (double) i / stacks);
                float  slat = (float) Math.sin(lat);
                float  clat = (float) Math.cos(lat);
                glVertex3f(r * clat * cl, r * slat, r * clat * sl);
            }
            glEnd();
        }
    }

    // ── Band color per planet ─────────────────────────────────────────────

    private float[] getBandColor(Planet p, int band, int total) {
        float t = (float) band / total;

        return switch (p.name) {
            case "Jupiter" -> {
                boolean dark = (band % 3 == 1);
                yield dark
                        ? new float[]{0.72f, 0.52f, 0.35f}
                        : new float[]{0.93f, 0.82f, 0.65f};
            }
            case "Saturn" -> {
                boolean dark = (band % 4 == 2);
                yield dark
                        ? new float[]{0.82f, 0.72f, 0.45f}
                        : new float[]{0.96f, 0.90f, 0.70f};
            }
            case "Uranus" -> new float[]{
                    0.42f + t * 0.10f,
                    0.82f,
                    0.88f
            };
            case "Neptune" -> {
                float eq = 1f - Math.abs(t - 0.5f) * 2f;
                yield new float[]{
                        0.15f + eq * 0.12f,
                        0.28f + eq * 0.18f,
                        0.90f
                };
            }
            case "Earth" -> {
                boolean pole = t < 0.12f || t > 0.88f;
                yield pole
                        ? new float[]{0.93f, 0.96f, 1.00f}
                        : new float[]{0.22f, 0.52f, 0.93f};
            }
            case "Mars" -> new float[]{
                    0.82f + t * 0.06f,
                    0.32f + t * 0.08f,
                    0.18f
            };
            case "Mercury" -> new float[]{
                    0.65f + t * 0.08f,
                    0.62f + t * 0.08f,
                    0.60f + t * 0.08f
            };
            case "Sun" -> {
                float eq = 1f - Math.abs(t - 0.5f) * 1.6f;
                eq = Math.max(0f, eq);
                yield new float[]{
                        1.00f,
                        0.70f + eq * 0.25f,
                        0.05f + eq * 0.20f
                };
            }
            default -> new float[]{p.r, p.g, p.b};
        };
    }

    // ── Saturn rings ──────────────────────────────────────────────────────

    private void drawSaturnRings(Planet p) {
        glPushMatrix();
        glRotatef(p.axialTilt, 0f, 0f, 1f);
        glRotatef(p.rotationAngle * 0.3f, 0f, 1f, 0f);

        glDepthMask(false); // rings are semi-transparent, don't block planet

        float innerR = p.renderRadius * 1.35f;
        float outerR = p.renderRadius * 2.30f;
        int   segs   = 80;

        float[][] bands = {
                {0.88f, 0.82f, 0.60f, 0.70f},
                {0.70f, 0.64f, 0.44f, 0.45f},
                {0.93f, 0.87f, 0.65f, 0.80f},
                {0.60f, 0.54f, 0.36f, 0.30f},
        };
        float[] radii = {innerR, innerR * 1.4f, innerR * 1.7f, outerR};

        for (int ring = 0; ring < bands.length - 1; ring++) {
            float r1 = radii[ring];
            float r2 = radii[ring + 1];
            float[] c = bands[ring];
            glColor4f(c[0], c[1], c[2], c[3]);
            glBegin(GL_QUAD_STRIP);
            for (int j = 0; j <= segs; j++) {
                double a = Math.PI * 2 * j / segs;
                float  cos = (float) Math.cos(a);
                float  sin = (float) Math.sin(a);
                glVertex3f(r1 * cos, 0f, r1 * sin);
                glVertex3f(r2 * cos, 0f, r2 * sin);
            }
            glEnd();
        }

        glDepthMask(true);
        glPopMatrix();
    }

    // ── Solar corona ──────────────────────────────────────────────────────

    private void drawSolarCorona(Planet p) {
        glDepthMask(false);
        float r = p.renderRadius;
        float t = (float)(System.currentTimeMillis() % 4000) / 4000f;

        glColor4f(1.0f, 0.65f, 0.05f, 0.18f);
        glLineWidth(1.5f);
        int rays = 16;
        glBegin(GL_LINES);
        for (int i = 0; i < rays; i++) {
            double angle  = Math.PI * 2 * i / rays + t * Math.PI * 0.5;
            float  length = r * (1.5f + 0.4f * (float)Math.sin(t * Math.PI * 2 + i));
            glVertex3f(0, 0, 0);
            glVertex3f((float)Math.cos(angle) * length,
                    (float)Math.sin(angle) * 0.3f * length,
                    (float)Math.sin(angle) * length);
        }
        glEnd();
        glDepthMask(true);
    }

    // ── Rotation axis ─────────────────────────────────────────────────────

    private void drawRotationAxis(Planet p) {
        float len     = p.renderRadius * 1.7f;
        float tiltRad = (float) Math.toRadians(p.axialTilt);
        float ax      = (float) Math.sin(tiltRad) * len;
        float ay      = (float) Math.cos(tiltRad) * len;

        glColor3f(0.85f, 0.85f, 0.85f);   // solid white, fully visible
        glLineWidth(1.2f);
        glBegin(GL_LINES);
        glVertex3f( ax,  ay, 0f);
        glVertex3f(-ax, -ay, 0f);
        glEnd();

        // North pole dot
        glPointSize(3.5f);
        glColor3f(1f, 1f, 1f);
        glBegin(GL_POINTS);
        glVertex3f(ax, ay, 0f);
        glEnd();
    }

    // ── Sphere helper ─────────────────────────────────────────────────────

    private void drawSphere(float r, int stacks, int slices) {
        for (int i = 0; i < stacks; i++) {
            double lat0 = Math.PI * (-0.5 + (double) i      / stacks);
            double lat1 = Math.PI * (-0.5 + (double)(i + 1) / stacks);
            double sl0  = Math.sin(lat0), cl0 = Math.cos(lat0);
            double sl1  = Math.sin(lat1), cl1 = Math.cos(lat1);
            glBegin(GL_QUAD_STRIP);
            for (int j = 0; j <= slices; j++) {
                double lng = 2 * Math.PI * j / slices;
                double c   = Math.cos(lng), s = Math.sin(lng);
                glVertex3f((float)(r*cl0*c), (float)(r*sl0), (float)(r*cl0*s));
                glVertex3f((float)(r*cl1*c), (float)(r*sl1), (float)(r*cl1*s));
            }
            glEnd();
        }
    }
}