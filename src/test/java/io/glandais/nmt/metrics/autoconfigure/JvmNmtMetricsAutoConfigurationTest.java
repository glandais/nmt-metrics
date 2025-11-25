package io.glandais.nmt.metrics.autoconfigure;

import io.glandais.nmt.metrics.JvmNmtMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JvmNmtMetricsAutoConfiguration}.
 */
public class JvmNmtMetricsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JvmNmtMetricsAutoConfiguration.class));

    /**
     * Test that auto-configuration creates JvmNmtMetrics bean when MeterRegistry is present.
     */
    @Test
    public void shouldCreateJvmNmtMetricsBeanWhenMeterRegistryPresent() {
        this.contextRunner
                .withUserConfiguration(MeterRegistryConfiguration.class)
                .run(context -> {
                    assertNotNull(context.getBean(JvmNmtMetrics.class), "JvmNmtMetrics bean should be created");
                    assertNotNull(context.getBean(MeterRegistry.class), "MeterRegistry should be present");
                });
    }

    /**
     * Test that auto-configuration does NOT create JvmNmtMetrics bean when MeterRegistry is absent.
     */
    @Test
    public void shouldNotCreateJvmNmtMetricsBeanWhenMeterRegistryAbsent() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JvmNmtMetricsAutoConfiguration.class))
                .withClassLoader(new FilteredClassLoader(MeterRegistry.class))
                .run(context -> assertFalse(context.containsBean("jvmNmtMetrics"),
                        "JvmNmtMetrics bean should NOT be created when MeterRegistry is absent"));
    }

    /**
     * Test that custom cache duration from properties is applied.
     */
    @Test
    public void shouldApplyCustomCacheDurationFromProperties() {
        this.contextRunner
                .withUserConfiguration(MeterRegistryConfiguration.class)
                .withPropertyValues("management.metrics.nmt.cache-duration=30s")
                .run(context -> {
                    JvmNmtMetricsProperties properties = context.getBean(JvmNmtMetricsProperties.class);
                    assertEquals(Duration.ofSeconds(30), properties.getCacheDuration(),
                            "Custom cache duration should be applied");
                });
    }

    /**
     * Test that default cache duration is used when no custom value is provided.
     */
    @Test
    public void shouldUseDefaultCacheDurationWhenNotConfigured() {
        this.contextRunner
                .withUserConfiguration(MeterRegistryConfiguration.class)
                .run(context -> {
                    JvmNmtMetricsProperties properties = context.getBean(JvmNmtMetricsProperties.class);
                    assertEquals(Duration.ofSeconds(10), properties.getCacheDuration(),
                            "Default cache duration should be 10 seconds");
                });
    }

    /**
     * Test that user-provided JvmNmtMetrics bean takes precedence over auto-configured bean.
     */
    @Test
    public void shouldUseUserProvidedBeanWhenPresent() {
        this.contextRunner
                .withUserConfiguration(MeterRegistryConfiguration.class, CustomJvmNmtMetricsConfiguration.class)
                .run(context -> {
                    JvmNmtMetrics bean = context.getBean(JvmNmtMetrics.class);
                    assertNotNull(bean, "JvmNmtMetrics bean should be present");
                    // Verify it's the custom bean by checking if there's only one bean
                    assertEquals(1, context.getBeansOfType(JvmNmtMetrics.class).size(),
                            "Only one JvmNmtMetrics bean should be present");
                });
    }

    /**
     * Test that properties bean is created.
     */
    @Test
    public void shouldCreatePropertiesBean() {
        this.contextRunner
                .withUserConfiguration(MeterRegistryConfiguration.class)
                .run(context -> {
                    assertNotNull(context.getBean(JvmNmtMetricsProperties.class),
                            "JvmNmtMetricsProperties bean should be created");
                });
    }

    /**
     * Configuration that provides a MeterRegistry bean.
     */
    @Configuration
    static class MeterRegistryConfiguration {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    /**
     * Configuration that provides a custom JvmNmtMetrics bean.
     */
    @Configuration
    static class CustomJvmNmtMetricsConfiguration {
        @Bean
        public JvmNmtMetrics customJvmNmtMetrics() {
            return new JvmNmtMetrics(Duration.ofSeconds(5));
        }
    }

}
