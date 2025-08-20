package com.physicsmaze;

import processing.core.PVector;
import java.util.*;

/**
 * A piecewise-linear curve composed of Sample points that evolves over time
 * using Brownian noise, Laplacian fairing, and attraction-repulsion forces.
 */
public class Curve {
    public ArrayList<Sample> samples;
    public int id;
    public boolean closed = false;
    public String type = "labyrinth"; // "boundary", "gap", "labyrinth", "solution"
    
    // Evolution parameters
    public float fB = 0.1f;  // Brownian force strength
    public float fF = 0.05f; // Fairing force strength
    public float fA = 2.0f;  // Attraction-repulsion strength
    public float D = 10.0f;  // Global sampling density
    
    // Resampling parameters
    public float kmin = 0.2f;
    public float kmax = 1.2f;
    public int nmin = 2; // Minimum neighbor distance for attraction-repulsion
    
    public Curve(int id) {
        this.id = id;
        this.samples = new ArrayList<>();
    }
    
    public Curve(int id, ArrayList<Sample> initialSamples) {
        this.id = id;
        this.samples = new ArrayList<>(initialSamples);
        updateSampleIndices();
    }
    
    /**
     * Add a sample to the curve
     */
    public void addSample(Sample sample) {
        sample.curveId = id;
        sample.indexInCurve = samples.size();
        samples.add(sample);
    }
    
    /**
     * Insert a sample at a specific index
     */
    public void insertSample(int index, Sample sample) {
        sample.curveId = id;
        samples.add(index, sample);
        updateSampleIndices();
    }
    
    /**
     * Remove a sample at index
     */
    public void removeSample(int index) {
        if (index >= 0 && index < samples.size()) {
            samples.remove(index);
            updateSampleIndices();
        }
    }
    
    /**
     * Update all sample indices after modification
     */
    private void updateSampleIndices() {
        for (int i = 0; i < samples.size(); i++) {
            samples.get(i).indexInCurve = i;
            samples.get(i).curveId = id;
        }
    }
    
    /**
     * Get the length of the curve
     */
    public float getLength() {
        float length = 0;
        for (int i = 0; i < samples.size() - 1; i++) {
            length += samples.get(i).distanceTo(samples.get(i + 1));
        }
        if (closed && samples.size() > 2) {
            length += samples.get(samples.size() - 1).distanceTo(samples.get(0));
        }
        return length;
    }
    
    /**
     * Get segment endpoints for spatial indexing
     */
    public ArrayList<PVector[]> getSegments() {
        ArrayList<PVector[]> segments = new ArrayList<>();
        
        for (int i = 0; i < samples.size() - 1; i++) {
            segments.add(new PVector[]{samples.get(i).pos, samples.get(i + 1).pos});
        }
        
        if (closed && samples.size() > 2) {
            segments.add(new PVector[]{samples.get(samples.size() - 1).pos, samples.get(0).pos});
        }
        
        return segments;
    }
    
    /**
     * Find closest point on curve to given position
     */
    public PVector closestPointOnCurve(PVector point) {
        float minDist = Float.MAX_VALUE;
        PVector closest = null;
        
        ArrayList<PVector[]> segments = getSegments();
        for (PVector[] segment : segments) {
            PVector closestOnSegment = closestPointOnSegment(point, segment[0], segment[1]);
            float dist = PVector.dist(point, closestOnSegment);
            if (dist < minDist) {
                minDist = dist;
                closest = closestOnSegment;
            }
        }
        
        return closest != null ? closest : samples.get(0).pos.copy();
    }
    
    /**
     * Find closest point on a line segment
     */
    private PVector closestPointOnSegment(PVector point, PVector a, PVector b) {
        PVector ab = PVector.sub(b, a);
        PVector ap = PVector.sub(point, a);
        
        float abLengthSq = ab.magSq();
        if (abLengthSq == 0) return a.copy();
        
        float t = PVector.dot(ap, ab) / abLengthSq;
        t = Math.max(0, Math.min(1, t));
        
        PVector closest = PVector.add(a, PVector.mult(ab, t));
        return closest;
    }
    
    /**
     * Adaptive resampling - split long segments and merge short ones
     */
    public void resample() {
        // Split long segments
        for (int i = samples.size() - 2; i >= 0; i--) {
            Sample current = samples.get(i);
            Sample next = samples.get(i + 1);
            
            float segmentLength = current.distanceTo(next);
            float avgDelta = (current.delta + next.delta) / 2.0f;
            float dmax = kmax * D * avgDelta;
            
            if (segmentLength > dmax) {
                // Insert midpoint
                PVector midPos = PVector.lerp(current.pos, next.pos, 0.5f);
                Sample midSample = new Sample(midPos, avgDelta);
                insertSample(i + 1, midSample);
            }
        }
        
        // Handle closed curve
        if (closed && samples.size() > 2) {
            Sample last = samples.get(samples.size() - 1);
            Sample first = samples.get(0);
            
            float segmentLength = last.distanceTo(first);
            float avgDelta = (last.delta + first.delta) / 2.0f;
            float dmax = kmax * D * avgDelta;
            
            if (segmentLength > dmax) {
                PVector midPos = PVector.lerp(last.pos, first.pos, 0.5f);
                Sample midSample = new Sample(midPos, avgDelta);
                addSample(midSample);
            }
        }
        
        // Merge short segments
        for (int i = samples.size() - 2; i > 0; i--) {
            Sample current = samples.get(i);
            Sample prev = samples.get(i - 1);
            Sample next = samples.get(i + 1);
            
            float avgDelta = (prev.delta + current.delta + next.delta) / 3.0f;
            float dmin = kmin * D * avgDelta;
            
            if (current.distanceTo(prev) < dmin || current.distanceTo(next) < dmin) {
                if (!current.locked) {
                    removeSample(i);
                }
            }
        }
    }
    
    /**
     * Create a simple circular curve for testing
     */
    public static Curve createCircle(int id, PVector center, float radius, int numSamples) {
        Curve curve = new Curve(id);
        curve.closed = true;
        
        for (int i = 0; i < numSamples; i++) {
            float angle = (float) (2 * Math.PI * i / numSamples);
            float x = center.x + radius * (float) Math.cos(angle);
            float y = center.y + radius * (float) Math.sin(angle);
            
            Sample sample = new Sample(new PVector(x, y));
            curve.addSample(sample);
        }
        
        return curve;
    }
    
    /**
     * Create a simple line curve
     */
    public static Curve createLine(int id, PVector start, PVector end, int numSamples) {
        Curve curve = new Curve(id);
        
        for (int i = 0; i < numSamples; i++) {
            float t = (float) i / (numSamples - 1);
            PVector pos = PVector.lerp(start, end, t);
            
            Sample sample = new Sample(pos);
            curve.addSample(sample);
        }
        
        return curve;
    }
}
