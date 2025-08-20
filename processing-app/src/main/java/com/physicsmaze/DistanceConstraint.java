package com.physicsmaze;

import processing.core.PVector;

public class DistanceConstraint {
    public Particle a, b;
    public float restLength;
    public float stiffness = 1.0f;
    
    public DistanceConstraint(Particle particleA, Particle particleB) {
        this.a = particleA;
        this.b = particleB;
        this.restLength = PVector.dist(a.pos, b.pos);
    }
    
    public DistanceConstraint(Particle particleA, Particle particleB, float length) {
        this.a = particleA;
        this.b = particleB;
        this.restLength = length;
    }
    
    public void solve() {
        PVector delta = PVector.sub(b.pos, a.pos);
        float currentLength = delta.mag();
        
        if (currentLength == 0) return; // Avoid division by zero
        
        float difference = (currentLength - restLength) / currentLength;
        float invMassA = a.locked ? 0 : 1.0f / a.mass;
        float invMassB = b.locked ? 0 : 1.0f / b.mass;
        float totalInvMass = invMassA + invMassB;
        
        if (totalInvMass == 0) return; // Both particles are locked
        
        // Apply position correction based on mass ratio
        delta.mult(stiffness * difference * 0.5f);
        
        if (!a.locked) {
            PVector correctionA = PVector.mult(delta, invMassA / totalInvMass);
            a.pos.add(correctionA);
        }
        
        if (!b.locked) {
            PVector correctionB = PVector.mult(delta, -invMassB / totalInvMass);
            b.pos.add(correctionB);
        }
    }
    
    public float getStress() {
        float currentLength = PVector.dist(a.pos, b.pos);
        return Math.abs(currentLength - restLength) / restLength;
    }
}
