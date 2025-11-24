# Native Memory Tracking (NMT) of Container memory for Java Applications

[![Maven Central](https://img.shields.io/maven-central/v/io.github.glandais/nmt-metrics.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.glandais/nmt-metrics)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://github.com/glandais/nmt-metrics/actions/workflows/release.yml/badge.svg)](https://github.com/glandais/nmt-metrics/actions/workflows/release.yml)

## What does this Library do?

This library exposes JVM [Native Memory Tracking (NMT)](https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/tooldescr007.html) metrics through [Micrometer](https://micrometer.io/), making them available in Spring Boot Actuator and other monitoring systems.

**You get:**
- Real-time visibility into native (off-heap) memory usage
- Per-category memory breakdowns (thread, class, GC, code, etc.)
- Metrics exposed via Prometheus, Grafana, or any Micrometer-compatible backend
- Automated tracking without manual jcmd commands

## Why will I ever need Java Native Memory Tracking?

### The Container Memory Problem

When running Java applications in containers (Docker, Kubernetes, Cloud Foundry), memory management differs from traditional deployments:

- **Hard limits**: Containers enforce strict memory limits. Exceeding them by even 1 byte kills your application.
- **No swap space**: Unlike bare-metal or VMs, containers typically have no swap buffer to absorb memory spikes.
- **Instant OOM kills**: Your app doesn't slow down—it just dies.

### The Native Memory Blind Spot

Standard JVM monitoring focuses on **heap memory** (where Java objects live), but misses **native memory**:

| Memory Type | Standard Monitoring | Visibility |
|-------------|---------------------|------------|
| **Heap memory** | GC logs, heap dumps, profilers | ✅ Excellent |
| **Native memory** (off-heap) | OS-level metrics only | ❌ Poor |

**Native memory includes:**
- Thread stacks
- Class metadata (Metaspace)
- JIT-compiled code
- GC internals
- Direct ByteBuffers

**Without NMT**, diagnosing native memory leaks requires deep Linux internals expertise and manual `jcmd` commands.

**With this library**, you get real-time native memory metrics with per-category breakdowns integrated into your existing monitoring stack.

### Real-World Scenario

```
Container limit: 1GB
Heap configured: 512MB
Expected total usage: ~700MB

❌ Without NMT: App killed at 1GB. No idea why.
✅ With NMT: Metrics show thread stacks growing from 50MB → 300MB over time.
            Leak identified and fixed before production impact.
```

As David Syer notes in ["Spring Boot Memory Performance"](https://spring.io/blog/2015/12/10/spring-boot-memory-performance), relying on JVM diagnostics (not just OS-level metrics) is essential for container environments. Native Memory Tracking reveals the hidden memory allocation patterns beneath the heap, enabling you to understand metrics trends and identify leaking components before they cause OOM failures.

# Requirements

- **Java 11** or higher
- **JVM option**: `-XX:NativeMemoryTracking=summary` (or `detail`)

## Key Dependencies

This library automatically includes:
- **Micrometer Core** (1.16.0+) - Metrics instrumentation library
- **SLF4J API** (2.0.17+) - Logging facade

The library uses lightweight time-based caching (10-second default TTL) to optimize jcmd calls and reduce system overhead.

# Usage

## Include library in your project

The library is available on Maven Central. Check the badge above for the latest version.

```xml
<dependency>
    <groupId>io.github.glandais</groupId>
    <artifactId>nmt-metrics</artifactId>
    <version><!-- Check Maven Central badge for latest version --></version>
</dependency>
```

Gradle:
```gradle
implementation 'io.github.glandais:nmt-metrics:<!-- latest version -->'
```

## Non Spring usage

Create a `JvmNmtMetrics` instance and bind it to the global registry:

```java
public class MyProgram {
    public static void main(String[] args) {
        new JvmNmtMetrics().bindTo(Metrics.globalRegistry);
        //...
    }
}
```

## Spring Boot usage

Add a `JvmNmtMetrics` bean in your context via a `@Configuration`:

```java
@Configuration
public class JvmMetricsConfiguration {
    @Bean
    public MeterBinder jvmNmtMetrics() {
        return new JvmNmtMetrics();
    }
}
```

## Start JVM with option

Start the JVM with command line option: `-XX:NativeMemoryTracking=summary`.

## Use NMT gauges

[Gauges](https://docs.micrometer.io/micrometer/reference/concepts/gauges.html) are added to Micrometer :


* `jvm.memory.nmt.reserved` : Reserved memory gauges, total or per category
    * `jvm.memory.nmt.reserved{category="total"}` : Total reserved memory
    * `jvm.memory.nmt.reserved{category="java.heap"}` : Reserved memory for Java instances
* `jvm.memory.nmt.committed` : Committed memory gauges, total or per category
    * `jvm.memory.nmt.committed{category="total"}` : Total committed memory (the "real" memory usage of JVM process)
    * `jvm.memory.nmt.committed{category="java.heap"}` : Committed memory for Java instances

**Categories** are dynamically extracted from jcmd output and may include: `total`, `java.heap`, `class`, `thread`, `code`, `gc`, `compiler`, `internal`, `symbol`, `native.memory.tracking`, `arena.chunk`, and others depending on your JVM version and configuration.

**Note**: The [NativeMemoryTrackingKind](src/main/java/com/marekcabaj/nmt/bean/NativeMemoryTrackingKind.java) enum defines the metric types (`RESERVED` and `COMMITTED`), not the category names.

If metrics are exposed with Prometheus, `jvm_memory_nmt_committed_bytes{category="thread"}` will display thread memory usage for instance.

# Contributing

This project uses [semantic-release](https://github.com/semantic-release/semantic-release) for automated versioning and publishing to Maven Central.

Please use [Conventional Commits](https://www.conventionalcommits.org/) format for your commit messages:
- `feat:` for new features (minor version bump)
- `fix:` for bug fixes (patch version bump)
- `BREAKING CHANGE:` or `!` suffix for breaking changes (major version bump)
- `docs:`, `style:`, `refactor:`, `test:`, `chore:` for non-releasing changes

See [CONTRIBUTING.md](CONTRIBUTING.md) for more details.

