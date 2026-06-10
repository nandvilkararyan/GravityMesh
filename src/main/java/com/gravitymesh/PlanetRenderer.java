package com.gravitymesh;

import static org.lwjgl.opengl.GL11.*;

public class PlanetRenderer {

    // Shared geometry resolution — lower = faster, still looks good
    private static final int STACKS = 14;
    private static final int SLICES = 14;

    public void draw(Planet p, float surfaceY) {
        float cy = surfaceY + p.renderRadius;

        glPushMatrix();
        glTranslatef(p.x, cy, p.z);

        // Solid body first — depth write ON, blending OFF
        glDepthMask(true);
        glDisable(GL_BLEND);
        drawSpinningBody(p);

        // Transparent layers after — depth write OFF, blending ON
        glDepthMask(false);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        drawAtmosphere(p);

        if (p.name.equals("Saturn")) drawSaturnRings(p);
        if (p.name.equals("Sun"))    drawSolarCorona(p);

        // Restore
        glDepthMask(true);
        glEnable(GL_BLEND);

        glPopMatrix();
    }

    // ── Atmosphere ────────────────────────────────────────────────────────

    private void drawAtmosphere(Planet p) {
        // Single shell only — halves the atmosphere draw cost
        glColor4f(p.r, p.g, p.b, 0.08f);
        drawSphere(p.renderRadius * 1.20f * p.atmosphereScale, 8, 8);
    }

    // ── Spinning body ─────────────────────────────────────────────────────

    private void drawSpinningBody(Planet p) {
        glPushMatrix();
        glRotatef(p.axialTilt,     0f, 0f, 1f);
        glRotatef(p.rotationAngle, 0f, 1f, 0f);
        drawPlanetSurface(p);
        glPopMatrix();
    }

    private void drawPlanetSurface(Planet p) {
        float r = p.renderRadius;

        // One single QUAD_STRIP pass — no separate longitude lines,
        // no second loop. Shading + bands all in the same pass.
        for (int i = 0; i < STACKS; i++) {
            double lat0 = Math.PI * (-0.5 + (double) i       / STACKS);
            double lat1 = Math.PI * (-0.5 + (double)(i + 1)  / STACKS);
            double sl0  = Math.sin(lat0), cl0 = Math.cos(lat0);
            double sl1  = Math.sin(lat1), cl1 = Math.cos(lat1);

            float[] band = getBandColor(p, i, STACKS);

            glBegin(GL_QUAD_STRIP);
            for (int j = 0; j <= SLICES; j++) {
                double lng = 2 * Math.PI * j / SLICES;
                double cl  = Math.cos(lng), sl = Math.sin(lng);

                // Normals for Lambertian shading
                float nx0 = (float)(cl0 * cl), ny0 = (float) sl0, nz0 = (float)(cl0 * sl);
                float nx1 = (float)(cl1 * cl), ny1 = (float) sl1, nz1 = (float)(cl1 * sl);

                // Light from above-right — min 0.35 so dark side stays visible
                float d0 = Math.max(0.35f, nx0*0.4f + ny0*0.82f + nz0*0.3f);
                float d1 = Math.max(0.35f, nx1*0.4f + ny1*0.82f + nz1*0.3f);

                glColor3f(band[0]*d0, band[1]*d0, band[2]*d0);
                glVertex3f((float)(r*cl0*cl), (float)(r*sl0), (float)(r*cl0*sl));

                glColor3f(band[0]*d1, band[1]*d1, band[2]*d1);
                glVertex3f((float)(r*cl1*cl), (float)(r*sl1), (float)(r*cl1*sl));
            }
            glEnd();
        }
    }

    private float[] getBandColor(Planet p, int band, int totalBands) {
        float t = (float) band / totalBands;

        return switch (p.name) {
            case "Jupiter" -> (band % 3 == 1)
                    ? new float[]{ 0.68f, 0.50f, 0.32f }
                    : new float[]{ 0.90f, 0.78f, 0.60f };

            case "Saturn"  -> (band % 4 == 2)
                    ? new float[]{ 0.78f, 0.68f, 0.42f }
                    : new float[]{ 0.94f, 0.88f, 0.68f };

            case "Uranus"  -> new float[]{
                    0.38f + t*0.12f, 0.80f + t*0.10f, 0.88f };

            case "Neptune" -> {
                float eq = 1f - Math.abs(t - 0.5f) * 2f;
                yield new float[]{
                        0.12f + eq*0.18f,
                        0.28f + eq*0.22f,
                        0.88f + eq*0.12f };
            }
            case "Earth"   -> (t < 0.12f || t > 0.88f)
                    ? new float[]{ 0.92f, 0.96f, 1.00f }
                    : new float[]{ p.r,   p.g,   p.b   };

            case "Mars"    -> (t < 0.15f || t > 0.85f)
                    ? new float[]{ 0.55f, 0.28f, 0.18f }
                    : new float[]{ 0.85f, 0.38f, 0.22f };

            case "Sun"     -> {
                float eq = Math.max(0f, 1f - Math.abs(t - 0.5f)*1.6f);
                yield new float[]{ 1.00f, 0.72f + eq*0.22f, 0.08f + eq*0.18f };
            }
            default -> new float[]{ p.r, p.g, p.b };
        };
    }

    // ── Saturn rings ──────────────────────────────────────────────────────

    private void drawSaturnRings(Planet p) {
        glPushMatrix();
        glRotatef(p.axialTilt,           0f, 0f, 1f);
        glRotatef(p.rotationAngle * 0.3f, 0f, 1f, 0f);

        // Two ring bands instead of four — half the geometry
        float inner = p.renderRadius * 1.35f;
        float outer = p.renderRadius * 2.20f;
        int   segs  = 48; // was 80 — still looks smooth, much cheaper

        float[][] bands = {
                { 0.90f, 0.84f, 0.60f, 0.75f },
                { 0.65f, 0.58f, 0.40f, 0.35f },
        };
        float[] radii = { inner, inner * 1.6f, outer };

        for (int ring = 0; ring < bands.length; ring++) {
            float r1 = radii[ring], r2 = radii[ring + 1];
            float[] c = bands[ring];
            glColor4f(c[0], c[1], c[2], c[3]);
            glBegin(GL_QUAD_STRIP);
            for (int j = 0; j <= segs; j++) {
                double a   = Math.PI * 2 * j / segs;
                float  cos = (float) Math.cos(a);
                float  sin = (float) Math.sin(a);
                glVertex3f(r1*cos, 0f, r1*sin);
                glVertex3f(r2*cos, 0f, r2*sin);
            }
            glEnd();
        }

        glPopMatrix();
    }

    // ── Solar corona ──────────────────────────────────────────────────────

    private void drawSolarCorona(Planet p) {
        float r = p.renderRadius;
        float t = (float)(System.currentTimeMillis() % 4000) / 4000f;

        glColor4f(1.0f, 0.75f, 0.15f, 0.18f);
        glLineWidth(1.2f);
        glBegin(GL_LINES); // single draw call for all rays
        for (int i = 0; i < 12; i++) { // was 16, 12 is plenty
            double angle  = Math.PI * 2 * i / 12 + t * Math.PI * 0.5;
            float  length = r * (1.5f + 0.3f * (float) Math.sin(t * Math.PI * 2 + i));
            glVertex3f(0, 0, 0);
            glVertex3f(
                    (float) Math.cos(angle) * length,
                    (float) Math.sin(angle) * 0.25f * length,
                    (float) Math.sin(angle) * length
            );
        }
        glEnd();
    }

    // ── Sphere primitive ──────────────────────────────────────────────────

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