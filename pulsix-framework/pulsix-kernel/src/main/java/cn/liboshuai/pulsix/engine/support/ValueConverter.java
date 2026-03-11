package cn.liboshuai.pulsix.engine.support;

import java.math.BigDecimal;
import java.util.Locale;

public final class ValueConverter {

    private ValueConverter() {
    }

    public static boolean asBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public static BigDecimal asDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    public static int asInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    public static long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    public static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public static Object coerce(String rawValue, String valueType) {
        if (rawValue == null) {
            return null;
        }
        if (valueType == null || valueType.isBlank()) {
            return rawValue;
        }
        return switch (valueType.trim().toUpperCase(Locale.ROOT)) {
            case "BOOLEAN", "BOOL" -> asBoolean(rawValue);
            case "INT", "INTEGER" -> asInt(rawValue);
            case "LONG" -> asLong(rawValue);
            case "DECIMAL", "NUMBER", "DOUBLE" -> asDecimal(rawValue);
            case "STRING" -> rawValue;
            default -> rawValue;
        };
    }

    public static Object coerce(Object rawValue, String valueType) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof String stringValue) {
            return coerce(stringValue, valueType);
        }
        if (valueType == null || valueType.isBlank()) {
            return rawValue;
        }
        return switch (valueType.trim().toUpperCase(Locale.ROOT)) {
            case "BOOLEAN", "BOOL" -> asBoolean(rawValue);
            case "INT", "INTEGER" -> asInt(rawValue);
            case "LONG" -> asLong(rawValue);
            case "DECIMAL", "NUMBER", "DOUBLE" -> asDecimal(rawValue);
            case "STRING" -> asString(rawValue);
            default -> rawValue;
        };
    }

}
