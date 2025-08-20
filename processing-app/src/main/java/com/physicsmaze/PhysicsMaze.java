package com.physicsmaze;

import processing.core.*;
import java.util.*;

public class PhysicsMaze extends PApplet {
    // Physics components
    ArrayList<Particle> particles = new ArrayList<>();
    ArrayList<DistanceConstraint> constraints = new ArrayList<>();
    SpatialHash spatialHash;
    
    // UI state
    boolean showForces = true;
    boolean showFPS = true;
    boolean paused = false;
    int mode = 0; // 0=draw, 1=erase, 2=drag
    
    // Interaction
    Particle draggedParticle = null;
    PVector mouseStart = new PVector();
    
    // History for undo/redo
    ArrayList<MazeState> history = new ArrayList<>();
    int historyIndex = -1;
    
    // Performance tracking
    float avgFPS = 60;
    long frameCount = 0;
    
    public static void main(String[] args) {
        PApplet.main("com.physicsmaze.PhysicsMaze");
    }
    
    public void settings() {
        size(1200, 800);
    }
    
    public void setup() {
        frameRate(60);
        spatialHash = new SpatialHash(32);
        
        // Initialize with a simple maze
        initializeMaze();
        
        // Save initial state
        saveState();
    }
    
    public void draw() {
        background(20, 25, 35);
        
        if (!paused) {
            updatePhysics();
        }
        
        render();
        drawUI();
        
        // Update FPS counter
        if (frameCount % 30 == 0) {
            avgFPS = avgFPS * 0.9f + frameRate * 0.1f;
        }
        frameCount++;
    }
    
    void updatePhysics() {
        float dt = 1.0f / 60.0f;
        
        // Verlet integration
        for (Particle p : particles) {
            p.verlet(dt);
        }
        
        // Rebuild spatial hash
        spatialHash.clear();
        for (Particle p : particles) {
            spatialHash.insert(p);
        }
        
        // Solve constraints (multiple iterations for stability)
        for (int iteration = 0; iteration < 6; iteration++) {
            for (DistanceConstraint constraint : constraints) {
                constraint.solve();
            }
            
            // Handle collisions
            handleCollisions();
        }
        
        // Keep particles in bounds
        for (Particle p : particles) {
            if (p.pos.x < 10) p.pos.x = 10;
            if (p.pos.x > width - 10) p.pos.x = width - 10;
            if (p.pos.y < 10) p.pos.y = 10;
            if (p.pos.y > height - 60) p.pos.y = height - 60;
        }
    }
    
    void handleCollisions() {
        for (Particle p : particles) {
            ArrayList<Particle> neighbors = spatialHash.queryNeighbors(p);
            for (Particle other : neighbors) {
                if (p == other) continue;
                
                PVector delta = PVector.sub(p.pos, other.pos);
                float distance = delta.mag();
                float minDistance = 8.0f; // Particle radius * 2
                
                if (distance < minDistance && distance > 0) {
                    delta.normalize();
                    delta.mult((minDistance - distance) * 0.5f);
                    
                    if (!p.locked) p.pos.add(delta);
                    if (!other.locked) other.pos.sub(delta);
                }
            }
        }
    }
    
    void render() {
        // Draw constraints (walls)
        stroke(150, 200, 255, 180);
        strokeWeight(2);
        for (DistanceConstraint constraint : constraints) {
            line(constraint.a.pos.x, constraint.a.pos.y, 
                 constraint.b.pos.x, constraint.b.pos.y);
        }
        
        // Draw particles
        fill(255, 100, 100);
        noStroke();
        for (Particle p : particles) {
            if (p.locked) {
                fill(100, 255, 100); // Green for fixed particles
            } else {
                fill(255, 100, 100); // Red for movable particles
            }
            ellipse(p.pos.x, p.pos.y, 6, 6);
        }
        
        // Draw force vectors if enabled
        if (showForces) {
            drawForceVectors();
        }
        
        // Highlight dragged particle
        if (draggedParticle != null) {
            stroke(255, 255, 0);
            strokeWeight(3);
            noFill();
            ellipse(draggedParticle.pos.x, draggedParticle.pos.y, 12, 12);
        }
    }
    
    void drawForceVectors() {
        stroke(255, 255, 0, 150);
        strokeWeight(1);
        
        for (Particle p : particles) {
            if (p.locked) continue;
            
            PVector force = PVector.sub(p.pos, p.prev);
            force.mult(10); // Scale for visibility
            
            if (force.mag() > 1) {
                line(p.pos.x, p.pos.y, 
                     p.pos.x + force.x, p.pos.y + force.y);
                
                // Arrow head
                pushMatrix();
                translate(p.pos.x + force.x, p.pos.y + force.y);
                rotate(atan2(force.y, force.x));
                line(0, 0, -5, -2);
                line(0, 0, -5, 2);
                popMatrix();
            }
        }
    }
    
    void drawUI() {
        // Top toolbar
        fill(40, 50, 65, 200);
        rect(0, 0, width, 50);
        
        // Mode buttons
        drawButton(10, 10, 80, 30, "Draw", mode == 0);
        drawButton(100, 10, 80, 30, "Erase", mode == 1);
        drawButton(190, 10, 80, 30, "Drag", mode == 2);
        
        // Control buttons
        drawButton(300, 10, 80, 30, paused ? "Play" : "Pause", false);
        drawButton(390, 10, 100, 30, "Show Forces", showForces);
        drawButton(500, 10, 80, 30, "Clear", false);
        drawButton(590, 10, 80, 30, "Generate", false);
        
        // Undo/Redo
        drawButton(700, 10, 60, 30, "Undo", false);
        drawButton(770, 10, 60, 30, "Redo", false);
        
        // FPS counter
        if (showFPS) {
            fill(255);
            textAlign(RIGHT);
            text("FPS: " + nf(avgFPS, 0, 1), width - 10, 25);
        }
        
        // Instructions
        fill(255, 255, 255, 150);
        textAlign(LEFT);
        text("Space: Play/Pause | Z: Undo | Y: Redo | F: Toggle Forces", 10, height - 10);
    }
    
    void drawButton(float x, float y, float w, float h, String label, boolean active) {
        if (active) {
            fill(100, 150, 255);
        } else {
            fill(60, 70, 85);
        }
        
        rect(x, y, w, h, 5);
        
        fill(255);
        textAlign(CENTER);
        text(label, x + w/2, y + h/2 + 4);
    }
    
    void initializeMaze() {
        // Create a simple grid maze for demonstration
        int gridSize = 20;
        int cols = width / gridSize;
        int rows = (height - 60) / gridSize;
        
        // Create grid of particles
        Particle[][] grid = new Particle[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                PVector pos = new PVector(col * gridSize + 60, row * gridSize + 80);
                grid[row][col] = new Particle(pos);
                
                // Lock border particles
                if (row == 0 || row == rows-1 || col == 0 || col == cols-1) {
                    grid[row][col].locked = true;
                }
                
                particles.add(grid[row][col]);
            }
        }
        
        // Create constraints (walls)
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                // Horizontal constraints
                if (col < cols - 1) {
                    constraints.add(new DistanceConstraint(grid[row][col], grid[row][col + 1]));
                }
                // Vertical constraints
                if (row < rows - 1) {
                    constraints.add(new DistanceConstraint(grid[row][col], grid[row + 1][col]));
                }
            }
        }
    }
    
    void saveState() {
        // Save current state for undo/redo
        MazeState state = new MazeState(particles, constraints);
        
        // Remove future states if we're not at the end
        while (history.size() > historyIndex + 1) {
            history.remove(history.size() - 1);
        }
        
        history.add(state);
        historyIndex++;
        
        // Limit history size
        if (history.size() > 50) {
            history.remove(0);
            historyIndex--;
        }
    }
    
    void undo() {
        if (historyIndex > 0) {
            historyIndex--;
            loadState(history.get(historyIndex));
        }
    }
    
    void redo() {
        if (historyIndex < history.size() - 1) {
            historyIndex++;
            loadState(history.get(historyIndex));
        }
    }
    
    void loadState(MazeState state) {
        particles.clear();
        constraints.clear();
        
        // Deep copy particles
        for (Particle p : state.particles) {
            particles.add(new Particle(p));
        }
        
        // Recreate constraints with new particle references
        for (int i = 0; i < state.constraintIndices.size(); i += 2) {
            int aIndex = state.constraintIndices.get(i);
            int bIndex = state.constraintIndices.get(i + 1);
            constraints.add(new DistanceConstraint(particles.get(aIndex), particles.get(bIndex)));
        }
    }
    
    // Mouse interaction
    public void mousePressed() {
        if (mouseY < 50) {
            handleUIClick();
            return;
        }
        
        if (mode == 2) { // Drag mode
            // Find closest particle
            float minDist = Float.MAX_VALUE;
            Particle closest = null;
            
            for (Particle p : particles) {
                float dist = PVector.dist(new PVector(mouseX, mouseY), p.pos);
                if (dist < minDist && dist < 20) {
                    minDist = dist;
                    closest = p;
                }
            }
            
            if (closest != null) {
                draggedParticle = closest;
                mouseStart.set(mouseX, mouseY);
            }
        }
    }
    
    public void mouseDragged() {
        if (draggedParticle != null) {
            draggedParticle.pos.set(mouseX, mouseY);
            draggedParticle.prev.set(mouseX, mouseY); // Stop velocity
        }
    }
    
    public void mouseReleased() {
        if (draggedParticle != null) {
            saveState();
            draggedParticle = null;
        }
    }
    
    void handleUIClick() {
        // Check button clicks
        if (mouseX >= 10 && mouseX <= 90 && mouseY >= 10 && mouseY <= 40) {
            mode = 0; // Draw
        } else if (mouseX >= 100 && mouseX <= 180 && mouseY >= 10 && mouseY <= 40) {
            mode = 1; // Erase
        } else if (mouseX >= 190 && mouseX <= 270 && mouseY >= 10 && mouseY <= 40) {
            mode = 2; // Drag
        } else if (mouseX >= 300 && mouseX <= 380 && mouseY >= 10 && mouseY <= 40) {
            paused = !paused;
        } else if (mouseX >= 390 && mouseX <= 490 && mouseY >= 10 && mouseY <= 40) {
            showForces = !showForces;
        } else if (mouseX >= 500 && mouseX <= 580 && mouseY >= 10 && mouseY <= 40) {
            clearMaze();
        } else if (mouseX >= 590 && mouseX <= 670 && mouseY >= 10 && mouseY <= 40) {
            generateMaze();
        } else if (mouseX >= 700 && mouseX <= 760 && mouseY >= 10 && mouseY <= 40) {
            undo();
        } else if (mouseX >= 770 && mouseX <= 830 && mouseY >= 10 && mouseY <= 40) {
            redo();
        }
    }
    
    void clearMaze() {
        particles.clear();
        constraints.clear();
        saveState();
    }
    
    void generateMaze() {
        clearMaze();
        initializeMaze();
        saveState();
    }
    
    // Keyboard shortcuts
    public void keyPressed() {
        if (key == ' ') {
            paused = !paused;
        } else if (key == 'z' || key == 'Z') {
            undo();
        } else if (key == 'y' || key == 'Y') {
            redo();
        } else if (key == 'f' || key == 'F') {
            showForces = !showForces;
        } else if (key == 'c' || key == 'C') {
            clearMaze();
        } else if (key == 'g' || key == 'G') {
            generateMaze();
        }
    }
}
