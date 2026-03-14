package cn.liboshuai.pulsix.module.risk.service.simulation;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationReportRespVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SimulationReportKernelViewBuilder {

    public void apply(SimulationReportRespVO respVO) {
        if (respVO == null) {
            return;
        }
        Map<String, Object> resultJson = ensureObject(respVO.getResultJson());
        Map<String, Object> finalResult = ensureObject(firstNonNull(resultJson.get("finalResult"), respVO.getFinalResult()));

        respVO.setResultJson(resultJson);
        respVO.setSnapshotId(firstNonBlank(respVO.getSnapshotId(), asString(resultJson.get("snapshotId"))));
        respVO.setUsedVersion(firstNonNullInteger(respVO.getUsedVersion(), asInteger(resultJson.get("usedVersion")), asInteger(finalResult.get("usedVersion"))));
        respVO.setChecksum(firstNonBlank(respVO.getChecksum(), asString(resultJson.get("checksum"))));
        respVO.setEventCount(firstNonNullInteger(respVO.getEventCount(), asInteger(resultJson.get("eventCount"))));
        respVO.setOverridesApplied(firstNonNullBoolean(respVO.getOverridesApplied(), asBoolean(resultJson.get("overridesApplied"))));
        respVO.setFinalResult(finalResult);
        respVO.setResults(ensureObjectList(firstNonNull(resultJson.get("results"), respVO.getResults())));
        respVO.setFinalAction(firstNonBlank(respVO.getFinalAction(), asString(firstNonNull(finalResult.get("finalAction"), resultJson.get("finalAction")))));
        respVO.setFinalScore(firstNonNullInteger(respVO.getFinalScore(), asInteger(firstNonNull(finalResult.get("finalScore"), resultJson.get("finalScore")))));
        respVO.setTotalScore(firstNonNullInteger(respVO.getTotalScore(), asInteger(firstNonNull(finalResult.get("totalScore"), resultJson.get("totalScore")))));
        respVO.setReason(firstNonBlank(respVO.getReason(), asString(firstNonNull(finalResult.get("reason"), resultJson.get("reason")))));
        respVO.setHitRules(ensureObjectList(firstNonNull(finalResult.get("hitRules"), resultJson.get("hitRules"), respVO.getHitRules())));
        respVO.setHitReasons(ensureStringList(firstNonNull(finalResult.get("hitReasons"), resultJson.get("hitReasons"), respVO.getHitReasons())));
        respVO.setFeatureSnapshot(ensureObject(firstNonNull(finalResult.get("featureSnapshot"), resultJson.get("featureSnapshot"), respVO.getFeatureSnapshot())));
        respVO.setTrace(ensureStringList(firstNonNull(finalResult.get("trace"), resultJson.get("trace"), respVO.getTrace())));
        if (StrUtil.isBlank(respVO.getTraceId())) {
            respVO.setTraceId(firstNonBlank(asString(finalResult.get("traceId")), asString(resultJson.get("traceId"))));
        }
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private Integer firstNonNullInteger(Integer... values) {
        if (values == null) {
            return null;
        }
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Boolean firstNonNullBoolean(Boolean... values) {
        if (values == null) {
            return null;
        }
        for (Boolean value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return NumberUtil.isInteger(String.valueOf(value)) ? Integer.valueOf(String.valueOf(value)) : null;
    }

    private Boolean asBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String text = String.valueOf(value);
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        return null;
    }

    private Map<String, Object> ensureObject(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private List<Map<String, Object>> ensureObjectList(Object value) {
        if (!(value instanceof List<?> items)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                map.forEach((key, element) -> normalized.put(String.valueOf(key), element));
                result.add(normalized);
            }
        }
        return result;
    }

    private List<String> ensureStringList(Object value) {
        if (!(value instanceof List<?> items)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (Object item : items) {
            if (item != null) {
                String text = String.valueOf(item);
                if (StrUtil.isNotBlank(text)) {
                    result.add(text);
                }
            }
        }
        return result;
    }

}
