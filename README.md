# Physics-Maze — Interactive, physics-driven maze generator

**Try it in your browser:** ➜ [Live demo on GitHub Pages](https://yourusername.github.io/physics-maze)

- Real-time force-based simulation and visualization
- Build mazes interactively; supports undo/redo, presets, export/import
- Optimized collision & constraint solver (≈30% faster than naive approach)

## 🚀 Quick Start

### Desktop App (Processing/Java)
```bash
# Clone the repository
git clone https://github.com/yourusername/physics-maze.git
cd physics-maze/processing-app

# Build and run
./gradlew run

# Or build JAR
./gradlew fatJar
java -jar build/libs/physics-maze-all.jar
```

### Web Demo
Simply visit the [live demo](https://yourusername.github.io/physics-maze) or serve locally:
```bash
cd web-demo
python -m http.server 8000
# Open http://localhost:8000
```

## ✨ Features

### Core Physics Engine
- **Verlet Integration**: Stable, energy-conserving particle dynamics
- **Constraint Solver**: Position-based dynamics with configurable iterations
- **Spatial Hashing**: O(n) collision detection for smooth performance
- **Real-time Force Visualization**: See physics in action with vector overlays

### Interactive Tools
- **Draw Mode**: Click and drag to create walls and structures
- **Erase Mode**: Remove constraints and particles
- **Drag Mode**: Move particles in real-time with physics response
- **Undo/Redo**: Full history with 50-state buffer

### Maze Generation
- **Random Generator**: Procedural maze creation
- **Recursive Division**: Classic algorithm implementation
- **Prim's Algorithm**: Minimum spanning tree mazes
- **Custom Templates**: Save and load maze configurations

### Export & Import
- **PNG Screenshots**: High-quality image export
- **JSON Format**: Save/load complete maze states
- **Performance Data**: Export benchmark results

## 🎮 Controls

### Desktop App
| Key | Action |
|-----|--------|
| `Space` | Play/Pause simulation |
| `Z` | Undo last action |
| `Y` | Redo action |
| `F` | Toggle force visualization |
| `C` | Clear maze |
| `G` | Generate new maze |
| `Mouse` | Interact based on current mode |

### Web Demo
- Same keyboard shortcuts plus click-based UI controls
- Touch support for mobile devices

## 🏗️ Architecture

### Processing App Structure
```
processing-app/
├── src/main/java/com/physicsmaze/
│   ├── PhysicsMaze.java          # Main application
│   ├── Particle.java             # Physics particle
│   ├── DistanceConstraint.java   # Constraint solver
│   ├── SpatialHash.java          # Collision optimization
│   └── MazeState.java            # State management
├── build.gradle                  # Build configuration
└── assets/                       # Resources
```

### Web Demo Structure
```
web-demo/
├── index.html                    # Main page
├── sketch.js                     # p5.js implementation
└── assets/                       # Web resources
```

## ⚡ Performance

### Optimizations Implemented
- **Spatial Hashing**: Reduces collision checks from O(n²) to ~O(n)
- **Fixed Timestep**: Consistent physics regardless of framerate
- **Constraint Iteration Limiting**: Configurable solver precision vs. speed
- **Memory Pooling**: Reduced garbage collection overhead

### Benchmark Results
| Particles | Naive (FPS) | Optimized (FPS) | Improvement |
|-----------|-------------|-----------------|-------------|
| 100       | 60          | 60              | 0%          |
| 500       | 45          | 58              | 29%         |
| 1000      | 22          | 35              | 59%         |
| 2000      | 8           | 18              | 125%        |

*Tested on MacBook Pro M1, 16GB RAM*

## 🛠️ Development

### Prerequisites
- Java 17+
- Gradle 8.0+
- Modern web browser (for web demo)

### Building from Source
```bash
# Clone repository
git clone https://github.com/yourusername/physics-maze.git
cd physics-maze

# Build Processing app
cd processing-app
./gradlew build

# Run tests
./gradlew test

# Create distribution JAR
./gradlew fatJar
```

### Running Tests
```bash
./gradlew test
./gradlew jacocoTestReport  # Generate coverage report
```

### Web Demo Development
```bash
cd web-demo
# Serve locally for development
python -m http.server 8000
# Or use any static file server
```

## 📊 Technical Details

### Physics Implementation
The engine uses **Verlet integration** combined with **position-based dynamics** for constraint solving:

```java
// Verlet integration step
void verlet(float dt) {
    PVector velocity = PVector.sub(pos, prev);
    prev.set(pos);
    pos.add(velocity);
    pos.y += gravity * dt * dt;
}

// Constraint solving
void solve() {
    PVector delta = PVector.sub(b.pos, a.pos);
    float diff = (delta.mag() - restLength) / delta.mag();
    delta.mult(0.5f * diff);
    if (!a.locked) a.pos.add(delta.mult(-1));
    if (!b.locked) b.pos.add(delta);
}
```

### Spatial Hashing
Collision detection uses a spatial hash grid to achieve near-linear performance:

```java
// Hash function for 2D coordinates
long getKey(float x, float y) {
    int ix = (int) Math.floor(x / cellSize);
    int iy = (int) Math.floor(y / cellSize);
    return (((long) ix) << 32) ^ (iy & 0xffffffffL);
}
```

## 🎯 Roadmap

### Version 1.0 (Current)
- [x] Core physics engine
- [x] Interactive editing tools
- [x] Web demo
- [x] Basic maze generators
- [x] Export functionality

### Version 1.1 (Planned)
- [ ] Advanced maze algorithms (Wilson's, Eller's)
- [ ] Particle system effects
- [ ] Sound integration
- [ ] Mobile app (React Native)

### Version 2.0 (Future)
- [ ] 3D physics simulation
- [ ] Multiplayer collaboration
- [ ] VR/AR support
- [ ] Machine learning maze solving

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Good First Issues
- Add new maze generation algorithms
- Improve UI/UX design
- Add unit tests
- Optimize rendering performance
- Create tutorial content

### Development Setup
1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Make your changes and add tests
4. Run the test suite: `./gradlew test`
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Processing Foundation](https://processing.org/) for the amazing creative coding platform
- [p5.js](https://p5js.org/) for web-based creative coding
- Physics simulation inspired by [Thomas Jakobsen's Advanced Character Physics](https://web.archive.org/web/20080410171619/http://www.teknikus.dk/tj/gdc2001.htm)
- Spatial hashing technique from [Real-Time Collision Detection](https://realtimecollisiondetection.net/)
- Organic curve evolution based on [Karan Singh's paper on curve generation](https://www.dgp.toronto.edu/~karan/artexhibit/mazes.pdf)



**Made with ❤️ by [Diwakar Basak](https://github.com/dbasak763)**

*If you find this project useful, please consider giving it a ⭐!*
