package com.gravitymesh;

public enum PlanetType {

    //         label      mass  renderR   R      G      B    rotSpeed  axialTilt  surfaceG
    MERCURY("Mercury",  0.38f,  0.10f, 0.72f, 0.70f, 0.68f,   0.017f,   0.03f,   0.38f),
    VENUS  ("Venus",    0.81f,  0.18f, 0.95f, 0.80f, 0.50f,  -0.004f,  177.4f,   0.91f),
    EARTH  ("Earth",    1.00f,  0.20f, 0.25f, 0.55f, 0.95f,   1.000f,   23.5f,   1.00f),
    MARS   ("Mars",     0.64f,  0.15f, 0.85f, 0.35f, 0.20f,   0.966f,   25.2f,   0.38f),
    JUPITER("Jupiter",  5.00f,  0.45f, 0.85f, 0.72f, 0.55f,   2.418f,    3.1f,   2.53f),
    SATURN ("Saturn",   4.50f,  0.40f, 0.92f, 0.85f, 0.65f,   2.252f,   26.7f,   1.07f),
    URANUS ("Uranus",   2.50f,  0.30f, 0.45f, 0.85f, 0.90f,  -1.392f,   97.8f,   0.89f),
    NEPTUNE("Neptune",  2.80f,  0.28f, 0.25f, 0.40f, 0.95f,   1.493f,   28.3f,   1.14f),
    SUN    ("Sun",     20.00f,  0.80f, 1.00f, 0.90f, 0.20f,   0.036f,    7.25f,  27.90f);

    public final String label;
    public final float  mass;
    public final float  renderRadius;
    public final float  r, g, b;

    /**
     * rotationSpeed: degrees per simulation tick, relative to Earth = 1.0
     * Negative = retrograde (Venus, Uranus)
     */
    public final float rotationSpeed;

    /**
     * axialTilt: degrees from orbital plane vertical
     * Earth = 23.5°, Uranus = 97.8° (on its side)
     */
    public final float axialTilt;

    /**
     * surfaceGravity: relative to Earth = 1.0
     * Affects mesh warp depth and influence radius
     */
    public final float surfaceGravity;

    PlanetType(String label, float mass, float renderRadius,
               float r, float g, float b,
               float rotationSpeed, float axialTilt, float surfaceGravity) {
        this.label         = label;
        this.mass          = mass;
        this.renderRadius  = renderRadius;
        this.r = r; this.g = g; this.b = b;
        this.rotationSpeed = rotationSpeed;
        this.axialTilt     = axialTilt;
        this.surfaceGravity = surfaceGravity;
    }
}