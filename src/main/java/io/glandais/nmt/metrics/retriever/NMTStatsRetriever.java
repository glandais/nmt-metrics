package io.glandais.nmt.metrics.retriever;

import io.glandais.nmt.metrics.bean.NativeMemoryTrackingKind;
import io.glandais.nmt.metrics.bean.NativeMemoryTrackingValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NMTStatsRetriever {

    private static final Logger LOGGER = LoggerFactory.getLogger(NMTStatsRetriever.class);

    private static final String RESERVED_PROPERTY = "reserved";
    private static final String COMMITTED_PROPERTY = "committed";
    private static final String CATEGORY_PROPERTY = "category";

    private static final Pattern CATEGORY_PATTERN = Pattern
            .compile("-\\s*(?<" + CATEGORY_PROPERTY + ">.*) \\(reserved=(?<" + RESERVED_PROPERTY
                    + ">\\d*)KB, committed=(?<" + COMMITTED_PROPERTY + ">\\d*)KB\\)");

    private static final Pattern TOTAL_PATTERN = Pattern.compile(
            "Total: reserved=(?<" + RESERVED_PROPERTY + ">\\d*)KB, committed=(?<" + COMMITTED_PROPERTY + ">\\d*)KB");

    public static String execute(String command, String... args) throws JMException {
        return (String) ManagementFactory.getPlatformMBeanServer().invoke(
                new ObjectName("com.sun.management:type=DiagnosticCommand"),
                command,
                new Object[]{args},
                new String[]{"[Ljava.lang.String;"});
    }

    public static NativeMemoryTrackingValues retrieveNativeMemoryTrackingValues() {
        try {
            final String output = NMTStatsRetriever.execute("vmNativeMemory", "summary");
            return extractFromNmtOutput(output);
        } catch (JMException e) {
            LOGGER.error("Failed to retrieve vmNativeMemory summary");
            return new NativeMemoryTrackingValues();
        }
    }

    public static NativeMemoryTrackingValues extractFromNmtOutput(final String nmtOutput) {
        final NativeMemoryTrackingValues result = new NativeMemoryTrackingValues();
        for (final NativeMemoryTrackingKind nmtKind : NativeMemoryTrackingKind.values()) {
            result.put(nmtKind, new TreeMap<>());
        }
        extractTotalProperty(result, nmtOutput);
        extractAllCategories(result, nmtOutput);
        LOGGER.debug("Extracted NMT properties : {}", result);

        if (result.isEmpty()) {
            LOGGER.warn(
                    "NMT properties are empty after extraction. Probably something wrong occurred during extraction");
        }
        return result;
    }

    protected static void extractAllCategories(final NativeMemoryTrackingValues result, final String nmtOutput) {
        final Matcher matcher = CATEGORY_PATTERN.matcher(nmtOutput);
        while (matcher.find()) {
            final String categoryString = matcher.group(CATEGORY_PROPERTY);
            final String category = categoryString.replace(' ', '.').toLowerCase();

            final long committed = Long.parseLong(matcher.group(COMMITTED_PROPERTY));
            result.get(NativeMemoryTrackingKind.COMMITTED).put(category, committed);

            final long reserved = Long.parseLong(matcher.group(RESERVED_PROPERTY));
            result.get(NativeMemoryTrackingKind.RESERVED).put(category, reserved);
        }
    }

    protected static void extractTotalProperty(final NativeMemoryTrackingValues result, final String nmtOutput) {
        final Matcher matcher = TOTAL_PATTERN.matcher(nmtOutput);
        if (matcher.find()) {
            final long committed = Long.parseLong(matcher.group(COMMITTED_PROPERTY));
            result.get(NativeMemoryTrackingKind.COMMITTED).put("total", committed);

            final long reserved = Long.parseLong(matcher.group(RESERVED_PROPERTY));
            result.get(NativeMemoryTrackingKind.RESERVED).put("total", reserved);
        }
    }

}
