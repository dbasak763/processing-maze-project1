package com.physicsmaze;

import org.junit.Test;
import static org.junit.Assert.*;
import processing.core.PVector;

public class ParticleTest {
    
    @Test
    public void testParticleCreation() {
        PVector pos = new PVector(100, 200);
        Particle particle = new Particle(pos);
        
        assertEquals(100.0f, particle.pos.x, 0.001f);
        assertEquals(200.0f, particle.pos.y, 0.001f);
        assertEquals(100.0f, particle.prev.x, 0.001f);
        assertEquals(200.0f, particle.prev.y, 0.001f);
        assertFalse(particle.locked);
        assertEquals(1.0f, particle.mass, 0.001f);
    }
    
    @Test
    public void testParticleCopyConstructor() {
        PVector pos = new PVector(50, 75);
        Particle original = new Particle(pos);
        original.locked = true;
        original.mass = 2.0f;
        
        Particle copy = new Particle(original);
        
        assertEquals(original.pos.x, copy.pos.x, 0.001f);
        assertEquals(original.pos.y, copy.pos.y, 0.001f);
        assertEquals(original.locked, copy.locked);
        assertEquals(original.mass, copy.mass, 0.001f);
        
        // Ensure it's a deep copy
        original.pos.x = 100;
        assertEquals(50.0f, copy.pos.x, 0.001f);
    }
    
    @Test
    public void testVerletIntegration() {
        PVector pos = new PVector(0, 0);
        Particle particle = new Particle(pos);
        
        // Set initial velocity by moving prev position
        particle.prev.set(-1, 0); // Moving right with velocity 1
        
        float dt = 1.0f / 60.0f;
        particle.verlet(dt);
        
        // Should move right due to velocity, down due to gravity
        assertTrue(particle.pos.x > 0);
        assertTrue(particle.pos.y > 0);
    }
    
    @Test
    public void testLockedParticleDoesNotMove() {
        PVector pos = new PVector(100, 100);
        Particle particle = new Particle(pos);
        particle.locked = true;
        particle.prev.set(99, 100); // Set velocity
        
        float dt = 1.0f / 60.0f;
        particle.verlet(dt);
        
        // Locked particle should not move
        assertEquals(100.0f, particle.pos.x, 0.001f);
        assertEquals(100.0f, particle.pos.y, 0.001f);
    }
    
    @Test
    public void testSetPosition() {
        PVector pos = new PVector(0, 0);
        Particle particle = new Particle(pos);
        particle.prev.set(-10, -10); // Set some velocity
        
        PVector newPos = new PVector(50, 75);
        particle.setPosition(newPos);
        
        assertEquals(50.0f, particle.pos.x, 0.001f);
        assertEquals(75.0f, particle.pos.y, 0.001f);
        assertEquals(50.0f, particle.prev.x, 0.001f);
        assertEquals(75.0f, particle.prev.y, 0.001f);
    }
    
    @Test
    public void testGetVelocity() {
        PVector pos = new PVector(10, 20);
        Particle particle = new Particle(pos);
        particle.prev.set(5, 15); // Velocity should be (5, 5)
        
        PVector velocity = particle.getVelocity();
        assertEquals(5.0f, velocity.x, 0.001f);
        assertEquals(5.0f, velocity.y, 0.001f);
    }
}
