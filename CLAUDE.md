# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java library that exposes JVM Native Memory Tracking (NMT) metrics to Micrometer, making them available through Spring Boot Actuator. It helps diagnose memory issues in containerized Java applications where OOM errors are harder to debug.

**Technology Stack:**
- Java 11+ (minimum requirement)
- Maven build system
- Micrometer Core 1.16.0+ (metrics library)
- SLF4J 2.0.17+ (logging)
- Spring Boot 3.x/4.x (optional - for auto-configuration)
- No external dependencies for caching (uses custom lightweight cache)

## Build Commands

```bash
# Build the project
mvn clean install

# Run tests only
mvn test

# Run a single test
mvn test -Dtest=NMTStatsRetrieverTest

# Package without tests
mvn package -DskipTests

# Clean build artifacts
mvn clean
```

## Architecture

### Core Components

**JvmNmtMetrics** (Main Entry Point)
- Implements Micrometer's `MeterBinder` interface
- Manages registration of NMT gauges with MeterRegistry
- Uses lightweight time-based cache (default 10s TTL) via `CachedValue` inner class
- Dynamically adds/removes meters based on available NMT categories
- Creates two gauge types per category: `jvm.memory.nmt.reserved` and `jvm.memory.nmt.committed`
- Converts KB values from NMT to bytes for Micrometer
- Supports configurable cache duration via constructor: `new JvmNmtMetrics(Duration.ofSeconds(30))`

**NMTStatsRetriever** (Data Collection & Parsing)
- Executes `vmNativeMemory` command via JMX DiagnosticCommand MBean
- Uses ManagementFactory.getPlatformMBeanServer() to invoke native JVM diagnostics
- Parses jcmd output text into structured data using regex patterns
- Extracts "Total" line and per-category lines with reserved/committed values
- Returns `NativeMemoryTrackingValues` (EnumMap structure)
- Handles errors gracefully by returning empty values

**NativeMemoryTrackingValues** (Data Model)
- EnumMap<NativeMemoryTrackingKind, Map<String, Long>>
- NativeMemoryTrackingKind: RESERVED or COMMITTED
- Inner map: category name → memory in KB
- TreeMap used for sorted category output

### Spring Boot Auto-Configuration

**JvmNmtMetricsAutoConfiguration** (Spring Boot 3.x/4.x)
- Automatically registers `JvmNmtMetrics` bean when Spring Boot and Micrometer are detected
- Uses `@ConditionalOnClass({MeterRegistry.class, JvmNmtMetrics.class})` for activation
- Uses `@ConditionalOnMissingBean` to allow user override
- Registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Spring Boot dependencies are `<optional>true</optional>` - library works without Spring Boot

**JvmNmtMetricsProperties** (Configuration Properties)
- Prefix: `management.metrics.nmt`
- Property: `cache-duration` (Duration, default: 10s)
- Example: `management.metrics.nmt.cache-duration=30s` in application.properties
- Metadata file: `META-INF/spring-configuration-metadata.json` for IDE autocomplete

### Data Flow

1. MeterRegistry requests metric value via gauge
2. JvmNmtMetrics.getValue() called → checks cache via getVmNativeMemorySummary()
3. Cache hit: return cached value (double-checked locking pattern)
4. Cache miss (synchronized):
   - NMTStatsRetriever.retrieveNativeMemoryTrackingValues()
   - Execute JMX command via DiagnosticCommand MBean
   - Parse output with regex (TOTAL_PATTERN and CATEGORY_PATTERN)
   - Update meters dynamically (add new categories, remove old ones)
   - Store result in cache with expiry timestamp
5. Convert KB to bytes and return to gauge

## Development Guidelines

### JVM Requirement
This library requires the JVM to be started with `-XX:NativeMemoryTracking=summary` or `-XX:NativeMemoryTracking=detail` flag. Without this flag, jcmd will return no data.

### Testing
Tests parse sample jcmd output from different Java versions (Java 8, 9, 11, 17, 21). When adding support for new Java versions, add corresponding sample outputs and test cases.

**Test Structure:**
- `JvmNmtMetricsTest` - Integration tests for core metrics functionality
- `NMTStatsRetrieverTest` - Unit tests for NMT data parsing
- `JvmNmtMetricsAutoConfigurationTest` - Spring Boot auto-configuration tests

**Testing Framework:** JUnit 5 (Jupiter)
- All tests use JUnit 5 annotations: `@Test` from `org.junit.jupiter.api.Test`
- Use JUnit 5 assertions: `org.junit.jupiter.api.Assertions.*`

### Package Structure
- `io.glandais.nmt.metrics` - Main MeterBinder implementation (JvmNmtMetrics)
- `io.glandais.nmt.metrics.autoconfigure` - Spring Boot auto-configuration (JvmNmtMetricsAutoConfiguration, JvmNmtMetricsProperties)
- `io.glandais.nmt.metrics.retriever` - NMT data retrieval and parsing (NMTStatsRetriever)
- `io.glandais.nmt.metrics.bean` - Data models (NativeMemoryTrackingKind enum, NativeMemoryTrackingValues)

### Key Implementation Details

**Caching Mechanism**
- Custom lightweight cache using volatile CachedValue with expiry timestamp
- Double-checked locking pattern for thread-safe cache updates
- No external dependencies (previously considered Caffeine, now removed)
- Cache invalidation based on time, not access patterns

**JMX Integration**
- Uses DiagnosticCommand MBean (`com.sun.management:type=DiagnosticCommand`)
- Invokes `vmNativeMemory` operation with `["summary"]` argument
- No external process execution - pure JMX approach
- Fallback to empty values on JMException (e.g., NMT not enabled)

**Regex Patterns**
```java
// Matches: "Total: reserved=123456KB, committed=78901KB"
TOTAL_PATTERN

// Matches: "- Category Name (reserved=123KB, committed=456KB)"
CATEGORY_PATTERN
```
Category names are normalized: spaces → dots, lowercase (e.g., "Java Heap" → "java.heap")

### Caching Strategy
The 10-second cache prevents excessive JMX calls which can impact performance. When modifying caching behavior, consider:
- Metrics scrape intervals (typically 10-60s)
- JMX invocation overhead (~10-50ms)
- Memory consumption vs freshness trade-offs
- Cache is duration-based (not LRU/access-based)

### Release Process
This project uses semantic-release for automated versioning and Maven Central publishing:
- Commit messages must follow Conventional Commits format
- `feat:` triggers minor version bump
- `fix:` triggers patch version bump
- `BREAKING CHANGE:` or `!` suffix triggers major version bump
- GitHub Actions workflow handles release on push to master

