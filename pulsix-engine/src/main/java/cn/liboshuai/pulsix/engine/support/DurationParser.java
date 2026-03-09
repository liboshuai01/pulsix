package cn.liboshuai.pulsix.engine.support;

import java.time.Duration;

public final class DurationParser {

    private DurationParser() {
    }

    public static Duration parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Duration.ZERO;
        }
        String text = raw.trim().toLowerCase();
        long amount = Long.parseLong(text.substring(0, text.length() - 1));
        char unit = text.charAt(text.length() - 1);
        return switch (unit) {
            case 's' -> Duration.ofSeconds(amount);
            case 'm' -> Duration.ofMinutes(amount);
            case 'h' -> Duration.ofHours(amount);
            case 'd' -> Duration.ofDays(amount);
            default -> throw new IllegalArgumentException("unsupported duration: " + raw);
        };
    }

}
