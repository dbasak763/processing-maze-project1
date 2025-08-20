package com.physicsmaze;

import processing.core.PVector;
import processing.core.PImage;

/**
 * Parameter maps for spatially-varying curve evolution parameters.
 * Can be backed by 2D textures/images or procedural functions.
 */
public class ParameterMap {
    private float[][] data;
    private int width, height;
    private float minX, minY, maxX, maxY;
    private float defaultValue;
    
    public ParameterMap(int width, int height, float minX, float minY, float maxX, float maxY, float defaultValue) {
        this.width = width;
        this.height = height;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.defaultValue = defaultValue;
        this.data = new float[height][width];
        
        // Initialize with default value
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                data[y][x] = defaultValue;
            }
        }
    }
    
    /**
     * Sample the parameter map at world coordinates
     */
    public float sample(float x, float y) {
        // Convert world coordinates to texture coordinates
        float u = (x - minX) / (maxX - minX);
        float v = (y - minY) / (maxY - minY);
        
        // Clamp to bounds
        u = Math.max(0, Math.min(1, u));
        v = Math.max(0, Math.min(1, v));
        
        // Convert to pixel coordinates
        float px = u * (width - 1);
        float py = v * (height - 1);
        
        // Bilinear interpolation
        int x0 = (int) Math.floor(px);
        int y0 = (int) Math.floor(py);
        int x1 = Math.min(x0 + 1, width - 1);
        int y1 = Math.min(y0 + 1, height - 1);
        
        float fx = px - x0;
        float fy = py - y0;
        
        float v00 = data[y0][x0];
        float v10 = data[y0][x1];
        float v01 = data[y1][x0];
        float v11 = data[y1][x1];
        
        float v0 = v00 * (1 - fx) + v10 * fx;
        float v1 = v01 * (1 - fx) + v11 * fx;
        
        return v0 * (1 - fy) + v1 * fy;
    }
    
    /**
     * Sample the parameter map at a sample point
     */
    public float sample(Sample sample) {
        return sample(sample.pos.x, sample.pos.y);
    }
    
    /**
     * Set value at texture coordinates (0-1 range)
     */
    public void setValue(float u, float v, float value) {
        int x = (int) (u * (width - 1));
        int y = (int) (v * (height - 1));
        
        if (x >= 0 && x < width && y >= 0 && y < height) {
            data[y][x] = value;
        }
    }
    
    /**
     * Set value at world coordinates
     */
    public void setValueWorld(float x, float y, float value) {
        float u = (x - minX) / (maxX - minX);
        float v = (y - minY) / (maxY - minY);
        setValue(u, v, value);
    }
    
    /**
     * Paint a circular brush at world coordinates
     */
    public void paintBrush(float x, float y, float radius, float value, float strength) {
        float u = (x - minX) / (maxX - minX);
        float v = (y - minY) / (maxY - minY);
        
        int centerX = (int) (u * (width - 1));
        int centerY = (int) (v * (height - 1));
        
        float radiusPixels = radius * width / (maxX - minX);
        int radiusInt = (int) Math.ceil(radiusPixels);
        
        for (int dy = -radiusInt; dy <= radiusInt; dy++) {
            for (int dx = -radiusInt; dx <= radiusInt; dx++) {
                int px = centerX + dx;
                int py = centerY + dy;
                
                if (px >= 0 && px < width && py >= 0 && py < height) {
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    if (dist <= radiusPixels) {
                        float falloff = 1.0f - (dist / radiusPixels);
                        falloff = falloff * falloff; // Smooth falloff
                        
                        float currentValue = data[py][px];
                        data[py][px] = currentValue + (value - currentValue) * strength * falloff;
                    }
                }
            }
        }
    }
    
    /**
     * Compute gradient at world coordinates for anisotropy
     */
    public PVector gradient(float x, float y) {
        float epsilon = Math.min((maxX - minX) / width, (maxY - minY) / height) * 0.5f;
        
        float dx = (sample(x + epsilon, y) - sample(x - epsilon, y)) / (2 * epsilon);
        float dy = (sample(x, y + epsilon) - sample(x, y - epsilon)) / (2 * epsilon);
        
        return new PVector(dx, dy);
    }
    
    /**
     * Compute gradient at sample point
     */
    public PVector gradient(Sample sample) {
        return gradient(sample.pos.x, sample.pos.y);
    }
    
    /**
     * Fill with a procedural pattern
     */
    public void fillProcedural(ProceduralFunction func) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float worldX = minX + (maxX - minX) * x / (width - 1);
                float worldY = minY + (maxY - minY) * y / (height - 1);
                data[y][x] = func.evaluate(worldX, worldY);
            }
        }
    }
    
    /**
     * Load from Processing PImage
     */
    public void loadFromImage(PImage img, int channel) {
        img.loadPixels();
        
        for (int y = 0; y < height && y < img.height; y++) {
            for (int x = 0; x < width && x < img.width; x++) {
                int pixel = img.pixels[y * img.width + x];
                float value;
                
                switch (channel) {
                    case 0: // Red
                        value = ((pixel >> 16) & 0xFF) / 255.0f;
                        break;
                    case 1: // Green
                        value = ((pixel >> 8) & 0xFF) / 255.0f;
                        break;
                    case 2: // Blue
                        value = (pixel & 0xFF) / 255.0f;
                        break;
                    case 3: // Alpha
                        value = ((pixel >> 24) & 0xFF) / 255.0f;
                        break;
                    default: // Grayscale
                        int r = (pixel >> 16) & 0xFF;
                        int g = (pixel >> 8) & 0xFF;
                        int b = pixel & 0xFF;
                        value = (r + g + b) / (3.0f * 255.0f);
                        break;
                }
                
                data[y][x] = value;
            }
        }
    }
    
    /**
     * Get raw data for visualization
     */
    public float[][] getData() {
        return data;
    }
    
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public float getMinX() { return minX; }
    public float getMinY() { return minY; }
    public float getMaxX() { return maxX; }
    public float getMaxY() { return maxY; }
    
    /**
     * Interface for procedural parameter generation
     */
    public interface ProceduralFunction {
        float evaluate(float x, float y);
    }
}
