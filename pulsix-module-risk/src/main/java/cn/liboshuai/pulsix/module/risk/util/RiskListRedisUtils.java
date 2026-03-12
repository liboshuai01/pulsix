package cn.liboshuai.pulsix.module.risk.util;

import cn.hutool.core.util.StrUtil;

import java.util.Locale;
import java.util.Map;

public final class RiskListRedisUtils {

    private RiskListRedisUtils() {
    }

    public static String buildRedisKeyPrefix(String listCode, String listType, String matchType) {
        String normalizedCode = StrUtil.blankToDefault(listCode, "").toUpperCase(Locale.ROOT);
        String normalizedListType = StrUtil.blankToDefault(listType, "").toUpperCase(Locale.ROOT);
        String normalizedMatchType = StrUtil.blankToDefault(matchType, "").toUpperCase(Locale.ROOT);
        String standardCode = switch (normalizedListType) {
            case "BLACK" -> normalizedMatchType + "_BLACKLIST";
            case "WHITE" -> normalizedMatchType + "_WHITELIST";
            case "WATCH" -> normalizedMatchType + "_WATCHLIST";
            default -> null;
        };
        if (standardCode != null && standardCode.equals(normalizedCode)) {
            return "pulsix:list:" + normalizeSegment(listType) + ':' + normalizeSegment(matchType);
        }
        return "pulsix:list:" + normalizeCode(listCode);
    }

    public static String buildRedisItemKey(String listCode, String listType, String matchType, String matchValue) {
        return buildRedisKeyPrefix(listCode, listType, matchType) + ':' + matchValue;
    }

    public static String buildHashValue(String remark, Map<String, Object> extJson) {
        if (extJson != null) {
            Object explicitValue = extJson.get("value");
            if (explicitValue != null && StrUtil.isNotBlank(String.valueOf(explicitValue))) {
                return String.valueOf(explicitValue);
            }
            if (!extJson.isEmpty()) {
                return cn.liboshuai.pulsix.framework.common.util.json.JsonUtils.toJsonString(extJson);
            }
        }
        if (StrUtil.isNotBlank(remark)) {
            return remark;
        }
        return "1";
    }

    private static String normalizeSegment(String value) {
        return StrUtil.blankToDefault(value, "unknown").toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String normalizeCode(String value) {
        return StrUtil.blankToDefault(value, "unknown").toLowerCase(Locale.ROOT).replace('_', ':');
    }

}
