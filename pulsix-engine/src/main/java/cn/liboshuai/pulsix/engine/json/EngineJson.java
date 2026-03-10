package cn.liboshuai.pulsix.engine.json;

import java.util.List;
import java.util.Objects;

public final class EngineJson {

    private static final JsonCodec DEFAULT_CODEC = new JacksonJsonCodec();

    private static volatile JsonCodec codec = DEFAULT_CODEC;

    private EngineJson() {
    }

    public static JsonCodec codec() {
        return codec;
    }

    public static void setCodec(JsonCodec jsonCodec) {
        codec = Objects.requireNonNull(jsonCodec, "jsonCodec");
    }

    public static void resetCodec() {
        codec = DEFAULT_CODEC;
    }

    public static <T> T read(String text, Class<T> type) {
        return codec.read(text, type);
    }

    public static <T> List<T> readList(String text, Class<T> elementType) {
        return codec.readList(text, elementType);
    }

    public static String write(Object value) {
        return codec.write(value);
    }

}
