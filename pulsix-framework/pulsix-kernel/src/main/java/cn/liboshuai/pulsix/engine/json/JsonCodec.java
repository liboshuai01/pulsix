package cn.liboshuai.pulsix.engine.json;

import java.util.List;

public interface JsonCodec {

    <T> T read(String text, Class<T> type);

    <T> List<T> readList(String text, Class<T> elementType);

    String write(Object value);

}
