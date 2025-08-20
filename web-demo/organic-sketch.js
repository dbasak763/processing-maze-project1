// Organic Physics-Maze Web Demo - Curve Evolution Implementation
// Based on user's comprehensive curve evolution guidance

// Mode enums
const MODE_NONE = 0;
const MODE_DRAW = 1;
const MODE_ERASE = 2;
const MODE_SELECT = 3;
let mode = MODE_DRAW;

// UI elements
let btnW = 80, btnH = 28;
let btnPos = [];

// Interaction thresholds
const SAMPLE_PICK_T = 12;  // click near sample to erase/select
const SEGMENT_PICK_T = 10; // click near segment to insert (draw)

// Evolution parameters (from paper recommendations)
let evolutionParams = {
    fA: 3.0,    // attraction-repulsion strength
    fB: 0.08,   // Brownian noise strength  
    fF: 0.03,   // fairing (Laplacian) strength
    k0: 0.18,   // R0 multiplier
    k1: 0.45,   // R1 multiplier
    nmin: 2,    // minimum neighbor distance
    D: 20,      // base sampling density
    sigma: 0.1, // Brownian noise std dev
    sigmaLJ: 5.0 // Lennard-Jones sigma
};

// Data structures
class Sample {
    constructor(pos, delta = 1.0) {
        this.pos = pos.copy();
        this.prev = pos.copy();
        this.delta = delta;
        this.locked = false;
        this.curveId = -1;
        this.indexInCurve = -1;
    }
    
    verletStep(dt) {
        if (this.locked) return;
        let vel = p5.Vector.sub(this.pos, this.prev);
        this.prev.set(this.pos);
        this.pos.add(vel);
    }
    
    applyForce(force) {
        if (this.locked) return;
        this.pos.add(force);
    }
}

class Curve {
    constructor(id) {
        this.id = id;
        this.samples = [];
        this.closed = false;
        this.type = "labyrinth";
        
        // Evolution parameters
        this.fB = evolutionParams.fB;
        this.fF = evolutionParams.fF;
        this.fA = evolutionParams.fA;
        this.D = evolutionParams.D;
        this.kmin = 0.2;
        this.kmax = 1.2;
        this.nmin = evolutionParams.nmin;
    }
    
    insertSample(index, pos) {
        let sample = new Sample(pos);
        sample.curveId = this.id;
        this.samples.splice(index, 0, sample);
        this.updateSampleIndices();
    }
    
    append(pos) {
        let sample = new Sample(pos);
        sample.curveId = this.id;
        sample.indexInCurve = this.samples.length;
        this.samples.push(sample);
    }
    
    removeIndex(index) {
        if (index >= 0 && index < this.samples.length) {
            this.samples.splice(index, 1);
            this.updateSampleIndices();
        }
    }
    
    updateSampleIndices() {
        for (let i = 0; i < this.samples.length; i++) {
            this.samples[i].indexInCurve = i;
            this.samples[i].curveId = this.id;
        }
    }
    
    getSegments() {
        let segments = [];
        for (let i = 0; i < this.samples.length - 1; i++) {
            segments.push([this.samples[i].pos, this.samples[i + 1].pos]);
        }
        if (this.closed && this.samples.length > 2) {
            segments.push([this.samples[this.samples.length - 1].pos, this.samples[0].pos]);
        }
        return segments;
    }
    
    // Adaptive resampling
    resample() {
        // Split long segments
        for (let i = this.samples.length - 2; i >= 0; i--) {
            let current = this.samples[i];
            let next = this.samples[i + 1];
            
            let segmentLength = p5.Vector.dist(current.pos, next.pos);
            let avgDelta = (current.delta + next.delta) / 2.0;
            let dmax = this.kmax * this.D * avgDelta;
            
            if (segmentLength > dmax) {
                let midPos = p5.Vector.lerp(current.pos, next.pos, 0.5);
                this.insertSample(i + 1, midPos);
            }
        }
        
        // Merge short segments
        for (let i = this.samples.length - 2; i > 0; i--) {
            let current = this.samples[i];
            let prev = this.samples[i - 1];
            let next = this.samples[i + 1];
            
            let avgDelta = (prev.delta + current.delta + next.delta) / 3.0;
            let dmin = this.kmin * this.D * avgDelta;
            
            if (p5.Vector.dist(current.pos, prev.pos) < dmin || 
                p5.Vector.dist(current.pos, next.pos) < dmin) {
                if (!current.locked) {
                    this.removeIndex(i);
                }
            }
        }
    }
}

// Global state
let curves = [];
let curveIdCounter = 0;
let needsRebuildIndex = true;
let spatialHash;
let isPlaying = false;
let showForces = true;

// Performance tracking
let frameCounter = 0;
let avgFPS = 60;

function setup() {
    let canvas = createCanvas(1000, 600);
    canvas.parent('sketch-container');
    
    spatialHash = new SpatialHash(32);
    setupButtons();
    
    // Create initial organic seed: circle instead of grid
    let centerX = width * 0.5;
    let centerY = height * 0.5;
    let radius = 120;
    let numSamples = 50;
    
    curves.push(createCircleCurve(centerX, centerY, radius, numSamples));
    
    updateUI();
}

function setupButtons() {
    btnPos = [
        createVector(10, 10),           // Draw
        createVector(100, 10),          // Erase
        createVector(190, 10),          // Select
        createVector(280, 10),          // Play/Pause
        createVector(370, 10),          // Forces
        createVector(460, 10),          // Clear
        createVector(550, 10)           // Generate
    ];
}

function draw() {
    background(20, 25, 35);
    
    // Evolution step
    if (isPlaying) {
        evolveStep();
    }
    
    // Render curves
    renderCurves();
    
    // Draw UI
    drawButtons();
    drawInfo();
    
    // Rebuild spatial index if needed
    if (needsRebuildIndex) {
        rebuildSpatialIndex();
        needsRebuildIndex = false;
    }
    
    // Update performance
    frameCounter++;
    if (frameCounter % 30 === 0) {
        avgFPS = avgFPS * 0.9 + frameRate() * 0.1;
    }
}

function evolveStep() {
    let dt = 1.0 / 60.0;
    
    // Apply evolution forces to all samples
    for (let curve of curves) {
        for (let sample of curve.samples) {
            if (sample.locked) continue;
            
            let totalForce = createVector(0, 0);
            
            // Brownian noise
            let brownian = computeBrownianForce(sample, curve);
            totalForce.add(brownian);
            
            // Laplacian fairing
            let fairing = computeFairingForce(sample, curve);
            totalForce.add(fairing);
            
            // Attraction-repulsion
            let attraction = computeAttractionRepulsionForce(sample, curve);
            totalForce.add(attraction);
            
            // Apply force
            totalForce.mult(dt);
            sample.applyForce(totalForce);
        }
    }
    
    // Verlet integration
    for (let curve of curves) {
        for (let sample of curve.samples) {
            sample.verletStep(dt);
        }
    }
    
    // Adaptive resampling
    for (let curve of curves) {
        curve.resample();
    }
    
    needsRebuildIndex = true;
}

function computeBrownianForce(sample, curve) {
    let fB = curve.fB;
    if (fB <= 0) return createVector(0, 0);
    
    let dx = randomGaussian() * evolutionParams.sigma;
    let dy = randomGaussian() * evolutionParams.sigma;
    
    return createVector(dx * fB * sample.delta, dy * fB * sample.delta);
}

function computeFairingForce(sample, curve) {
    let fF = curve.fF;
    if (fF <= 0) return createVector(0, 0);
    
    let index = sample.indexInCurve;
    if (index <= 0 || index >= curve.samples.length - 1) {
        if (!curve.closed) return createVector(0, 0);
    }
    
    // Get neighbors
    let prev, next;
    if (curve.closed) {
        prev = curve.samples[(index - 1 + curve.samples.length) % curve.samples.length];
        next = curve.samples[(index + 1) % curve.samples.length];
    } else {
        if (index <= 0 || index >= curve.samples.length - 1) {
            return createVector(0, 0);
        }
        prev = curve.samples[index - 1];
        next = curve.samples[index + 1];
    }
    
    // Weighted Laplacian
    let deltaPrev = prev.delta;
    let deltaNext = next.delta;
    let deltaSum = deltaPrev + deltaNext;
    
    if (deltaSum <= 0) return createVector(0, 0);
    
    let weightedSum = createVector(0, 0);
    weightedSum.add(p5.Vector.mult(prev.pos, deltaNext));
    weightedSum.add(p5.Vector.mult(next.pos, deltaPrev));
    weightedSum.div(deltaSum);
    weightedSum.sub(sample.pos);
    
    return p5.Vector.mult(weightedSum, fF);
}

function computeAttractionRepulsionForce(sample, curve) {
    let fA = curve.fA;
    if (fA <= 0) return createVector(0, 0);
    
    let R1 = evolutionParams.k1 * sample.delta;
    let totalForce = createVector(0, 0);
    
    // Query nearby curves
    let nearbyCurves = queryNearbyCurves(sample, R1);
    
    for (let otherCurve of nearbyCurves) {
        let segments = otherCurve.getSegments();
        
        for (let segIndex = 0; segIndex < segments.length; segIndex++) {
            let segment = segments[segIndex];
            
            // Skip segments from same curve that are too close
            if (otherCurve === curve) {
                let sampleIndex = sample.indexInCurve;
                if (Math.abs(segIndex - sampleIndex) <= curve.nmin) {
                    continue;
                }
            }
            
            // Find closest point on segment
            let closestPoint = projectPointOntoSegment(sample.pos, segment[0], segment[1]);
            let r = p5.Vector.dist(sample.pos, closestPoint);
            
            if (r < R1 && r > 0.001) {
                // Lennard-Jones potential
                let sigmaOverR = evolutionParams.sigmaLJ / r;
                let sigmaOverR6 = Math.pow(sigmaOverR, 6);
                let sigmaOverR12 = sigmaOverR6 * sigmaOverR6;
                let w = sigmaOverR12 - sigmaOverR6;
                
                // Clamp to avoid explosive forces
                w = Math.max(-10, Math.min(10, w));
                
                let direction = p5.Vector.sub(sample.pos, closestPoint);
                direction.normalize();
                direction.mult(w * fA);
                
                totalForce.add(direction);
            }
        }
    }
    
    return totalForce;
}

function renderCurves() {
    // Draw curve segments
    stroke(150, 200, 255, 180);
    strokeWeight(2);
    
    for (let curve of curves) {
        let segments = curve.getSegments();
        for (let segment of segments) {
            line(segment[0].x, segment[0].y, segment[1].x, segment[1].y);
        }
    }
    
    // Draw samples
    noStroke();
    for (let curve of curves) {
        for (let sample of curve.samples) {
            if (sample.locked) {
                fill(100, 255, 100); // Green for locked
            } else {
                fill(255, 100, 100); // Red for movable
            }
            ellipse(sample.pos.x, sample.pos.y, 4, 4);
        }
    }
    
    // Draw force vectors if enabled
    if (showForces && isPlaying) {
        drawForceVectors();
    }
}

function drawForceVectors() {
    stroke(255, 255, 0, 150);
    strokeWeight(1);
    
    for (let curve of curves) {
        for (let sample of curve.samples) {
            if (sample.locked) continue;
            
            let force = p5.Vector.sub(sample.pos, sample.prev);
            force.mult(10); // Scale for visibility
            
            if (force.mag() > 1) {
                line(sample.pos.x, sample.pos.y,
                     sample.pos.x + force.x, sample.pos.y + force.y);
                
                // Arrow head
                push();
                translate(sample.pos.x + force.x, sample.pos.y + force.y);
                rotate(atan2(force.y, force.x));
                line(0, 0, -5, -2);
                line(0, 0, -5, 2);
                pop();
            }
        }
    }
}

function drawButtons() {
    let labels = ["Draw", "Erase", "Select", isPlaying ? "Pause" : "Play", "Forces", "Clear", "Circle"];
    
    for (let i = 0; i < labels.length; i++) {
        let p = btnPos[i];
        let active = (mode === i + 1) || (i === 4 && showForces) || (i === 3 && isPlaying);
        
        stroke(200);
        fill(active ? color(100, 200, 150) : color(60, 70, 85));
        rect(p.x, p.y, btnW, btnH, 5);
        
        fill(255);
        textAlign(CENTER, CENTER);
        text(labels[i], p.x + btnW/2, p.y + btnH/2 + 2);
    }
}

function drawInfo() {
    fill(255, 255, 255, 200);
    textAlign(LEFT);
    text(`FPS: ${avgFPS.toFixed(1)} | Samples: ${getTotalSamples()} | Curves: ${curves.length}`, 10, height - 40);
    text(`fA: ${evolutionParams.fA} | fB: ${evolutionParams.fB} | fF: ${evolutionParams.fF}`, 10, height - 20);
    
    textAlign(RIGHT);
    text("Space: Play/Pause | F: Forces | C: Clear | G: Add Circle", width - 10, height - 20);
}

// Mouse interaction
function mousePressed() {
    // Check button clicks
    for (let i = 0; i < btnPos.length; i++) {
        let p = btnPos[i];
        if (mouseX >= p.x && mouseX <= p.x + btnW && mouseY >= p.y && mouseY <= p.y + btnH) {
            handleButtonClick(i);
            return;
        }
    }
    
    if (mode === MODE_DRAW) {
        // Try insert on nearest segment
        let hit = findNearestSegment(mouseX, mouseY, SEGMENT_PICK_T);
        if (hit) {
            hit.curve.insertSample(hit.insertIndex, createVector(mouseX, mouseY));
            needsRebuildIndex = true;
        } else {
            // Create new curve
            let curve = new Curve(curveIdCounter++);
            curve.append(createVector(mouseX, mouseY));
            curves.push(curve);
            needsRebuildIndex = true;
        }
    } else if (mode === MODE_ERASE) {
        let hit = findNearestSample(mouseX, mouseY, SAMPLE_PICK_T);
        if (hit) {
            hit.curve.removeIndex(hit.index);
            if (hit.curve.samples.length === 0) {
                curves.splice(curves.indexOf(hit.curve), 1);
            }
            needsRebuildIndex = true;
        }
    }
}

function mouseDragged() {
    if (mode === MODE_DRAW) {
        let hit = findNearestSegment(mouseX, mouseY, SEGMENT_PICK_T);
        if (hit) {
            hit.curve.insertSample(hit.insertIndex, createVector(mouseX, mouseY));
            needsRebuildIndex = true;
        } else if (curves.length > 0) {
            let lastCurve = curves[curves.length - 1];
            if (lastCurve.samples.length > 0) {
                let lastSample = lastCurve.samples[lastCurve.samples.length - 1];
                if (p5.Vector.dist(lastSample.pos, createVector(mouseX, mouseY)) > 6) {
                    lastCurve.append(createVector(mouseX, mouseY));
                    needsRebuildIndex = true;
                }
            }
        }
    }
}

function handleButtonClick(index) {
    switch(index) {
        case 0: mode = MODE_DRAW; break;
        case 1: mode = MODE_ERASE; break;
        case 2: mode = MODE_SELECT; break;
        case 3: isPlaying = !isPlaying; break;
        case 4: showForces = !showForces; break;
        case 5: clearAll(); break;
        case 6: addCircle(); break;
    }
    updateUI();
}

function keyPressed() {
    if (key === ' ') {
        isPlaying = !isPlaying;
    } else if (key === 'f' || key === 'F') {
        showForces = !showForces;
    } else if (key === 'c' || key === 'C') {
        clearAll();
    } else if (key === 'g' || key === 'G') {
        addCircle();
    }
    updateUI();
}

// Helper functions
function findNearestSample(x, y, thresh) {
    let best = null;
    for (let curve of curves) {
        for (let i = 0; i < curve.samples.length; i++) {
            let d = dist(x, y, curve.samples[i].pos.x, curve.samples[i].pos.y);
            if (d < thresh) {
                if (!best || d < best.dist) {
                    best = {curve: curve, index: i, dist: d};
                }
            }
        }
    }
    return best;
}

function findNearestSegment(x, y, thresh) {
    let best = null;
    for (let curve of curves) {
        for (let j = 0; j < curve.samples.length - 1; j++) {
            let a = curve.samples[j].pos;
            let b = curve.samples[j + 1].pos;
            let proj = projectPointOntoSegment(createVector(x, y), a, b);
            let d = p5.Vector.dist(proj, createVector(x, y));
            if (d < thresh) {
                if (!best || d < best.dist) {
                    best = {curve: curve, insertIndex: j + 1, dist: d};
                }
            }
        }
    }
    return best;
}

function projectPointOntoSegment(p, a, b) {
    let ap = p5.Vector.sub(p, a);
    let ab = p5.Vector.sub(b, a);
    let ab2 = ab.magSq();
    if (ab2 === 0) return a.copy();
    let t = p5.Vector.dot(ap, ab) / ab2;
    t = constrain(t, 0, 1);
    return p5.Vector.add(a, p5.Vector.mult(ab, t));
}

function createCircleCurve(cx, cy, radius, n) {
    let curve = new Curve(curveIdCounter++);
    curve.closed = true;
    for (let i = 0; i < n; i++) {
        let angle = TWO_PI * i / n;
        let x = cx + cos(angle) * radius;
        let y = cy + sin(angle) * radius;
        curve.append(createVector(x, y));
    }
    return curve;
}

function clearAll() {
    curves = [];
    needsRebuildIndex = true;
}

function addCircle() {
    let cx = random(100, width - 100);
    let cy = random(100, height - 100);
    let radius = random(50, 100);
    curves.push(createCircleCurve(cx, cy, radius, 40));
    needsRebuildIndex = true;
}

function getTotalSamples() {
    return curves.reduce((total, curve) => total + curve.samples.length, 0);
}

function queryNearbyCurves(sample, radius) {
    // Simplified: return all curves for now
    // In production, use spatial hash for efficiency
    return curves;
}

function rebuildSpatialIndex() {
    // Placeholder for spatial index rebuild
    // spatialHash.clear();
    // for each curve, insert samples...
}

function updateUI() {
    // Update button states in HTML if needed
}

// Simple spatial hash implementation
class SpatialHash {
    constructor(cellSize) {
        this.cellSize = cellSize;
        this.buckets = new Map();
    }
    
    clear() {
        this.buckets.clear();
    }
    
    insert(sample) {
        let key = this.getKey(sample.pos.x, sample.pos.y);
        if (!this.buckets.has(key)) {
            this.buckets.set(key, []);
        }
        this.buckets.get(key).push(sample);
    }
    
    getKey(x, y) {
        let ix = Math.floor(x / this.cellSize);
        let iy = Math.floor(y / this.cellSize);
        return `${ix},${iy}`;
    }
}
