package cn.liboshuai.pulsix.access.ingest.service.normalize;

import cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize.StandardEventNormalizeResult;

import java.util.Map;

public interface StandardEventNormalizationService {

    StandardEventNormalizeResult normalize(String sourceCode, String sceneCode, String eventCode,
                                           Map<String, Object> rawEventJson);

}
