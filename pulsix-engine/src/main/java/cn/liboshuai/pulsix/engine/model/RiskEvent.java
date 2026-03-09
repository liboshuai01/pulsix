package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class RiskEvent implements Serializable {

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

    private Map<String, Object> ext = new LinkedHashMap<>();

    public String routeKey() {
        return sceneCode;
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
