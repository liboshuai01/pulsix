package cn.liboshuai.pulsix.engine.flink.typeinfo;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.PojoField;
import org.apache.flink.api.java.typeutils.PojoTypeInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EnginePojoTypeInfos {

    private EnginePojoTypeInfos() {
    }

    public static <T> PojoTypeInfo<T> pojo(Class<T> type, Map<String, TypeInformation<?>> fieldTypes) {
        List<PojoField> pojoFields = new ArrayList<>(fieldTypes.size());
        fieldTypes.forEach((fieldName, fieldType) -> pojoFields.add(new PojoField(findField(type, fieldName), fieldType)));
        return new PojoTypeInfo<>(type, pojoFields);
    }

    private static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new IllegalArgumentException("field not found: " + type.getName() + '#' + fieldName);
    }

}
