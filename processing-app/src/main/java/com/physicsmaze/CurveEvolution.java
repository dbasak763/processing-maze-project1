package com.physicsmaze;

import processing.core.PVector;
import java.util.*;

/**
 * Core curve evolution engine implementing Brownian noise, Laplacian fairing,
 * and attraction-repulsion forces for organic maze generation.
 */
public class CurveEvolution {
    private ArrayList<Curve> curves;
    private SpatialHash spatialIndex;
    private Random random;
    
    // Global parameters
    public float sigma = 0.1f; // Brownian noise standard deviation
    public float sigmaLJ = 5.0f; // Lennard-Jones sigma parameter
    public float k0 = 0.2f; // R0 multiplier
    public float k1 = 0.4f; // R1 multiplier
    
    // Parameter maps
    public ParameterMap fBMap; // Brownian force strength
    public ParameterMap fFMap; // Fairing force strength
    public ParameterMap fAMap; // Attraction-repulsion strength
    public ParameterMap deltaMap; // Local scaling factor
    public ParameterMap fgMap; // Anisotropy field
    
    public CurveEvolution(float worldWidth, float worldHeight) {
        curves = new ArrayList<>();
        spatialIndex = new SpatialHash(32);
        random = new Random();
        
        // Initialize parameter maps with reasonable defaults
        int mapRes = 128;
        fBMap = new ParameterMap(mapRes, mapRes, 0, 0, worldWidth, worldHeight, 0.1f);
        fFMap = new ParameterMap(mapRes, mapRes, 0, 0, worldWidth, worldHeight, 0.05f);
        fAMap = new ParameterMap(mapRes, mapRes, 0, 0, worldWidth, worldHeight, 2.0f);
        deltaMap = new ParameterMap(mapRes, mapRes, 0, 0, worldWidth, worldHeight, 1.0f);
        fgMap = new ParameterMap(mapRes, mapRes, 0, 0, worldWidth, worldHeight, 0.0f);
    }
    
    public void addCurve(Curve curve) {
        curves.add(curve);
    }
    
    public void removeCurve(Curve curve) {
        curves.remove(curve);
    }
    
    public ArrayList<Curve> getCurves() {
        return curves;
    }
    
    /**
     * Main evolution step - apply all forces and update curves
     */
    public void evolve(float dt) {
        // Rebuild spatial index
        rebuildSpatialIndex();
        
        // Apply forces to all samples
        for (Curve curve : curves) {
            for (Sample sample : curve.samples) {
                if (sample.locked) continue;
                
                PVector totalForce = new PVector(0, 0);
                
                // Brownian noise
                PVector brownian = computeBrownianForce(sample);
                totalForce.add(brownian);
                
                // Laplacian fairing
                PVector fairing = computeFairingForce(sample, curve);
                totalForce.add(fairing);
                
                // Attraction-repulsion
                PVector attraction = computeAttractionRepulsionForce(sample, curve);
                totalForce.add(attraction);
                
                // Apply anisotropy
                PVector anisotropic = applyAnisotropy(sample, attraction);
                totalForce.sub(attraction);
                totalForce.add(anisotropic);
                
                // Apply force
                sample.applyForce(PVector.mult(totalForce, dt));
            }
        }
        
        // Verlet integration
        for (Curve curve : curves) {
            for (Sample sample : curve.samples) {
                sample.verletStep(dt);
            }
        }
        
        // Adaptive resampling
        for (Curve curve : curves) {
            curve.resample();
        }
    }
    
    /**
     * Compute Brownian force: Bi = fB(pi) * Normal(0, σ) * δ(pi) * D
     */
    private PVector computeBrownianForce(Sample sample) {
        float fB = fBMap.sample(sample);
        float delta = deltaMap.sample(sample);
        
        if (fB <= 0) return new PVector(0, 0);
        
        float dx = (float) (random.nextGaussian() * sigma);
        float dy = (float) (random.nextGaussian() * sigma);
        
        return new PVector(dx * fB * delta, dy * fB * delta);
    }
    
    /**
     * Compute Laplacian fairing force: Fi = fF(pi) * weighted_laplacian
     */
    private PVector computeFairingForce(Sample sample, Curve curve) {
        float fF = fFMap.sample(sample);
        if (fF <= 0) return new PVector(0, 0);
        
        int index = sample.indexInCurve;
        if (index <= 0 || index >= curve.samples.size() - 1) {
            if (!curve.closed) return new PVector(0, 0);
        }
        
        // Get neighbors (handle closed curves)
        Sample prev, next;
        if (curve.closed) {
            prev = curve.samples.get((index - 1 + curve.samples.size()) % curve.samples.size());
            next = curve.samples.get((index + 1) % curve.samples.size());
        } else {
            if (index <= 0 || index >= curve.samples.size() - 1) {
                return new PVector(0, 0);
            }
            prev = curve.samples.get(index - 1);
            next = curve.samples.get(index + 1);
        }
        
        // Weighted Laplacian: (pi-1*δ(pi+1) + pi+1*δ(pi-1)) / (δ(pi-1)+δ(pi+1)) - pi
        float deltaPrev = deltaMap.sample(prev);
        float deltaNext = deltaMap.sample(next);
        float deltaSum = deltaPrev + deltaNext;
        
        if (deltaSum <= 0) return new PVector(0, 0);
        
        PVector weightedSum = new PVector(0, 0);
        weightedSum.add(PVector.mult(prev.pos, deltaNext));
        weightedSum.add(PVector.mult(next.pos, deltaPrev));
        weightedSum.div(deltaSum);
        weightedSum.sub(sample.pos);
        
        return PVector.mult(weightedSum, fF);
    }
    
    /**
     * Compute attraction-repulsion force using Lennard-Jones potential
     */
    private PVector computeAttractionRepulsionForce(Sample sample, Curve curve) {
        float fA = fAMap.sample(sample);
        if (fA <= 0) return new PVector(0, 0);
        
        float delta = deltaMap.sample(sample);
        float R1 = k1 * delta;
        
        PVector totalForce = new PVector(0, 0);
        
        // Query nearby segments
        ArrayList<Curve> nearbyCurves = queryNearbyCurves(sample, R1);
        
        for (Curve otherCurve : nearbyCurves) {
            ArrayList<PVector[]> segments = otherCurve.getSegments();
            
            for (int segIndex = 0; segIndex < segments.size(); segIndex++) {
                PVector[] segment = segments.get(segIndex);
                
                // Skip segments from same curve that are too close
                if (otherCurve == curve) {
                    int sampleIndex = sample.indexInCurve;
                    if (Math.abs(segIndex - sampleIndex) <= curve.nmin) {
                        continue;
                    }
                    // Handle wraparound for closed curves
                    if (curve.closed) {
                        int wrapDist = Math.min(
                            Math.abs(segIndex - sampleIndex),
                            curve.samples.size() - Math.abs(segIndex - sampleIndex)
                        );
                        if (wrapDist <= curve.nmin) {
                            continue;
                        }
                    }
                }
                
                // Find closest point on segment
                PVector closestPoint = closestPointOnSegment(sample.pos, segment[0], segment[1]);
                float r = PVector.dist(sample.pos, closestPoint);
                
                if (r < R1 && r > 0.001f) { // Avoid singularity
                    // Lennard-Jones potential: w(r) = (σ/r)^12 - (σ/r)^6
                    float sigmaOverR = sigmaLJ / r;
                    float sigmaOverR6 = (float) Math.pow(sigmaOverR, 6);
                    float sigmaOverR12 = sigmaOverR6 * sigmaOverR6;
                    float w = sigmaOverR12 - sigmaOverR6;
                    
                    // Clamp to avoid explosive forces
                    w = Math.max(-10, Math.min(10, w));
                    
                    PVector direction = PVector.sub(sample.pos, closestPoint);
                    direction.normalize();
                    direction.mult(w * fA);
                    
                    totalForce.add(direction);
                }
            }
        }
        
        return totalForce;
    }
    
    /**
     * Apply anisotropy based on fg gradient
     */
    private PVector applyAnisotropy(Sample sample, PVector force) {
        PVector gradient = fgMap.gradient(sample);
        float gradMag = gradient.mag();
        
        if (gradMag < 0.001f) return force.copy();
        
        gradient.normalize();
        float dotProduct = PVector.dot(gradient, force);
        
        PVector anisotropicForce = force.copy();
        anisotropicForce.add(PVector.mult(gradient, dotProduct));
        
        return anisotropicForce;
    }
    
    /**
     * Rebuild spatial index for efficient neighbor queries
     */
    private void rebuildSpatialIndex() {
        spatialIndex.clear();
        
        // Add all curve segments to spatial index
        for (Curve curve : curves) {
            for (Sample sample : curve.samples) {
                spatialIndex.insert(new SpatialParticle(sample));
            }
        }
    }
    
    /**
     * Query curves near a sample point
     */
    private ArrayList<Curve> queryNearbyCurves(Sample sample, float radius) {
        Set<Integer> curveIds = new HashSet<>();
        ArrayList<SpatialParticle> nearby = spatialIndex.queryRadius(
            sample.pos.x, sample.pos.y, radius, SpatialParticle.class
        );
        
        for (SpatialParticle sp : nearby) {
            curveIds.add(sp.sample.curveId);
        }
        
        ArrayList<Curve> result = new ArrayList<>();
        for (int id : curveIds) {
            for (Curve curve : curves) {
                if (curve.id == id) {
                    result.add(curve);
                    break;
                }
            }
        }
        
        return result;
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
        
        return PVector.add(a, PVector.mult(ab, t));
    }
    
    /**
     * Wrapper class for spatial indexing of samples
     */
    private static class SpatialParticle extends Particle {
        public Sample sample;
        
        public SpatialParticle(Sample sample) {
            super(sample.pos);
            this.sample = sample;
        }
    }
}
