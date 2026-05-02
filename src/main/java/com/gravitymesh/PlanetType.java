package com.gravitymesh;

public enum PlanetType {

    //  Name        mass    radius   R     G     B      (color)
    MERCURY("Mercury",  0.4f,  0.10f, 0.72f, 0.70f, 0.68f),
    VENUS  ("Venus",    0.9f,  0.18f, 0.95f, 0.80f, 0.50f),
    EARTH  ("Earth",    1.0f,  0.20f, 0.25f, 0.55f, 0.95f),
    MARS   ("Mars",     0.6f,  0.15f, 0.85f, 0.35f, 0.20f),
    JUPITER("Jupiter",  5.0f,  0.45f, 0.85f, 0.72f, 0.55f),
    SATURN ("Saturn",   4.0f,  0.40f, 0.92f, 0.85f, 0.65f),
    URANUS ("Uranus",   2.5f,  0.30f, 0.45f, 0.85f, 0.90f),
    NEPTUNE("Neptune",  2.8f,  0.28f, 0.25f, 0.40f, 0.95f),
    SUN    ("Sun",     20.0f,  0.80f, 1.00f, 0.90f, 0.20f);

    public final String  label;
    public final float   mass;
    public final float   renderRadius;
    public final float   r, g, b;

    PlanetType(String label, float mass, float renderRadius,
               float r, float g, float b) {
        this.label        = label;
        this.mass         = mass;
        this.renderRadius = renderRadius;
        this.r = r; this.g = g; this.b = b;
    }
}