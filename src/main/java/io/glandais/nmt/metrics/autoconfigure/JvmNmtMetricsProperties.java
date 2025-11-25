package io.glandais.nmt.metrics.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for JVM Native Memory Tracking (NMT) metrics.
 * <p>
 * These properties allow customization of NMT metrics behavior in Spring Boot applications.
 * Properties are prefixed with {@code management.metrics.nmt}.
 * </p>
 */
@ConfigurationProperties(prefix = "management.metrics.nmt")
public class JvmNmtMetricsProperties {

    /**
     * Cache duration for NMT metric values.
     * <p>
     * NMT data retrieval via JMX can be expensive, so values are cached to reduce overhead.
     * This setting controls how long the cached values are valid before being refreshed.
     * </p>
     * <p>
     * Default: 10 seconds
     * </p>
     */
    private Duration cacheDuration = Duration.ofSeconds(10);

    /**
     * Gets the configured cache duration for NMT metrics.
     *
     * @return the cache duration
     */
    public Duration getCacheDuration() {
        return cacheDuration;
    }

    /**
     * Sets the cache duration for NMT metrics.
     *
     * @param cacheDuration the cache duration to set
     */
    public void setCacheDuration(Duration cacheDuration) {
        this.cacheDuration = cacheDuration;
    }

}
