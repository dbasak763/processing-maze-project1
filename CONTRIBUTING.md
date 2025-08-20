# Contributing to Physics-Maze

Thank you for your interest in contributing to Physics-Maze! This document provides guidelines and information for contributors.

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Gradle 8.0+
- Git
- A modern web browser (for web demo testing)

### Development Setup
1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/yourusername/physics-maze.git
   cd physics-maze
   ```
3. Build the project:
   ```bash
   cd processing-app
   ./gradlew build
   ```
4. Run tests to ensure everything works:
   ```bash
   ./gradlew test
   ```

## 🎯 How to Contribute

### Reporting Bugs
- Use the GitHub issue tracker
- Include a clear description of the problem
- Provide steps to reproduce the issue
- Include system information (OS, Java version, etc.)
- Add screenshots or GIFs if applicable

### Suggesting Features
- Check existing issues first to avoid duplicates
- Use the feature request template
- Explain the use case and benefits
- Consider implementation complexity

### Code Contributions

#### Good First Issues
Look for issues labeled `good first issue`:
- Add new maze generation algorithms
- Improve UI/UX components
- Add unit tests for existing functionality
- Optimize rendering performance
- Create documentation and tutorials

#### Development Workflow
1. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Make your changes following our coding standards
3. Add or update tests as needed
4. Run the test suite:
   ```bash
   ./gradlew test
   ```
5. Update documentation if necessary
6. Commit your changes with clear messages
7. Push to your fork and create a pull request

## 📝 Coding Standards

### Java Code Style
- Use 4 spaces for indentation (no tabs)
- Follow standard Java naming conventions
- Keep methods under 50 lines when possible
- Add JavaDoc comments for public methods
- Use meaningful variable and method names

### Code Organization
- Keep classes focused and single-purpose
- Separate concerns (physics, rendering, UI)
- Use appropriate design patterns
- Minimize dependencies between components

### Testing
- Write unit tests for new functionality
- Aim for >80% code coverage
- Use descriptive test method names
- Test edge cases and error conditions

## 🏗️ Project Structure

### Core Components
- `PhysicsMaze.java`: Main application and rendering
- `Particle.java`: Physics particle implementation
- `DistanceConstraint.java`: Constraint solving
- `SpatialHash.java`: Collision optimization
- `MazeState.java`: State management for undo/redo

### Adding New Features

#### New Maze Generators
1. Create a new class in `com.physicsmaze.generators`
2. Implement the `MazeGenerator` interface
3. Add UI integration in `PhysicsMaze.java`
4. Write comprehensive tests
5. Update documentation

#### New Physics Features
1. Consider performance implications
2. Maintain backward compatibility
3. Add configuration options where appropriate
4. Test with various particle counts

## 🧪 Testing Guidelines

### Unit Tests
- Test individual components in isolation
- Mock dependencies when necessary
- Use JUnit 4 conventions
- Place tests in `src/test/java`

### Integration Tests
- Test component interactions
- Verify physics simulation accuracy
- Test UI interactions

### Performance Tests
- Benchmark critical paths
- Test with large particle counts
- Monitor memory usage
- Document performance characteristics

## 📚 Documentation

### Code Documentation
- Add JavaDoc for all public methods
- Include parameter descriptions
- Document return values and exceptions
- Provide usage examples

### User Documentation
- Update README.md for new features
- Add screenshots for UI changes
- Update keyboard shortcuts
- Create tutorials for complex features

## 🎨 UI/UX Guidelines

### Design Principles
- Keep interfaces intuitive and discoverable
- Provide immediate visual feedback
- Support keyboard shortcuts for power users
- Ensure accessibility (color contrast, keyboard navigation)

### Visual Consistency
- Use consistent color schemes
- Maintain button and control styling
- Follow established layout patterns
- Test on different screen sizes

## 🚀 Release Process

### Version Numbering
We follow semantic versioning (MAJOR.MINOR.PATCH):
- MAJOR: Breaking changes
- MINOR: New features, backward compatible
- PATCH: Bug fixes, backward compatible

### Release Checklist
- [ ] All tests pass
- [ ] Documentation updated
- [ ] Version numbers bumped
- [ ] Changelog updated
- [ ] Performance benchmarks run
- [ ] Cross-platform testing completed

## 🤝 Community Guidelines

### Code of Conduct
- Be respectful and inclusive
- Welcome newcomers and help them learn
- Focus on constructive feedback
- Celebrate contributions of all sizes

### Communication
- Use clear, descriptive commit messages
- Respond to feedback promptly
- Ask questions when uncertain
- Share knowledge and best practices

## 🏷️ Issue Labels

- `bug`: Something isn't working
- `enhancement`: New feature or request
- `good first issue`: Good for newcomers
- `help wanted`: Extra attention needed
- `performance`: Performance related
- `documentation`: Documentation improvements
- `web-demo`: Web demo specific issues

## 📋 Pull Request Template

When creating a pull request, please include:

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Manual testing completed
- [ ] Performance impact assessed

## Screenshots
(If applicable)

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] Tests pass locally
```

## 🙋 Getting Help

- Check existing documentation first
- Search closed issues for similar problems
- Ask questions in GitHub discussions
- Tag maintainers for urgent issues

## 🎉 Recognition

Contributors will be:
- Listed in the README.md
- Mentioned in release notes
- Invited to join the core team (for significant contributions)

Thank you for contributing to Physics-Maze! 🚀
