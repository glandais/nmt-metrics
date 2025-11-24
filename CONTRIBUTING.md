# Contributing to NMT Metrics

Thank you for your interest in contributing to NMT Metrics! This document provides guidelines and information for contributors.

## Development Setup

### Prerequisites

- Java 21 or higher
- Maven 3.6+
- Git

### Building the Project

```bash
# Clone the repository
git clone https://github.com/glandais/nmt-metrics.git
cd nmt-metrics

# Build the project
mvn clean install

# Run tests
mvn test
```

## Commit Message Convention

This project uses [Conventional Commits](https://www.conventionalcommits.org/) for automated versioning and changelog generation. All commit messages **must** follow this format:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Commit Types

- **feat**: A new feature (triggers a **minor** version bump: 1.0.0 → 1.1.0)
- **fix**: A bug fix (triggers a **patch** version bump: 1.0.0 → 1.0.1)
- **perf**: Performance improvement (triggers a **patch** version bump)
- **refactor**: Code refactoring without changing functionality (triggers a **patch** version bump)
- **docs**: Documentation changes only (no release)
- **style**: Code style changes (formatting, no logic changes) (no release)
- **test**: Adding or updating tests (no release)
- **chore**: Maintenance tasks, dependency updates (no release)
- **ci**: CI/CD configuration changes (no release)
- **build**: Build system changes (no release)
- **revert**: Reverting a previous commit (triggers a **patch** version bump)

### Breaking Changes

To trigger a **major** version bump (1.0.0 → 2.0.0), use one of these formats:

```bash
# Method 1: BREAKING CHANGE in footer
feat: redesign API structure

BREAKING CHANGE: The JvmNmtMetrics constructor signature has changed

# Method 2: ! after type
feat!: redesign API structure
```

### Examples

```bash
# Feature (minor version bump)
feat: add support for JDK 22 NMT categories

# Bug fix (patch version bump)
fix: handle missing NMT data gracefully

# Performance improvement (patch version bump)
perf: optimize cache hit rate for NMT retrieval

# Refactoring (patch version bump)
refactor: simplify NMTPropertiesExtractor parsing logic

# Documentation (no release)
docs: update README with JDK 21 requirements

# Breaking change (major version bump)
feat!: change MeterBinder implementation to support custom tags

BREAKING CHANGE: JvmNmtMetrics now requires TagProvider in constructor
```

### Scope (Optional)

You can add scope to provide context:

```bash
feat(cache): add configurable TTL for NMT data
fix(parser): handle Java 22 NMT output format
docs(readme): add troubleshooting section
```

## Pull Request Process

1. **Fork the repository** and create your branch from `master`
2. **Make your changes** following the coding standards
3. **Write or update tests** to cover your changes
4. **Ensure all tests pass**: `mvn test`
5. **Use conventional commit messages** for all commits
6. **Open a Pull Request** with a clear description of your changes

### Pull Request Guidelines

- Keep PRs focused on a single feature or fix
- Update documentation if you're changing functionality
- Add tests for new features
- Ensure backward compatibility unless it's a breaking change
- Reference related issues in the PR description

## Release Process

Releases are **fully automated** using [semantic-release](https://github.com/semantic-release/semantic-release):

1. Commits are pushed to the `master` branch (via merged PRs)
2. GitHub Actions workflow triggers automatically
3. semantic-release analyzes commit messages since the last release
4. Version is calculated based on conventional commits:
   - `feat:` → minor version bump (1.0.0 → 1.1.0)
   - `fix:`, `perf:`, `refactor:` → patch version bump (1.0.0 → 1.0.1)
   - `BREAKING CHANGE:` or `!` → major version bump (1.0.0 → 2.0.0)
5. CHANGELOG.md is automatically generated and updated
6. pom.xml version is updated
7. Artifacts are built, signed, and published to Maven Central
8. GitHub release is created with changelog

**You don't need to manually update versions or create releases!**

## Code Style

- Follow Java naming conventions (camelCase for methods/variables, PascalCase for classes)
- Use meaningful variable and method names
- Add Javadoc comments for public APIs
- Keep methods focused and concise (Single Responsibility Principle)
- Write tests for new functionality

## Testing

- All public APIs should have unit tests
- Tests should use descriptive names that explain what they test
- Include edge cases and error scenarios in tests
- Use JUnit for testing framework
- Mock external dependencies appropriately

Example test naming:
```java
@Test
public void shouldReturnEmptyMapWhenNMTDataNotAvailable() {
    // test implementation
}
```

## Questions or Issues?

- **Bug reports**: Open an issue with detailed reproduction steps
- **Feature requests**: Open an issue explaining the use case
- **Questions**: Open a discussion or issue

## License

By contributing to NMT Metrics, you agree that your contributions will be licensed under the Apache License 2.0.

Thank you for contributing! 🎉
