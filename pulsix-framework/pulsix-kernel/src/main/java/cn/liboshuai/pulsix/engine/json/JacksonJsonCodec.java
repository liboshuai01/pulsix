package cn.liboshuai.pulsix.engine.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

final class JacksonJsonCodec implements JsonCodec {

    private final ObjectMapper objectMapper;

    JacksonJsonCodec() {
        this(createObjectMapper());
    }

    JacksonJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    private static ObjectMapper createObjectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(Instant.class, FlexibleInstantDeserializer.INSTANCE);
        return new ObjectMapper().registerModule(javaTimeModule);
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

    private static final class FlexibleInstantDeserializer extends JsonDeserializer<Instant> {

        private static final FlexibleInstantDeserializer INSTANCE = new FlexibleInstantDeserializer();

        private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Shanghai");

        @Override
        public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
                long numericValue = parser.getLongValue();
                return Math.abs(numericValue) >= 100_000_000_000L
                        ? Instant.ofEpochMilli(numericValue)
                        : Instant.ofEpochSecond(numericValue);
            }
            String text = parser.getValueAsString();
            if (text == null || text.isBlank()) {
                return null;
            }
            String normalized = text.trim();
            try {
                return Instant.parse(normalized);
            } catch (Exception ignored) {
            }
            try {
                return OffsetDateTime.parse(normalized).toInstant();
            } catch (Exception ignored) {
            }
            try {
                return LocalDateTime.parse(normalized).atZone(DEFAULT_ZONE_ID).toInstant();
            } catch (Exception ignored) {
            }
            throw InvalidFormatException.from(parser,
                    "Cannot deserialize Instant from value: " + normalized,
                    normalized,
                    Instant.class);
        }
    }

}
