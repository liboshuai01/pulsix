package cn.liboshuai.pulsix.engine.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Objects;

final class JacksonJsonCodec implements JsonCodec {

    private final ObjectMapper objectMapper;

    JacksonJsonCodec() {
        this(new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    JacksonJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public <T> T read(String text, Class<T> type) {
        try {
            return objectMapper.readValue(text, type);
        } catch (Exception exception) {
            throw new IllegalStateException("read json failed", exception);
        }
    }

    @Override
    public <T> List<T> readList(String text, Class<T> elementType) {
        try {
            return objectMapper.readValue(text,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (Exception exception) {
            throw new IllegalStateException("read json list failed", exception);
        }
    }

    @Override
    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("write json failed", exception);
        }
    }

}
