package com.gravitymesh;

public class Planet {

    public float x, z;       // position on mesh (world space)
    public float vx, vz;     // velocity
    public float mass;
    public float renderRadius;
    public float r, g, b;    // color
    public String name;

    /** Constructed from a PlanetType at a given mesh position */
    public Planet(PlanetType type, float x, float z) {
        this.name         = type.label;
        this.mass         = type.mass;
        this.renderRadius = type.renderRadius;
        this.r            = type.r;
        this.g            = type.g;
        this.b            = type.b;
        this.x            = x;
        this.z            = z;
        this.vx           = 0;
        this.vz           = 0;
    }
}