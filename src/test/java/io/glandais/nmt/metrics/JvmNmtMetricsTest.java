package io.glandais.nmt.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for JvmNmtMetrics.
 * Requires JVM to be started with -XX:NativeMemoryTracking=summary
 */
public class JvmNmtMetricsTest {

    private SimpleMeterRegistry registry;
    private JvmNmtMetrics jvmNmtMetrics;

    @BeforeEach
    public void setUp() {
        registry = new SimpleMeterRegistry();
        jvmNmtMetrics = new JvmNmtMetrics();
    }

    @Test
    public void testBindToRegistersMeters() {
        // When
        jvmNmtMetrics.bindTo(registry);

        // Then
        List<Meter> meters = registry.getMeters();
        assertFalse(meters.isEmpty(), "Meters should be registered");

        // Verify we have both reserved and committed metrics
        long reservedCount = meters.stream()
                .filter(m -> m.getId().getName().equals("jvm.memory.nmt.reserved"))
                .count();
        long committedCount = meters.stream()
                .filter(m -> m.getId().getName().equals("jvm.memory.nmt.committed"))
                .count();

        assertTrue(reservedCount > 0, "Should have reserved metrics");
        assertTrue(committedCount > 0, "Should have committed metrics");
        assertEquals(reservedCount, committedCount, "Reserved and committed counts should match");
    }

    @Test
    public void testNmtCategoriesAvailable() {
        // When
        jvmNmtMetrics.bindTo(registry);

        // Then
        Set<String> categories = registry.getMeters().stream()
                .flatMap(m -> m.getId().getTags().stream())
                .filter(tag -> tag.getKey().equals("category"))
                .map(Tag::getValue)
                .collect(Collectors.toSet());

        System.out.println("=== NMT Categories Discovered ===");
        categories.forEach(category -> System.out.println("  - " + category));
        System.out.println("=== Total Categories: " + categories.size() + " ===");

        assertFalse(categories.isEmpty(), "Should discover at least one category");

        // total category should always be present when NMT is enabled (lowercase in newer Java versions)
        assertTrue(categories.contains("total"), "total category should be present");
    }

    @Test
    public void testMetricValuesArePositive() {
        // When
        jvmNmtMetrics.bindTo(registry);

        // Then
        List<Gauge> gauges = registry.getMeters().stream()
                .filter(m -> m instanceof Gauge)
                .map(m -> (Gauge) m)
                .collect(Collectors.toList());

        assertFalse(gauges.isEmpty(), "Should have gauge metrics");

        System.out.println("=== NMT Metric Values ===");
        gauges.forEach(gauge -> {
            String name = gauge.getId().getName();
            String category = gauge.getId().getTag("category");
            double value = gauge.value();
            System.out.printf("  %s [%s] = %.0f bytes (%.2f MB)%n",
                    name, category, value, value / 1024 / 1024);
        });

        // Verify total reserved and committed are positive (lowercase in newer Java versions)
        Gauge totalReserved = registry.find("jvm.memory.nmt.reserved")
                .tag("category", "total")
                .gauge();
        assertNotNull(totalReserved, "total reserved metric should exist");
        assertTrue(totalReserved.value() > 0, "total reserved should be positive");

        Gauge totalCommitted = registry.find("jvm.memory.nmt.committed")
                .tag("category", "total")
                .gauge();
        assertNotNull(totalCommitted, "total committed metric should exist");
        assertTrue(totalCommitted.value() > 0, "total committed should be positive");

        // Committed should be less than or equal to reserved
        assertTrue(totalCommitted.value() <= totalReserved.value(), "Committed should be <= reserved");
    }

    @Test
    public void testCacheConfiguration() throws InterruptedException {
        // Test with custom cache duration
        JvmNmtMetrics shortCacheMetrics = new JvmNmtMetrics(Duration.ofMillis(100)); // 100ms cache
        SimpleMeterRegistry testRegistry = new SimpleMeterRegistry();

        // When
        shortCacheMetrics.bindTo(testRegistry);

        // Get initial value (using lowercase "total" for newer Java versions)
        Gauge gauge = testRegistry.find("jvm.memory.nmt.reserved")
                .tag("category", "total")
                .gauge();
        assertNotNull(gauge, "Metric should exist");
        double firstValue = gauge.value();

        // Wait for cache to expire
        Thread.sleep(150);

        // Get value again (should trigger refresh)
        double secondValue = gauge.value();

        // Values might be the same or different, but both should be positive
        assertTrue(firstValue > 0, "First value should be positive");
        assertTrue(secondValue > 0, "Second value should be positive");
    }

    @Test
    public void testMetricUnitsAndDescriptions() {
        // When
        jvmNmtMetrics.bindTo(registry);

        // Then
        List<Meter> meters = registry.getMeters();

        for (Meter meter : meters) {
            // Verify base unit is set to bytes
            assertEquals("bytes", meter.getId().getBaseUnit(), "Base unit should be bytes");

            // Verify description exists
            String description = meter.getId().getDescription();
            assertNotNull(description, "Description should not be null");
            assertFalse(description.trim().isEmpty(), "Description should not be empty");

            // Verify description mentions reserved or committed
            String name = meter.getId().getName();
            if (name.contains("reserved")) {
                assertTrue(description.toLowerCase().contains("reserved"),
                        "Reserved metric description should mention 'reserved'");
            } else if (name.contains("committed")) {
                assertTrue(description.toLowerCase().contains("committed"),
                        "Committed metric description should mention 'committed'");
            }
        }
    }
}
