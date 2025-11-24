package com.marekcabaj.nmt.bean;

import java.util.EnumMap;
import java.util.Map;

public class NativeMemoryTrackingValues extends EnumMap<NativeMemoryTrackingKind, Map<String, Long>> {

    public NativeMemoryTrackingValues() {
        super(NativeMemoryTrackingKind.class);
    }

}
