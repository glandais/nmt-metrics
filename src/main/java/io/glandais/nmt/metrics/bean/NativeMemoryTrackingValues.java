package io.glandais.nmt.metrics.bean;

import java.util.EnumMap;
import java.util.Map;

public class NativeMemoryTrackingValues extends EnumMap<NativeMemoryTrackingKind, Map<String, Long>> {

    public NativeMemoryTrackingValues() {
        super(NativeMemoryTrackingKind.class);
    }

}
