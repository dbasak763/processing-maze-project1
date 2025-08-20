package com.physicsmaze;

import java.util.*;

public class MazeState {
    public ArrayList<Particle> particles;
    public ArrayList<Integer> constraintIndices; // Store indices instead of direct references
    
    public MazeState(ArrayList<Particle> particles, ArrayList<DistanceConstraint> constraints) {
        // Deep copy particles
        this.particles = new ArrayList<>();
        for (Particle p : particles) {
            this.particles.add(new Particle(p));
        }
        
        // Store constraint indices for reconstruction
        this.constraintIndices = new ArrayList<>();
        for (DistanceConstraint constraint : constraints) {
            int aIndex = particles.indexOf(constraint.a);
            int bIndex = particles.indexOf(constraint.b);
            if (aIndex != -1 && bIndex != -1) {
                constraintIndices.add(aIndex);
                constraintIndices.add(bIndex);
            }
        }
    }
}
