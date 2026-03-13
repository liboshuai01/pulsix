package cn.liboshuai.pulsix.access.ingest.domain.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestSourceConfig {

    private String sourceCode;

    private String sourceName;

    private String sourceType;

    private String authType;

    @Builder.Default
    private Map<String, Object> authConfigJson = new LinkedHashMap<>();

    @Builder.Default
    private Set<String> sceneScope = new LinkedHashSet<>();

    private String standardTopicName;

    private String errorTopicName;

    private Integer rateLimitQps;

    private Integer status;

    private String description;

    public boolean supportsScene(String sceneCode) {
        if (CollUtil.isEmpty(sceneScope)) {
            return true;
        }
        return sceneScope.stream().filter(StrUtil::isNotBlank).anyMatch(item -> StrUtil.equals(item, sceneCode));
    }

}
