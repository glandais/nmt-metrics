# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java library that exposes JVM Native Memory Tracking (NMT) metrics to Micrometer, making them available through Spring Boot Actuator. It helps diagnose memory issues in containerized Java applications where OOM errors are harder to debug.

**Technology Stack:**
- Java 21
- Spring Boot 3.5.8
- Maven build system
- Micrometer Core (metrics library)
- Caffeine (caching)

## Build Commands

```bash
# Build the project
mvn clean install

# Run tests only
mvn test

# Run a single test
mvn test -Dtest=NMTExtractorTest

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
- Uses Caffeine cache (default 10s TTL) to avoid excessive jcmd calls
- Dynamically adds/removes meters based on available NMT categories
- Creates two gauge types per category: `jvm.memory.nmt.reserved` and `jvm.memory.nmt.committed`

**NMTJcmdRetriever** (Data Collection Layer)
- Coordinates between command execution and parsing
- Runs `VM.native_memory summary` jcmd command
- Delegates to JcmdCommandRunner for execution and NMTPropertiesExtractor for parsing

**JcmdCommandRunner** (System Integration)
- Executes jcmd commands against the current JVM process
- Auto-detects OS (Unix/Windows) to use correct jcmd binary path
- Retrieves current process PID via ManagementFactory
- Handles cross-platform execution (Unix: `./jcmd`, Windows: `jcmd`)

**NMTPropertiesExtractor** (Parser)
- Parses jcmd output text into structured data
- Extracts reserved and committed memory values per category
- Returns `NativeMemoryTrackingValues` (EnumMap structure)

**NativeMemoryTrackingValues** (Data Model)
- EnumMap<NativeMemoryTrackingKind, Map<String, Long>>
- NativeMemoryTrackingKind: RESERVED or COMMITTED
- Inner map: category name → memory in KB

### Data Flow

1. MeterRegistry requests metric value
2. JvmNmtMetrics checks cache (Caffeine)
3. On cache miss: NMTJcmdRetriever → JcmdCommandRunner → jcmd execution
4. NMTPropertiesExtractor parses output
5. Result cached and meters updated
6. Dynamic meter registration: new categories add meters, removed categories cleanup meters

## Development Guidelines

### JVM Requirement
This library requires the JVM to be started with `-XX:NativeMemoryTracking=summary` or `-XX:NativeMemoryTracking=detail` flag. Without this flag, jcmd will return no data.

### Testing
Tests parse sample jcmd output from different Java versions (Java 8, 9, 11, 17, 21). When adding support for new Java versions, add corresponding sample outputs and test cases.

### Package Structure
- `com.marekcabaj.nmt` - Main MeterBinder implementation
- `com.marekcabaj.nmt.jcmd` - jcmd command execution and parsing
- `com.marekcabaj.nmt.bean` - Data models (enums and value objects)

### Caching Strategy
The 10-second cache prevents excessive jcmd calls which can impact performance. When modifying caching behavior, consider:
- Metrics scrape intervals (typically 10-60s)
- jcmd execution overhead (~10-50ms)
- Memory consumption vs freshness trade-offs
