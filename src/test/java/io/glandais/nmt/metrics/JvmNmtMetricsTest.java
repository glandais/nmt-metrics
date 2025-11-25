package io.glandais.nmt.metrics;

import io.glandais.nmt.metrics.JvmNmtMetrics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Integration test for JvmNmtMetrics.
 * Requires JVM to be started with -XX:NativeMemoryTracking=summary
 */
public class JvmNmtMetricsTest {

    private SimpleMeterRegistry registry;
    private JvmNmtMetrics jvmNmtMetrics;

    @Before
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
        assertFalse("Meters should be registered", meters.isEmpty());

        // Verify we have both reserved and committed metrics
        long reservedCount = meters.stream()
                .filter(m -> m.getId().getName().equals("jvm.memory.nmt.reserved"))
                .count();
        long committedCount = meters.stream()
                .filter(m -> m.getId().getName().equals("jvm.memory.nmt.committed"))
                .count();

        assertTrue("Should have reserved metrics", reservedCount > 0);
        assertTrue("Should have committed metrics", committedCount > 0);
        assertEquals("Reserved and committed counts should match", reservedCount, committedCount);
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

        assertFalse("Should discover at least one category", categories.isEmpty());

        // total category should always be present when NMT is enabled (lowercase in newer Java versions)
        assertTrue("total category should be present", categories.contains("total"));
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

        assertFalse("Should have gauge metrics", gauges.isEmpty());

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
        assertNotNull("total reserved metric should exist", totalReserved);
        assertTrue("total reserved should be positive", totalReserved.value() > 0);

        Gauge totalCommitted = registry.find("jvm.memory.nmt.committed")
                .tag("category", "total")
                .gauge();
        assertNotNull("total committed metric should exist", totalCommitted);
        assertTrue("total committed should be positive", totalCommitted.value() > 0);

        // Committed should be less than or equal to reserved
        assertTrue("Committed should be <= reserved",
                totalCommitted.value() <= totalReserved.value());
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
        assertNotNull("Metric should exist", gauge);
        double firstValue = gauge.value();

        // Wait for cache to expire
        Thread.sleep(150);

        // Get value again (should trigger refresh)
        double secondValue = gauge.value();

        // Values might be the same or different, but both should be positive
        assertTrue("First value should be positive", firstValue > 0);
        assertTrue("Second value should be positive", secondValue > 0);
    }

    @Test
    public void testMetricUnitsAndDescriptions() {
        // When
        jvmNmtMetrics.bindTo(registry);

        // Then
        List<Meter> meters = registry.getMeters();

        for (Meter meter : meters) {
            // Verify base unit is set to bytes
            assertEquals("Base unit should be bytes", "bytes", meter.getId().getBaseUnit());

            // Verify description exists
            String description = meter.getId().getDescription();
            assertNotNull("Description should not be null", description);
            assertFalse("Description should not be empty", description.trim().isEmpty());

            // Verify description mentions reserved or committed
            String name = meter.getId().getName();
            if (name.contains("reserved")) {
                assertTrue("Reserved metric description should mention 'reserved'",
                        description.toLowerCase().contains("reserved"));
            } else if (name.contains("committed")) {
                assertTrue("Committed metric description should mention 'committed'",
                        description.toLowerCase().contains("committed"));
            }
        }
    }
}
