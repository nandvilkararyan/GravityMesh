package com.gravitymesh;

import static org.lwjgl.opengl.GL11.*;

public class UIPanel {

    // Screen dimensions (passed in from window)
    private int screenW, screenH;

    // Panel layout
    private static final float PANEL_HEIGHT_RATIO = 0.18f; // 18% of screen height
    private static final int   PADDING            = 14;
    private static final int   ITEM_SPACING        = 10;

    private PlanetType[] types   = PlanetType.values();
    private PlanetType   selected = PlanetType.EARTH;

    // Computed each frame
    private float panelY;       // bottom Y of panel in screen coords
    private float itemSize;     // size of each circle slot

    public UIPanel(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;
    }

    public PlanetType getSelected() {
        return selected;
    }

    /**
     * Renders the panel. Must be called while in 2D orthographic mode.
     */
    public void render() {
        float panelH = screenH * PANEL_HEIGHT_RATIO;
        panelY       = screenH - panelH;
        itemSize     = panelH - PADDING * 2;

        // ── Background bar ──────────────────────────────────────────────
        glColor4f(0.04f, 0.06f, 0.14f, 0.92f);
        fillRect(0, panelY, screenW, panelH);

        // ── Separator line ───────────────────────────────────────────────
        glColor3f(0.20f, 0.50f, 0.80f);
        glLineWidth(1.5f);
        glBegin(GL_LINES);
        glVertex2f(0,       panelY);
        glVertex2f(screenW, panelY);
        glEnd();

        // ── Items ─────────────────────────────────────────────────────────
        float totalW   = types.length * (itemSize + ITEM_SPACING) - ITEM_SPACING;
        float startX   = (screenW - totalW) / 2f;

        for (int i = 0; i < types.length; i++) {
            PlanetType t  = types[i];
            float cx      = startX + i * (itemSize + ITEM_SPACING) + itemSize / 2f;
            float cy      = panelY + PADDING + itemSize / 2f;
            float radius  = itemSize / 2f * 0.62f;

            boolean isSelected = (t == selected);

            // Selection highlight ring
            if (isSelected) {
                glColor3f(0.40f, 0.85f, 1.00f);
                drawCircleOutline(cx, cy, radius + 5f, 32);
                glColor3f(0.15f, 0.30f, 0.55f);
                fillCircle(cx, cy, radius + 4f, 32);
            }

            // Planet circle
            glColor3f(t.r, t.g, t.b);
            fillCircle(cx, cy, radius, 32);

            // Planet glow (additive-ish by drawing translucent bigger circle)
            glColor4f(t.r, t.g, t.b, 0.18f);
            fillCircle(cx, cy, radius * 1.5f, 32);

            // Saturn ring (special case)
            if (t == PlanetType.SATURN) {
                glColor3f(0.85f, 0.78f, 0.55f);
                glLineWidth(2f);
                drawEllipseOutline(cx, cy, radius * 1.7f, radius * 0.45f, 40);
            }

            // Label below circle
            // (OpenGL legacy has no text — we'll use a simple dot-matrix trick
            //  or just draw the first letter as a filled marker.
            //  For proper text, see the note below.)
            drawLabel(t.label, cx, panelY + PADDING + itemSize + 6f);
        }

        // ── "Selected:" indicator at top-left of panel ────────────────────
        glColor3f(0.45f, 0.75f, 1.0f);
        // Draw small filled diamond to indicate active
        float dx = 18, dy = panelY + panelH / 2f;
        glBegin(GL_QUADS);
        glVertex2f(dx,         dy - 6);
        glVertex2f(dx + 6,     dy);
        glVertex2f(dx,         dy + 6);
        glVertex2f(dx - 6,     dy);
        glEnd();
    }

    /**
     * Call this on mouse click (screen coords).
     * Returns true if the click was consumed by the panel.
     */
    public boolean handleClick(float mouseX, float mouseY) {
        if (mouseY < panelY) return false;   // click was above panel

        float panelH = screenH * PANEL_HEIGHT_RATIO;
        float totalW = types.length * (itemSize + ITEM_SPACING) - ITEM_SPACING;
        float startX = (screenW - totalW) / 2f;

        for (int i = 0; i < types.length; i++) {
            float cx = startX + i * (itemSize + ITEM_SPACING) + itemSize / 2f;
            float cy = panelY + PADDING + itemSize / 2f;
            float radius = itemSize / 2f * 0.62f + 6f; // slightly generous hit area

            float ddx = mouseX - cx;
            float ddy = mouseY - cy;
            if (ddx * ddx + ddy * ddy <= radius * radius) {
                selected = types[i];
                return true;
            }
        }
        return false;
    }

    // ── Primitive helpers ─────────────────────────────────────────────────

    private void fillRect(float x, float y, float w, float h) {
        glBegin(GL_QUADS);
        glVertex2f(x,     y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x,     y + h);
        glEnd();
    }

    private void fillCircle(float cx, float cy, float r, int segs) {
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(cx, cy);
        for (int i = 0; i <= segs; i++) {
            double a = Math.PI * 2 * i / segs;
            glVertex2f(cx + (float) Math.cos(a) * r,
                    cy + (float) Math.sin(a) * r);
        }
        glEnd();
    }

    private void drawCircleOutline(float cx, float cy, float r, int segs) {
        glBegin(GL_LINE_LOOP);
        for (int i = 0; i < segs; i++) {
            double a = Math.PI * 2 * i / segs;
            glVertex2f(cx + (float) Math.cos(a) * r,
                    cy + (float) Math.sin(a) * r);
        }
        glEnd();
    }

    private void drawEllipseOutline(float cx, float cy, float rx, float ry, int segs) {
        glBegin(GL_LINE_LOOP);
        for (int i = 0; i < segs; i++) {
            double a = Math.PI * 2 * i / segs;
            glVertex2f(cx + (float) Math.cos(a) * rx,
                    cy + (float) Math.sin(a) * ry);
        }
        glEnd();
    }

    /**
     * Draws a tiny pixel-art style label using scaled GL_POINTS.
     * Each letter is approximated by a 3×5 dot grid.
     * For production you'd use a bitmap font — this keeps zero dependencies.
     */
    private void drawLabel(String text, float cx, float baseY) {
        // We'll render up to 7 chars of the label, centered
        String s = text.length() > 7 ? text.substring(0, 7) : text;
        float charW = 4f, charH = 6f, gap = 1f;
        float totalW = s.length() * (charW + gap) - gap;
        float startX = cx - totalW / 2f;

        glPointSize(1.5f);
        glColor3f(0.70f, 0.85f, 1.00f);
        glBegin(GL_POINTS);
        for (int ci = 0; ci < s.length(); ci++) {
            char c = Character.toUpperCase(s.charAt(ci));
            boolean[][] dots = getCharDots(c);
            float ox = startX + ci * (charW + gap);
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 3; col++) {
                    if (dots[row][col]) {
                        glVertex2f(ox + col * (charW / 3f),
                                baseY + row * (charH / 5f));
                    }
                }
            }
        }
        glEnd();
    }

    /** Minimal 3×5 pixel font for A–Z and space */
    private boolean[][] getCharDots(char c) {
        // Each entry: 5 rows × 3 cols
        switch (c) {
            case 'A': return new boolean[][]{{false,true,false},{true,false,true},{true,true,true},{true,false,true},{true,false,true}};
            case 'B': return new boolean[][]{{true,true,false},{true,false,true},{true,true,false},{true,false,true},{true,true,false}};
            case 'C': return new boolean[][]{{false,true,true},{true,false,false},{true,false,false},{true,false,false},{false,true,true}};
            case 'D': return new boolean[][]{{true,true,false},{true,false,true},{true,false,true},{true,false,true},{true,true,false}};
            case 'E': return new boolean[][]{{true,true,true},{true,false,false},{true,true,false},{true,false,false},{true,true,true}};
            case 'G': return new boolean[][]{{false,true,true},{true,false,false},{true,false,true},{true,false,true},{false,true,true}};
            case 'H': return new boolean[][]{{true,false,true},{true,false,true},{true,true,true},{true,false,true},{true,false,true}};
            case 'I': return new boolean[][]{{true,true,true},{false,true,false},{false,true,false},{false,true,false},{true,true,true}};
            case 'J': return new boolean[][]{{false,false,true},{false,false,true},{false,false,true},{true,false,true},{false,true,false}};
            case 'M': return new boolean[][]{{true,false,true},{true,true,true},{true,false,true},{true,false,true},{true,false,true}};
            case 'N': return new boolean[][]{{true,false,true},{true,true,true},{true,true,true},{true,false,true},{true,false,true}};
            case 'P': return new boolean[][]{{true,true,false},{true,false,true},{true,true,false},{true,false,false},{true,false,false}};
            case 'R': return new boolean[][]{{true,true,false},{true,false,true},{true,true,false},{true,false,true},{true,false,true}};
            case 'S': return new boolean[][]{{false,true,true},{true,false,false},{false,true,false},{false,false,true},{true,true,false}};
            case 'T': return new boolean[][]{{true,true,true},{false,true,false},{false,true,false},{false,true,false},{false,true,false}};
            case 'U': return new boolean[][]{{true,false,true},{true,false,true},{true,false,true},{true,false,true},{false,true,false}};
            case 'V': return new boolean[][]{{true,false,true},{true,false,true},{true,false,true},{true,false,true},{false,true,false}};
            case 'Y': return new boolean[][]{{true,false,true},{true,false,true},{false,true,false},{false,true,false},{false,true,false}};
            default:  return new boolean[][]{{false,false,false},{false,false,false},{false,false,false},{false,false,false},{false,false,false}};
        }
    }
}