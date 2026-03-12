package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Data
@NoArgsConstructor
public class RiskEvent implements Serializable {

    private static final int PROCESSING_ROUTE_BUCKETS = 64;

    private String eventId;

    private String traceId;

    private String sceneCode;

    private String eventType;

    private Instant eventTime;

    private String userId;

    private String deviceId;

    private String ip;

    private BigDecimal amount;

    private String result;

    private String merchantId;

    private String channel;

    private String province;

    private String city;

    private String currency;

    private Map<String, String> ext = new LinkedHashMap<>();

    public void setExt(Map<String, ?> ext) {
        this.ext = new LinkedHashMap<>();
        if (ext == null) {
            return;
        }
        ext.forEach((key, value) -> this.ext.put(key, value == null ? null : String.valueOf(value)));
    }

    public String routeKey() {
        String sceneRoute = normalizeRoutePart(sceneCode, "scene:default");
        if (deviceId != null && !deviceId.isBlank()) {
            return sceneRoute + "|device:" + deviceId.trim();
        }
        if (userId != null && !userId.isBlank()) {
            return sceneRoute + "|user:" + userId.trim();
        }
        if (ip != null && !ip.isBlank()) {
            return sceneRoute + "|ip:" + ip.trim();
        }
        if (merchantId != null && !merchantId.isBlank()) {
            return sceneRoute + "|merchant:" + merchantId.trim();
        }
        if (eventId != null && !eventId.isBlank()) {
            return sceneRoute + "|event:" + eventId.trim();
        }
        return sceneRoute + "|trace:" + normalizeRoutePart(traceId, "unknown");
    }

    public String processingRouteKey() {
        String sceneRoute = normalizeRoutePart(sceneCode, "scene:default");
        int bucket = Math.floorMod(Objects.hash(
                normalizeRoutePart(eventId, "event:null"),
                normalizeRoutePart(traceId, "trace:null"),
                normalizeRoutePart(userId, "user:null"),
                normalizeRoutePart(deviceId, "device:null"),
                normalizeRoutePart(ip, "ip:null"),
                normalizeRoutePart(merchantId, "merchant:null"),
                eventTime == null ? 0L : eventTime.toEpochMilli()
        ), PROCESSING_ROUTE_BUCKETS);
        return sceneRoute + "|bucket:" + bucket;
    }

    private String normalizeRoutePart(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    public Map<String, Object> toFlatMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("eventId", eventId);
        values.put("traceId", traceId);
        values.put("sceneCode", sceneCode);
        values.put("eventType", eventType);
        values.put("eventTime", eventTime);
        values.put("userId", userId);
        values.put("deviceId", deviceId);
        values.put("ip", ip);
        values.put("amount", amount);
        values.put("result", result);
        values.put("merchantId", merchantId);
        values.put("channel", channel);
        values.put("province", province);
        values.put("city", city);
        values.put("currency", currency);
        if (ext != null) {
            values.putAll(ext);
        }
        return values;
    }

}
