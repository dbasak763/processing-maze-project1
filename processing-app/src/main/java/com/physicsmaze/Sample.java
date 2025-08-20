package com.physicsmaze;

import processing.core.PVector;

/**
 * A sample point on a curve with position, previous position for Verlet integration,
 * local scaling factor (delta), and various flags for curve evolution.
 */
public class Sample {
    public PVector pos;
    public PVector prev;
    public float delta = 1.0f;  // Local scaling factor δ(pi)
    public boolean locked = false;
    public boolean ignoreNeighbors = false;
    
    // For curve topology
    public int curveId = -1;
    public int indexInCurve = -1;
    
    public Sample(PVector position) {
        pos = position.copy();
        prev = position.copy();
    }
    
    public Sample(PVector position, float delta) {
        this(position);
        this.delta = delta;
    }
    
    // Copy constructor
    public Sample(Sample other) {
        pos = other.pos.copy();
        prev = other.prev.copy();
        delta = other.delta;
        locked = other.locked;
        ignoreNeighbors = other.ignoreNeighbors;
        curveId = other.curveId;
        indexInCurve = other.indexInCurve;
    }
    
    /**
     * Verlet integration step for temporal stability
     */
    public void verletStep(float dt) {
        if (locked) return;
        
        PVector vel = PVector.sub(pos, prev);
        prev.set(pos);
        pos.add(vel);
    }
    
    /**
     * Apply a force to this sample
     */
    public void applyForce(PVector force) {
        if (locked) return;
        pos.add(force);
    }
    
    /**
     * Get current velocity based on Verlet integration
     */
    public PVector getVelocity() {
        return PVector.sub(pos, prev);
    }
    
    /**
     * Set position and stop any existing velocity
     */
    public void setPosition(PVector newPos) {
        pos.set(newPos);
        prev.set(newPos);
    }
    
    /**
     * Distance to another sample
     */
    public float distanceTo(Sample other) {
        return PVector.dist(pos, other.pos);
    }
    
    /**
     * Distance to a point
     */
    public float distanceTo(PVector point) {
        return PVector.dist(pos, point);
    }
}
