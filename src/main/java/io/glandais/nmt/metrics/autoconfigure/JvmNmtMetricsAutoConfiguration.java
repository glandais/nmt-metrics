package io.glandais.nmt.metrics.autoconfigure;

import io.glandais.nmt.metrics.JvmNmtMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for JVM Native Memory Tracking (NMT) metrics.
 * <p>
 * This auto-configuration automatically registers a {@link JvmNmtMetrics} bean
 * when Micrometer is present on the classpath. The bean will be bound to the
 * {@link MeterRegistry} and expose NMT metrics through Spring Boot Actuator.
 * </p>
 * <p>
 * The auto-configuration can be customized through application properties using
 * the {@code management.metrics.nmt} prefix. See {@link JvmNmtMetricsProperties}
 * for available configuration options.
 * </p>
 * <p>
 * This auto-configuration will only activate when:
 * </p>
 * <ul>
 *   <li>Micrometer's {@link MeterRegistry} is on the classpath</li>
 *   <li>{@link JvmNmtMetrics} is on the classpath</li>
 *   <li>No custom {@link JvmNmtMetrics} bean has been defined by the user</li>
 * </ul>
 * <p>
 * <b>Note:</b> The JVM must be started with {@code -XX:NativeMemoryTracking=summary}
 * or {@code -XX:NativeMemoryTracking=detail} for NMT metrics to be available.
 * Without this flag, the metrics will return empty values.
 * </p>
 *
 * @see JvmNmtMetrics
 * @see JvmNmtMetricsProperties
 */
@AutoConfiguration
@ConditionalOnClass({ MeterRegistry.class, JvmNmtMetrics.class })
@EnableConfigurationProperties(JvmNmtMetricsProperties.class)
public class JvmNmtMetricsAutoConfiguration {

    /**
     * Creates and configures a {@link JvmNmtMetrics} bean for automatic registration
     * with Micrometer's {@link MeterRegistry}.
     * <p>
     * The bean is configured with the cache duration specified in
     * {@link JvmNmtMetricsProperties}. If no custom value is provided,
     * the default of 10 seconds is used.
     * </p>
     * <p>
     * This bean will only be created if no other {@link JvmNmtMetrics} bean
     * has been defined, allowing users to provide their own custom configuration
     * if needed.
     * </p>
     *
     * @param properties the NMT metrics configuration properties
     * @return a configured {@link JvmNmtMetrics} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public JvmNmtMetrics jvmNmtMetrics(JvmNmtMetricsProperties properties) {
        return new JvmNmtMetrics(properties.getCacheDuration());
    }

}
