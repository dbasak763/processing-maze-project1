// Physics-Maze Web Demo - p5.js Implementation
let particles = [];
let constraints = [];
let spatialHash;
let mode = 0; // 0=draw, 1=erase, 2=drag
let showForces = true;
let paused = false;
let draggedParticle = null;
let mouseStart;

// Performance tracking
let fpsHistory = [];
let frameCounter = 0;

// History for undo/redo
let history = [];
let historyIndex = -1;

function setup() {
    let canvas = createCanvas(1000, 600);
    canvas.parent('sketch-container');
    
    spatialHash = new SpatialHash(32);
    mouseStart = createVector();
    
    // Initialize with a simple demo maze
    initializeDemoMaze();
    saveState();
    
    // Update UI
    updateUI();
}

function draw() {
    background(20, 25, 35);
    
    if (!paused) {
        updatePhysics();
    }
    
    render();
    updatePerformanceCounters();
}

function updatePhysics() {
    let dt = 1.0 / 60.0;
    
    // Verlet integration
    for (let p of particles) {
        p.verlet(dt);
    }
    
    // Rebuild spatial hash
    spatialHash.clear();
    for (let p of particles) {
        spatialHash.insert(p);
    }
    
    // Solve constraints (multiple iterations)
    for (let iteration = 0; iteration < 6; iteration++) {
        for (let constraint of constraints) {
            constraint.solve();
        }
        handleCollisions();
    }
    
    // Keep particles in bounds
    for (let p of particles) {
        p.pos.x = constrain(p.pos.x, 10, width - 10);
        p.pos.y = constrain(p.pos.y, 10, height - 10);
    }
}

function handleCollisions() {
    for (let p of particles) {
        let neighbors = spatialHash.queryNeighbors(p);
        for (let other of neighbors) {
            if (p === other) continue;
            
            let delta = p5.Vector.sub(p.pos, other.pos);
            let distance = delta.mag();
            let minDistance = 8.0;
            
            if (distance < minDistance && distance > 0) {
                delta.normalize();
                delta.mult((minDistance - distance) * 0.5);
                
                if (!p.locked) p.pos.add(delta);
                if (!other.locked) other.pos.sub(delta);
            }
        }
    }
}

function render() {
    // Draw constraints (walls)
    stroke(150, 200, 255, 180);
    strokeWeight(2);
    for (let constraint of constraints) {
        line(constraint.a.pos.x, constraint.a.pos.y,
             constraint.b.pos.x, constraint.b.pos.y);
    }
    
    // Draw particles
    noStroke();
    for (let p of particles) {
        if (p.locked) {
            fill(100, 255, 100); // Green for fixed
        } else {
            fill(255, 100, 100); // Red for movable
        }
        ellipse(p.pos.x, p.pos.y, 6, 6);
    }
    
    // Draw force vectors
    if (showForces) {
        drawForceVectors();
    }
    
    // Highlight dragged particle
    if (draggedParticle) {
        stroke(255, 255, 0);
        strokeWeight(3);
        noFill();
        ellipse(draggedParticle.pos.x, draggedParticle.pos.y, 12, 12);
    }
}

function drawForceVectors() {
    stroke(255, 255, 0, 150);
    strokeWeight(1);
    
    for (let p of particles) {
        if (p.locked) continue;
        
        let force = p5.Vector.sub(p.pos, p.prev);
        force.mult(10); // Scale for visibility
        
        if (force.mag() > 1) {
            line(p.pos.x, p.pos.y,
                 p.pos.x + force.x, p.pos.y + force.y);
            
            // Arrow head
            push();
            translate(p.pos.x + force.x, p.pos.y + force.y);
            rotate(atan2(force.y, force.x));
            line(0, 0, -5, -2);
            line(0, 0, -5, 2);
            pop();
        }
    }
}

function updatePerformanceCounters() {
    frameCounter++;
    if (frameCounter % 30 === 0) {
        fpsHistory.push(frameRate());
        if (fpsHistory.length > 10) {
            fpsHistory.shift();
        }
        
        let avgFPS = fpsHistory.reduce((a, b) => a + b, 0) / fpsHistory.length;
        document.getElementById('fps-counter').textContent = avgFPS.toFixed(1) + ' FPS';
        document.getElementById('particle-count').textContent = particles.length;
    }
}

function initializeDemoMaze() {
    particles = [];
    constraints = [];
    
    let gridSize = 25;
    let cols = Math.floor(width / gridSize);
    let rows = Math.floor(height / gridSize);
    
    // Create grid of particles
    let grid = [];
    for (let row = 0; row < rows; row++) {
        grid[row] = [];
        for (let col = 0; col < cols; col++) {
            let pos = createVector(col * gridSize + 50, row * gridSize + 50);
            let particle = new Particle(pos);
            
            // Lock border particles
            if (row === 0 || row === rows-1 || col === 0 || col === cols-1) {
                particle.locked = true;
            }
            
            grid[row][col] = particle;
            particles.push(particle);
        }
    }
    
    // Create constraints
    for (let row = 0; row < rows; row++) {
        for (let col = 0; col < cols; col++) {
            // Horizontal constraints
            if (col < cols - 1) {
                constraints.push(new DistanceConstraint(grid[row][col], grid[row][col + 1]));
            }
            // Vertical constraints
            if (row < rows - 1) {
                constraints.push(new DistanceConstraint(grid[row][col], grid[row + 1][col]));
            }
        }
    }
}

// Particle class
class Particle {
    constructor(pos) {
        this.pos = pos.copy();
        this.prev = pos.copy();
        this.locked = false;
        this.mass = 1.0;
    }
    
    verlet(dt) {
        if (this.locked) return;
        
        let velocity = p5.Vector.sub(this.pos, this.prev);
        this.prev.set(this.pos);
        this.pos.add(velocity);
        
        // Apply gravity
        this.pos.y += 980 * dt * dt * this.mass;
    }
}

// Distance constraint class
class DistanceConstraint {
    constructor(a, b) {
        this.a = a;
        this.b = b;
        this.restLength = p5.Vector.dist(a.pos, b.pos);
        this.stiffness = 1.0;
    }
    
    solve() {
        let delta = p5.Vector.sub(this.b.pos, this.a.pos);
        let currentLength = delta.mag();
        
        if (currentLength === 0) return;
        
        let difference = (currentLength - this.restLength) / currentLength;
        let invMassA = this.a.locked ? 0 : 1.0 / this.a.mass;
        let invMassB = this.b.locked ? 0 : 1.0 / this.b.mass;
        let totalInvMass = invMassA + invMassB;
        
        if (totalInvMass === 0) return;
        
        delta.mult(this.stiffness * difference * 0.5);
        
        if (!this.a.locked) {
            let correctionA = p5.Vector.mult(delta, invMassA / totalInvMass);
            this.a.pos.add(correctionA);
        }
        
        if (!this.b.locked) {
            let correctionB = p5.Vector.mult(delta, -invMassB / totalInvMass);
            this.b.pos.add(correctionB);
        }
    }
}

// Spatial hash class
class SpatialHash {
    constructor(cellSize) {
        this.cellSize = cellSize;
        this.buckets = new Map();
    }
    
    getKey(x, y) {
        let ix = Math.floor(x / this.cellSize);
        let iy = Math.floor(y / this.cellSize);
        return `${ix},${iy}`;
    }
    
    clear() {
        this.buckets.clear();
    }
    
    insert(particle) {
        let key = this.getKey(particle.pos.x, particle.pos.y);
        if (!this.buckets.has(key)) {
            this.buckets.set(key, []);
        }
        this.buckets.get(key).push(particle);
    }
    
    queryNeighbors(particle) {
        let neighbors = [];
        let ix = Math.floor(particle.pos.x / this.cellSize);
        let iy = Math.floor(particle.pos.y / this.cellSize);
        
        for (let dx = -1; dx <= 1; dx++) {
            for (let dy = -1; dy <= 1; dy++) {
                let key = `${ix + dx},${iy + dy}`;
                let bucket = this.buckets.get(key);
                if (bucket) {
                    neighbors.push(...bucket);
                }
            }
        }
        
        return neighbors;
    }
}

// Mouse interaction
function mousePressed() {
    if (mode === 2) { // Drag mode
        let minDist = Infinity;
        let closest = null;
        
        for (let p of particles) {
            let dist = p5.Vector.dist(createVector(mouseX, mouseY), p.pos);
            if (dist < minDist && dist < 20) {
                minDist = dist;
                closest = p;
            }
        }
        
        if (closest) {
            draggedParticle = closest;
            mouseStart.set(mouseX, mouseY);
        }
    }
}

function mouseDragged() {
    if (draggedParticle) {
        draggedParticle.pos.set(mouseX, mouseY);
        draggedParticle.prev.set(mouseX, mouseY);
    }
}

function mouseReleased() {
    if (draggedParticle) {
        saveState();
        draggedParticle = null;
    }
}

// Keyboard shortcuts
function keyPressed() {
    if (key === ' ') {
        paused = !paused;
        updateUI();
    } else if (key === 'f' || key === 'F') {
        showForces = !showForces;
        updateUI();
    } else if (key === 'c' || key === 'C') {
        clearMaze();
    } else if (key === 'g' || key === 'G') {
        generateMaze();
    }
}

// UI Control functions
function setMode(newMode) {
    mode = newMode;
    updateUI();
}

function togglePause() {
    paused = !paused;
    updateUI();
}

function toggleForces() {
    showForces = !showForces;
    updateUI();
}

function clearMaze() {
    particles = [];
    constraints = [];
    saveState();
}

function generateMaze() {
    initializeDemoMaze();
    saveState();
}

function exportMaze() {
    let mazeData = {
        particles: particles.map(p => ({
            x: p.pos.x,
            y: p.pos.y,
            locked: p.locked
        })),
        constraints: constraints.map(c => ({
            a: particles.indexOf(c.a),
            b: particles.indexOf(c.b),
            restLength: c.restLength
        }))
    };
    
    let dataStr = JSON.stringify(mazeData, null, 2);
    let dataBlob = new Blob([dataStr], {type: 'application/json'});
    let url = URL.createObjectURL(dataBlob);
    
    let link = document.createElement('a');
    link.href = url;
    link.download = 'physics-maze.json';
    link.click();
    
    URL.revokeObjectURL(url);
}

function updateUI() {
    // Update button states
    let buttons = document.querySelectorAll('.control-btn');
    buttons.forEach(btn => btn.classList.remove('active'));
    
    if (mode === 0) buttons[0].classList.add('active');
    else if (mode === 1) buttons[1].classList.add('active');
    else if (mode === 2) buttons[2].classList.add('active');
    
    if (showForces) buttons[4].classList.add('active');
}

// State management
function saveState() {
    let state = {
        particles: particles.map(p => ({
            pos: p.pos.copy(),
            prev: p.prev.copy(),
            locked: p.locked,
            mass: p.mass
        })),
        constraints: constraints.map(c => ({
            aIndex: particles.indexOf(c.a),
            bIndex: particles.indexOf(c.b),
            restLength: c.restLength,
            stiffness: c.stiffness
        }))
    };
    
    // Remove future states
    while (history.length > historyIndex + 1) {
        history.pop();
    }
    
    history.push(state);
    historyIndex++;
    
    // Limit history size
    if (history.length > 50) {
        history.shift();
        historyIndex--;
    }
}
