package com.marekcabaj.nmt.jcmd;

import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marekcabaj.nmt.bean.NativeMemoryTrackingKind;
import com.marekcabaj.nmt.bean.NativeMemoryTrackingValues;

class NMTPropertiesExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NMTPropertiesExtractor.class);

    private static final String RESERVED_PROPERTY = "reserved";
    private static final String COMMITTED_PROPERTY = "committed";
    private static final String CATEGORY_PROPERTY = "category";

    private static final Pattern CATEGORY_PATTERN = Pattern
            .compile("-\\s*(?<" + CATEGORY_PROPERTY + ">.*) \\(reserved=(?<" + RESERVED_PROPERTY
                    + ">\\d*)KB, committed=(?<" + COMMITTED_PROPERTY + ">\\d*)KB\\)");

    private static final Pattern TOTAL_PATTERN = Pattern.compile(
            "Total: reserved=(?<" + RESERVED_PROPERTY + ">\\d*)KB, committed=(?<" + COMMITTED_PROPERTY + ">\\d*)KB");

    public NMTPropertiesExtractor() {
        super();
    }

    public NativeMemoryTrackingValues extractFromJcmdOutput(final String jcmdOutput) {
        final NativeMemoryTrackingValues result = new NativeMemoryTrackingValues();
        for (final NativeMemoryTrackingKind nmtKind : NativeMemoryTrackingKind.values()) {
            result.put(nmtKind, new TreeMap<>());
        }
        extractTotalProperty(result, jcmdOutput);
        extractAllCategories(result, jcmdOutput);
        LOGGER.debug("Extracted NMT properties : {}", result);

        if (result.isEmpty()) {
            LOGGER.warn(
                    "NMT properties are empty after extraction. Probably something wrong occurred during extraction");
        }
        return result;
    }

    protected void extractAllCategories(final NativeMemoryTrackingValues result, final String jcmdOutput) {
        final Matcher matcher = CATEGORY_PATTERN.matcher(jcmdOutput);
        while (matcher.find()) {
            final String categoryString = matcher.group(CATEGORY_PROPERTY);
            final String category = categoryString.replace(' ', '.').toLowerCase();

            final long committed = Long.parseLong(matcher.group(COMMITTED_PROPERTY));
            result.get(NativeMemoryTrackingKind.COMMITTED).put(category, committed);

            final long reserved = Long.parseLong(matcher.group(RESERVED_PROPERTY));
            result.get(NativeMemoryTrackingKind.RESERVED).put(category, reserved);
        }
    }

    protected void extractTotalProperty(final NativeMemoryTrackingValues result, final String jcmdOutput) {
        final Matcher matcher = TOTAL_PATTERN.matcher(jcmdOutput);
        if (matcher.find()) {
            final long committed = Long.parseLong(matcher.group(COMMITTED_PROPERTY));
            result.get(NativeMemoryTrackingKind.COMMITTED).put("total", committed);

            final long reserved = Long.parseLong(matcher.group(RESERVED_PROPERTY));
            result.get(NativeMemoryTrackingKind.RESERVED).put("total", reserved);
        }
    }

}
