package com.kinetirx.common;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** RFC 3339 in UTC, matching the Go "2006-01-02T15:04:05Z07:00" layout used for token expiry. */
public final class TimeFormat {

    private static final DateTimeFormatter RFC3339_SECONDS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private TimeFormat() {}

    public static String rfc3339Seconds(Instant t) {
        return RFC3339_SECONDS.format(t);
    }
}
