package io.glandais.nmt.metrics.bean;

public enum NativeMemoryTrackingKind {

    RESERVED("reserved memory (max possible usage)"),

    COMMITTED("committed memory (real memory used)");

    private final String comment;

    NativeMemoryTrackingKind(final String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

}
