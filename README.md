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

## Prerequisites

Before using this library, ensure:

✅ Java 11+ installed
✅ JVM started with `-XX:NativeMemoryTracking=summary` flag
✅ Micrometer registry available in your application

**⚠️ Critical**: Without the NMT flag, metrics will show -1 values!

## Installation

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

## Integration

### Non-Spring Applications

Create a `JvmNmtMetrics` instance and bind it to your Micrometer registry:

```java
import io.glandais.nmt.metrics.JvmNmtMetrics;
import io.micrometer.core.instrument.Metrics;

import java.time.Duration;

public class MyProgram {
    public static void main(String[] args) {
        // Default configuration (10-second cache)
        new JvmNmtMetrics().bindTo(Metrics.globalRegistry);

        // Or with custom cache duration
        new JvmNmtMetrics(Duration.ofSeconds(30)).bindTo(Metrics.globalRegistry);

        // Your application code...
    }
}
```

### Spring Boot Applications

Add a `JvmNmtMetrics` bean in your context via a `@Configuration`:

```java
import io.glandais.nmt.metrics.JvmNmtMetrics;
import io.micrometer.core.instrument.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JvmMetricsConfiguration {

    @Bean
    public MeterBinder jvmNmtMetrics() {
        // Default 10-second cache (recommended for most cases)
        return new JvmNmtMetrics();

        // Or customize based on your Prometheus scrape interval
        // return new JvmNmtMetrics(Duration.ofSeconds(15));
    }
}
```

**Note**: Cache duration should match or slightly exceed your metrics scrape interval to minimize jcmd overhead while keeping data fresh.

## JVM Configuration

The JVM must be started with Native Memory Tracking enabled. Add this flag to your JVM startup arguments:

```
-XX:NativeMemoryTracking=summary
```

### Platform-Specific Configuration

**IDE (IntelliJ IDEA, Eclipse):**
```
VM options: -XX:NativeMemoryTracking=summary
```

**Maven (Surefire/Failsafe plugins):**
```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-XX:NativeMemoryTracking=summary</argLine>
    </configuration>
</plugin>
```

**Gradle:**
```gradle
tasks.withType(Test) {
    jvmArgs '-XX:NativeMemoryTracking=summary'
}
```

**Spring Boot Maven Plugin:**
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <jvmArguments>-XX:NativeMemoryTracking=summary</jvmArguments>
    </configuration>
</plugin>
```

**Docker:**
```dockerfile
ENV JAVA_OPTS="-XX:NativeMemoryTracking=summary"
CMD ["java", "-XX:NativeMemoryTracking=summary", "-jar", "app.jar"]
```

**Kubernetes:**
```yaml
env:
- name: JAVA_OPTS
  value: "-XX:NativeMemoryTracking=summary"
```

**Command Line:**
```bash
java -XX:NativeMemoryTracking=summary -jar your-app.jar
```

## Understanding the Metrics

This library exposes [Micrometer gauges](https://docs.micrometer.io/micrometer/reference/concepts/gauges.html) for Native Memory Tracking data.

### Metric Types

**`jvm.memory.nmt.reserved`** - Memory reserved by the OS for JVM use (upper bound)
- Unit: bytes
- Tag: `category` (dynamically discovered)
- Example: `jvm_memory_nmt_reserved_bytes{category="total"} 1505280000`

**`jvm.memory.nmt.committed`** - Memory actually used by JVM (current usage)
- Unit: bytes
- Tag: `category` (dynamically discovered)
- Example: `jvm_memory_nmt_committed_bytes{category="thread"} 22537216`

**Key Difference**:
- **Reserved** = What the OS promised to the JVM (can grow to this)
- **Committed** = What the JVM is actually using right now

### Available Categories

Categories are dynamically extracted from jcmd output at runtime. Common categories include:

| Category | Description |
|----------|-------------|
| `total` | **Total JVM memory** (this is your actual container memory usage) |
| `java.heap` | Heap memory for Java objects |
| `thread` | Thread stacks (grows with thread count) |
| `class` | Class metadata (Metaspace in Java 8+) |
| `code` | JIT-compiled code cache |
| `gc` | Garbage collector internal structures |
| `compiler` | JIT compiler overhead |
| `internal` | JVM internal data structures |
| `symbol` | Symbol table (class/method names) |
| `native.memory.tracking` | NMT bookkeeping overhead |
| `arena.chunk` | Arena-based memory allocation |

**Note**: Available categories vary by Java version (8, 11, 17, 21, etc.) and are auto-discovered from your JVM.

### Example Metrics

```
# Total container memory (most important for OOM prevention)
jvm_memory_nmt_committed_bytes{category="total"} 175022080

# Heap usage
jvm_memory_nmt_committed_bytes{category="java.heap"} 47710208

# Thread stack memory
jvm_memory_nmt_committed_bytes{category="thread"} 22537216

# Class metadata
jvm_memory_nmt_committed_bytes{category="class"} 37698560
```

## Verification

### Check Metrics Are Available

**Spring Boot Actuator:**
```bash
# List all NMT metrics
curl http://localhost:8080/actuator/metrics | grep nmt

# Expected output:
# "jvm.memory.nmt.reserved"
# "jvm.memory.nmt.committed"
```

**Query Specific Category:**
```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.nmt.committed?tag=category:thread

# Response shows thread memory usage in bytes
```

**Prometheus Endpoint:**
```bash
curl http://localhost:8080/actuator/prometheus | grep jvm_memory_nmt

# Should show multiple metrics with category labels
```

### Verify NMT is Enabled

If metrics show `-1` values, NMT is not enabled. Verify with jcmd:

```bash
# Get JVM process ID
jps

# Check NMT status
jcmd <PID> VM.native_memory summary

# Should show memory breakdown, not "Native memory tracking is not enabled"
```

## Troubleshooting

### Metrics Show -1 Values

**Cause**: JVM not started with `-XX:NativeMemoryTracking=summary`

**Solution**:
1. Add the flag to JVM startup arguments (see JVM Configuration section)
2. Restart your application
3. Verify with `jcmd <PID> VM.native_memory summary`

### No Metrics Appearing

**Possible causes**:

1. **Bean not registered**:
   - Check `@Configuration` class is component-scanned
   - Add `@ComponentScan` if needed

2. **Micrometer not on classpath**:
   - Verify `micrometer-core` dependency is present

3. **Actuator endpoints disabled**:
   ```properties
   management.endpoints.web.exposure.include=health,metrics,prometheus
   ```

4. **Enable debug logging**:
   ```properties
   logging.level.io.glandais.nmt.metrics=DEBUG
   ```

### High jcmd Overhead

**Cause**: Cache duration too short with frequent metric scraping

**Solution**: Increase cache duration to match scrape interval:
```java
// If Prometheus scrapes every 30 seconds
new JvmNmtMetrics(Duration.ofSeconds(30))
```

### Categories Different Than Documentation

**Normal behavior**: Categories vary by JVM version and configuration. The library automatically discovers available categories from your specific JVM.

## Prometheus Queries

### Basic Queries

**Total Container Memory:**
```promql
jvm_memory_nmt_committed_bytes{category="total"}
```

**Thread Memory Growth Detection:**
```promql
rate(jvm_memory_nmt_committed_bytes{category="thread"}[5m])
```

**Top 5 Memory Consumers:**
```promql
topk(5, jvm_memory_nmt_committed_bytes)
```

### Advanced Queries

**Heap vs Off-Heap Memory:**
```promql
# Heap
jvm_memory_nmt_committed_bytes{category="java.heap"}

# Off-heap (Total - Heap)
jvm_memory_nmt_committed_bytes{category="total"}
  - jvm_memory_nmt_committed_bytes{category="java.heap"}
```

**Memory Utilization Percentage:**
```promql
(jvm_memory_nmt_committed_bytes{category="total"}
 / jvm_memory_nmt_reserved_bytes{category="total"}) * 100
```

**Memory by Category (Stacked):**
```promql
jvm_memory_nmt_committed_bytes{category!="total"}
```

# Contributing

This project uses [semantic-release](https://github.com/semantic-release/semantic-release) for automated versioning and publishing to Maven Central.

Please use [Conventional Commits](https://www.conventionalcommits.org/) format for your commit messages:
- `feat:` for new features (minor version bump)
- `fix:` for bug fixes (patch version bump)
- `BREAKING CHANGE:` or `!` suffix for breaking changes (major version bump)
- `docs:`, `style:`, `refactor:`, `test:`, `chore:` for non-releasing changes

See [CONTRIBUTING.md](CONTRIBUTING.md) for more details.

