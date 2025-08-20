package com.physicsmaze;

import processing.core.PVector;

public class Particle {
    public PVector pos;
    public PVector prev;
    public boolean locked = false;
    public float mass = 1.0f;
    
    public Particle(PVector position) {
        pos = position.copy();
        prev = position.copy();
    }
    
    // Copy constructor for undo/redo
    public Particle(Particle other) {
        pos = other.pos.copy();
        prev = other.prev.copy();
        locked = other.locked;
        mass = other.mass;
    }
    
    public void verlet(float dt) {
        if (locked) return;
        
        // Verlet integration: x(t+dt) = 2*x(t) - x(t-dt) + a*dt^2
        PVector velocity = PVector.sub(pos, prev);
        prev.set(pos);
        pos.add(velocity);
        
        // Apply gravity
        pos.y += 980 * dt * dt * mass;
    }
    
    public void setPosition(PVector newPos) {
        pos.set(newPos);
        prev.set(newPos); // Stop any existing velocity
    }
    
    public PVector getVelocity() {
        return PVector.sub(pos, prev);
    }
    
    public void addForce(PVector force, float dt) {
        if (locked) return;
        
        // F = ma, so a = F/m
        PVector acceleration = PVector.div(force, mass);
        acceleration.mult(dt * dt);
        pos.add(acceleration);
    }
}
