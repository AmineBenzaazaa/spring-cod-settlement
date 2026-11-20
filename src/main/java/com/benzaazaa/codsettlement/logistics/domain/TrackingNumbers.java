package com.benzaazaa.codsettlement.logistics.domain;

import java.util.Locale;

/** One normalization rule for tracking numbers, shared by the API and the CSV importer. */
public final class TrackingNumbers {

    private TrackingNumbers() {
    }

    public static String normalize(String raw) {
        return raw == null ? "" : raw.strip().toUpperCase(Locale.ROOT);
    }
}
