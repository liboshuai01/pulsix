package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.json.EngineJson;
import org.apache.flink.api.common.serialization.SerializationSchema;

import java.nio.charset.StandardCharsets;

final class EngineJsonSerializationSchema<T> implements SerializationSchema<T> {

    @Override
    public byte[] serialize(T element) {
        return EngineJson.write(element).getBytes(StandardCharsets.UTF_8);
    }

}
