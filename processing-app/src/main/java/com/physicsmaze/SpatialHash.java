package com.physicsmaze;

import java.util.*;

public class SpatialHash {
    private HashMap<Long, ArrayList<Particle>> buckets = new HashMap<>();
    private int cellSize;
    
    public SpatialHash(int cellSize) {
        this.cellSize = cellSize;
    }
    
    private long getKey(float x, float y) {
        int ix = (int) Math.floor(x / cellSize);
        int iy = (int) Math.floor(y / cellSize);
        return (((long) ix) << 32) ^ (iy & 0xffffffffL);
    }
    
    public void clear() {
        buckets.clear();
    }
    
    public void insert(Particle particle) {
        long key = getKey(particle.pos.x, particle.pos.y);
        buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(particle);
    }
    
    public ArrayList<Particle> queryNeighbors(Particle particle) {
        ArrayList<Particle> neighbors = new ArrayList<>();
        int ix = (int) Math.floor(particle.pos.x / cellSize);
        int iy = (int) Math.floor(particle.pos.y / cellSize);
        
        // Check 3x3 grid around the particle
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                long key = (((long) (ix + dx)) << 32) ^ ((iy + dy) & 0xffffffffL);
                ArrayList<Particle> bucket = buckets.get(key);
                if (bucket != null) {
                    neighbors.addAll(bucket);
                }
            }
        }
        
        return neighbors;
    }
    
    public ArrayList<Particle> queryRadius(float x, float y, float radius) {
        ArrayList<Particle> result = new ArrayList<>();
        int cellRadius = (int) Math.ceil(radius / cellSize);
        int ix = (int) Math.floor(x / cellSize);
        int iy = (int) Math.floor(y / cellSize);
        
        for (int dx = -cellRadius; dx <= cellRadius; dx++) {
            for (int dy = -cellRadius; dy <= cellRadius; dy++) {
                long key = (((long) (ix + dx)) << 32) ^ ((iy + dy) & 0xffffffffL);
                ArrayList<Particle> bucket = buckets.get(key);
                if (bucket != null) {
                    for (Particle p : bucket) {
                        float dist = (float) Math.sqrt((p.pos.x - x) * (p.pos.x - x) + 
                                                     (p.pos.y - y) * (p.pos.y - y));
                        if (dist <= radius) {
                            result.add(p);
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Generic query method that returns any type extending Particle
     */
    @SuppressWarnings("unchecked")
    public <T extends Particle> ArrayList<T> queryRadius(float x, float y, float radius, Class<T> type) {
        ArrayList<T> result = new ArrayList<>();
        int cellRadius = (int) Math.ceil(radius / cellSize);
        int ix = (int) Math.floor(x / cellSize);
        int iy = (int) Math.floor(y / cellSize);
        
        for (int dx = -cellRadius; dx <= cellRadius; dx++) {
            for (int dy = -cellRadius; dy <= cellRadius; dy++) {
                long key = (((long) (ix + dx)) << 32) ^ ((iy + dy) & 0xffffffffL);
                ArrayList<Particle> bucket = buckets.get(key);
                if (bucket != null) {
                    for (Particle p : bucket) {
                        if (type.isInstance(p)) {
                            float dist = (float) Math.sqrt((p.pos.x - x) * (p.pos.x - x) + 
                                                         (p.pos.y - y) * (p.pos.y - y));
                            if (dist <= radius) {
                                result.add((T) p);
                            }
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    public int getBucketCount() {
        return buckets.size();
    }
    
    public int getTotalParticles() {
        return buckets.values().stream().mapToInt(ArrayList::size).sum();
    }
}
