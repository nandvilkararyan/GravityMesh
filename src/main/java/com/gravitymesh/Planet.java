package com.gravitymesh;

public class Planet {

    // ── Position & velocity (physics) ─────────────────────────────────────
    public float x, z;
    public float vx, vz;

    // ── Physical properties ────────────────────────────────────────────────
    public float mass;
    public float renderRadius;
    public float surfaceGravity;   // relative to Earth = 1.0
    public float r, g, b;
    public String name;

    // ── Rotation state ─────────────────────────────────────────────────────
    public float rotationAngle;    // current spin angle in degrees (0–360)
    public float rotationSpeed;    // degrees per tick (negative = retrograde)
    public float axialTilt;        // degrees from vertical

    // ── Rotation axis (3D unit vector derived from axialTilt) ──────────────
    // Axis lies in the XY plane: (sin(tilt), cos(tilt), 0) rotated by yaw
    // We'll fix the tilt axis direction per planet for visual clarity
    public float axisTiltX;        // X component of spin axis
    public float axisTiltZ;        // Z component of spin axis

    // ── Atmosphere / glow scale ────────────────────────────────────────────
    // Some planets have thicker atmospheres — scale their glow halo
    public float atmosphereScale;

    public Planet(PlanetType type, float x, float z) {
        this.name          = type.label;
        this.mass          = type.mass;
        this.renderRadius  = type.renderRadius;
        this.surfaceGravity = type.surfaceGravity;
        this.r = type.r; this.g = type.g; this.b = type.b;
        this.x = x; this.z = z;
        this.vx = 0; this.vz = 0;

        this.rotationAngle = 0f;
        this.rotationSpeed = type.rotationSpeed * 0.8f; // scale for visual comfort
        this.axialTilt     = type.axialTilt;

        // Tilt axis: perpendicular to the "up" axis, rotated by axialTilt
        // We tilt into the X axis for consistency
        this.axisTiltX = (float) Math.sin(Math.toRadians(axialTilt));
        this.axisTiltZ = 0f;

        // Atmosphere scale: thicker for gas giants and Venus
        this.atmosphereScale = switch (type) {
            case JUPITER, SATURN -> 2.0f;
            case URANUS, NEPTUNE -> 1.7f;
            case VENUS           -> 1.8f;
            case EARTH           -> 1.4f;
            case SUN             -> 2.5f;
            default              -> 1.1f;
        };
    }

    /** Called every simulation tick to advance rotation */
    public void updateRotation() {
        rotationAngle = (rotationAngle + rotationSpeed) % 360f;
        if (rotationAngle < 0) rotationAngle += 360f; // keep positive for retrograde
    }
}