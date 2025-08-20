package com.physicsmaze;

import org.junit.Test;
import static org.junit.Assert.*;
import processing.core.PVector;

public class DistanceConstraintTest {
    
    @Test
    public void testConstraintCreation() {
        Particle a = new Particle(new PVector(0, 0));
        Particle b = new Particle(new PVector(10, 0));
        
        DistanceConstraint constraint = new DistanceConstraint(a, b);
        
        assertEquals(10.0f, constraint.restLength, 0.001f);
        assertEquals(1.0f, constraint.stiffness, 0.001f);
        assertSame(a, constraint.a);
        assertSame(b, constraint.b);
    }
    
    @Test
    public void testConstraintWithCustomLength() {
        Particle a = new Particle(new PVector(0, 0));
        Particle b = new Particle(new PVector(10, 0));
        
        DistanceConstraint constraint = new DistanceConstraint(a, b, 15.0f);
        
        assertEquals(15.0f, constraint.restLength, 0.001f);
    }
    
    @Test
    public void testConstraintSolving() {
        Particle a = new Particle(new PVector(0, 0));
        Particle b = new Particle(new PVector(20, 0)); // Too far apart
        
        DistanceConstraint constraint = new DistanceConstraint(a, b, 10.0f);
        constraint.solve();
        
        // Particles should move closer together
        float finalDistance = PVector.dist(a.pos, b.pos);
        assertTrue(finalDistance < 20.0f);
        assertTrue(finalDistance > 9.0f); // Should be close to rest length
    }
    
    @Test
    public void testLockedParticleConstraint() {
        Particle a = new Particle(new PVector(0, 0));
        Particle b = new Particle(new PVector(20, 0));
        a.locked = true; // Lock particle a
        
        PVector originalA = a.pos.copy();
        
        DistanceConstraint constraint = new DistanceConstraint(a, b, 10.0f);
        constraint.solve();
        
        // Locked particle should not move
        assertEquals(originalA.x, a.pos.x, 0.001f);
        assertEquals(originalA.y, a.pos.y, 0.001f);
        
        // Only particle b should move
        assertTrue(b.pos.x < 20.0f);
    }
    
    @Test
    public void testBothParticlesLocked() {
        Particle a = new Particle(new PVector(0, 0));
        Particle b = new Particle(new PVector(20, 0));
        a.locked = true;
        b.locked = true;
        
        PVector originalA = a.pos.copy();
        PVector originalB = b.pos.copy();
        
        DistanceConstraint constraint = new DistanceConstraint(a, b, 10.0f);
        constraint.solve();
        
        // Neither particle should move
        assertEquals(originalA.x, a.pos.x, 0.001f);
        assertEquals(originalA.y, a.pos.y, 0.001f);
        assertEquals(originalB.x, b.pos.x, 0.001f);
        assertEquals(originalB.y, b.pos.y, 0.001f);
    }
    
    @Test
    public void testGetStress() {
        Particle a = new Particle(new PVector(0, 0));
        Particle b = new Particle(new PVector(15, 0));
        
        DistanceConstraint constraint = new DistanceConstraint(a, b, 10.0f);
        
        float stress = constraint.getStress();
        assertEquals(0.5f, stress, 0.001f); // (15-10)/10 = 0.5
    }
    
    @Test
    public void testZeroDistanceHandling() {
        Particle a = new Particle(new PVector(5, 5));
        Particle b = new Particle(new PVector(5, 5)); // Same position
        
        DistanceConstraint constraint = new DistanceConstraint(a, b, 10.0f);
        
        // Should not crash or produce NaN
        constraint.solve();
        
        // Positions should still be valid numbers
        assertFalse(Float.isNaN(a.pos.x));
        assertFalse(Float.isNaN(a.pos.y));
        assertFalse(Float.isNaN(b.pos.x));
        assertFalse(Float.isNaN(b.pos.y));
    }
}
