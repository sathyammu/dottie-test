package com.brimmatech.general.utils;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private static final Pattern PART = Pattern.compile("(?i)(\\d+)(ms|s|m|h|d)");

    public static Duration parse(String raw) {
        String s = raw.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Empty duration");

        if (s.startsWith("P") || s.startsWith("p")) {
            return Duration.parse(s);
        }

        long totalMillis = 0L;
        int pos = 0;
        Matcher m = PART.matcher(s);
        while (m.find()) {
            if (m.start() != pos) {
                throw new IllegalArgumentException("Invalid duration: " + raw);
            }
            long val = Long.parseLong(m.group(1));
            switch (m.group(2).toLowerCase()) {
                case "ms": totalMillis += val; break;
                case "s":  totalMillis += val * 1_000L; break;
                case "m":  totalMillis += val * 60_000L; break;
                case "h":  totalMillis += val * 3_600_000L; break;
                case "d":  totalMillis += val * 86_400_000L; break;
                default: throw new IllegalArgumentException("Bad unit in: " + raw);
            }
            pos = m.end();
        }
        if (pos != s.length()) throw new IllegalArgumentException("Invalid duration: " + raw);
        return Duration.ofMillis(totalMillis);
    }
}
