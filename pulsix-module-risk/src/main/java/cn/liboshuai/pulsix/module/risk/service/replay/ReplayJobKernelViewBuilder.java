package cn.liboshuai.pulsix.module.risk.service.replay;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobDetailRespVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReplayJobKernelViewBuilder {

    public void apply(ReplayJobDetailRespVO respVO) {
        if (respVO == null) {
            return;
        }
        Map<String, Object> summaryJson = ensureObject(respVO.getSummaryJson());
        List<Map<String, Object>> sampleDiffJson = ensureObjectList(respVO.getSampleDiffJson());
        Map<String, Object> baselineSource = firstNonEmptyObject(respVO.getBaseline(), summaryJson.get("baseline"));
        Map<String, Object> candidateSource = firstNonEmptyObject(respVO.getCandidate(), summaryJson.get("candidate"));

        respVO.setSummaryJson(summaryJson);
        respVO.setSampleDiffJson(sampleDiffJson);
        respVO.setEventCount(firstNonNullInteger(respVO.getEventCount(), asInteger(summaryJson.get("eventCount")), respVO.getEventTotalCount()));
        respVO.setChangedEventCount(firstNonNullInteger(respVO.getChangedEventCount(), asInteger(summaryJson.get("changedEventCount")), respVO.getDiffEventCount()));
        respVO.setChangeRate(firstNonNullDouble(respVO.getChangeRate(), asDouble(summaryJson.get("changeRate")),
                calculateChangeRate(respVO.getChangedEventCount(), respVO.getEventCount())));
        respVO.setBaseline(buildSnapshotRef(baselineSource));
        respVO.setCandidate(buildSnapshotRef(candidateSource));
        respVO.setBaselineSummary(buildReplaySummary(firstNonEmptyObject(respVO.getBaselineSummary(), baselineSource)));
        respVO.setCandidateSummary(buildReplaySummary(firstNonEmptyObject(respVO.getCandidateSummary(), candidateSource)));
        respVO.setTopChangeTypes(ensureIntegerMap(firstNonEmptyObject(respVO.getTopChangeTypes(), summaryJson.get("topChangeTypes"))));
        respVO.setDifferences(buildDifferences(firstNonEmptyList(respVO.getDifferences(), sampleDiffJson)));
        respVO.setGoldenCase(firstNonEmptyObject(respVO.getGoldenCase(), summaryJson.get("goldenCase")));
        respVO.setGoldenVerification(firstNonEmptyObject(respVO.getGoldenVerification(), summaryJson.get("goldenVerification")));
    }

    private Map<String, Object> buildSnapshotRef(Map<String, Object> source) {
        Map<String, Object> snapshotRef = new LinkedHashMap<>();
        if (source.isEmpty()) {
            return snapshotRef;
        }
        putIfNotBlank(snapshotRef, "snapshotId", asString(source.get("snapshotId")));
        putIfNotNull(snapshotRef, "version", asInteger(source.get("version")));
        putIfNotBlank(snapshotRef, "checksum", asString(source.get("checksum")));
        return snapshotRef;
    }

    private Map<String, Object> buildReplaySummary(Map<String, Object> source) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (source.isEmpty()) {
            return summary;
        }
        summary.put("finalActionCounts", ensureIntegerMap(source.get("finalActionCounts")));
        putIfNotNull(summary, "matchedEventCount", asInteger(source.get("matchedEventCount")));
        return summary;
    }

    private List<Map<String, Object>> buildDifferences(Object value) {
        List<Map<String, Object>> items = ensureObjectList(value);
        List<Map<String, Object>> differences = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, Object> difference = new LinkedHashMap<>();
            putIfNotNull(difference, "eventIndex", asInteger(item.get("eventIndex")));
            putIfNotBlank(difference, "eventId", asString(item.get("eventId")));
            putIfNotBlank(difference, "traceId", asString(item.get("traceId")));
            difference.put("changeTypes", ensureStringList(item.get("changeTypes")));
            difference.put("baselineResult", buildSimulationResult(item, "baseline"));
            difference.put("candidateResult", buildSimulationResult(item, "candidate"));
            differences.add(difference);
        }
        return differences;
    }

    private Map<String, Object> buildSimulationResult(Map<String, Object> difference, String prefix) {
        Map<String, Object> result = ensureObject(difference.get(prefix + "Result"));
        if (!result.isEmpty()) {
            result.putIfAbsent("finalAction", asString(difference.get(prefix + "Action")));
            if (!result.containsKey("hitRules")) {
                result.put("hitRules", toRuleList(difference.get(prefix + "HitRules")));
            }
            if (!result.containsKey("hitReasons")) {
                result.put("hitReasons", ensureStringList(difference.get(prefix + "HitReasons")));
            }
            return result;
        }
        Map<String, Object> projection = new LinkedHashMap<>();
        putIfNotBlank(projection, "finalAction", asString(difference.get(prefix + "Action")));
        List<Map<String, Object>> hitRules = toRuleList(difference.get(prefix + "HitRules"));
        if (!hitRules.isEmpty()) {
            projection.put("hitRules", hitRules);
        }
        List<String> hitReasons = ensureStringList(difference.get(prefix + "HitReasons"));
        if (!hitReasons.isEmpty()) {
            projection.put("hitReasons", hitReasons);
        }
        putIfNotNull(projection, "finalScore", asInteger(difference.get(prefix + "FinalScore")));
        return projection;
    }

    private List<Map<String, Object>> toRuleList(Object value) {
        List<Map<String, Object>> rules = new ArrayList<>();
        if (!(value instanceof List<?> items)) {
            return rules;
        }
        for (Object item : items) {
            if (item instanceof Map<?, ?> source) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                source.forEach((key, element) -> normalized.put(String.valueOf(key), element));
                rules.add(normalized);
                continue;
            }
            String ruleCode = asString(item);
            if (StrUtil.isBlank(ruleCode)) {
                continue;
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("ruleCode", ruleCode.trim());
            rules.add(normalized);
        }
        return rules;
    }

    private Map<String, Integer> ensureIntegerMap(Object value) {
        Map<String, Object> source = ensureObject(value);
        Map<String, Integer> result = new LinkedHashMap<>();
        source.forEach((key, item) -> {
            Integer count = asInteger(item);
            if (count != null) {
                result.put(key, count);
            }
        });
        return result;
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
            if (item instanceof Map<?, ?> source) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                source.forEach((key, element) -> normalized.put(String.valueOf(key), element));
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
            String text = asString(item);
            if (StrUtil.isNotBlank(text)) {
                result.add(text.trim());
            }
        }
        return result;
    }

    private Map<String, Object> firstNonEmptyObject(Object... values) {
        if (values == null) {
            return new LinkedHashMap<>();
        }
        for (Object value : values) {
            Map<String, Object> candidate = ensureObject(value);
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }
        return new LinkedHashMap<>();
    }

    private Object firstNonEmptyList(Object preferred, Object fallback) {
        if (!ensureObjectList(preferred).isEmpty()) {
            return preferred;
        }
        return fallback;
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

    private Double firstNonNullDouble(Double... values) {
        if (values == null) {
            return null;
        }
        for (Double value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = asString(value);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        String text = asString(value);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return Double.valueOf(text.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double calculateChangeRate(Integer changedEventCount, Integer eventCount) {
        int total = eventCount == null ? 0 : eventCount;
        if (total <= 0) {
            return 0D;
        }
        return (changedEventCount == null ? 0 : changedEventCount) * 1.0D / total;
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            target.put(key, value.trim());
        }
    }

    private void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

}
