package cn.liboshuai.pulsix.engine.feature;

import cn.liboshuai.pulsix.engine.model.LookupType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class InMemoryLookupService implements LookupService {

    private final Map<String, Set<String>> setStore = new HashMap<>();

    private final Map<String, Map<String, Object>> hashStore = new HashMap<>();

    private final Map<String, Object> stringStore = new HashMap<>();

    @Override
    public Object lookup(LookupType lookupType, String sourceRef, String key) {
        if (lookupType == null || sourceRef == null || key == null) {
            return null;
        }
        return switch (lookupType) {
            case REDIS_SET -> setStore.getOrDefault(sourceRef, Set.of()).contains(key);
            case REDIS_HASH, DICT -> hashStore.getOrDefault(sourceRef, Map.of()).get(key);
            case REDIS_STRING -> stringStore.get(sourceRef + ':' + key);
        };
    }

    public InMemoryLookupService putSet(String sourceRef, Set<String> values) {
        setStore.put(sourceRef, new HashSet<>(values));
        return this;
    }

    public InMemoryLookupService putHash(String sourceRef, Map<String, Object> values) {
        hashStore.put(sourceRef, new HashMap<>(values));
        return this;
    }

    public InMemoryLookupService putString(String sourceRef, String key, Object value) {
        stringStore.put(sourceRef + ':' + key, value);
        return this;
    }

    public static InMemoryLookupService demo() {
        return new InMemoryLookupService()
                .putSet("pulsix:list:black:device", Set.of("D0009"))
                .putHash("pulsix:profile:user:risk", Map.of(
                        "U1001", "H",
                        "U2002", "L",
                        "U3003", "L",
                        "U4004", "M"
                ));
    }

    @Override
    public String toString() {
        return "InMemoryLookupService{" +
                "setStore=" + setStore.keySet() +
                ", hashStore=" + hashStore.keySet() +
                ", stringStore=" + stringStore.keySet().stream().filter(Objects::nonNull).toList() +
                '}';
    }

}
