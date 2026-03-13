package cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StandardEventNormalizeResult {

    private Map<String, Object> rawEventJson;

    private Map<String, Object> standardEventJson;

    private List<String> missingRequiredFields;

    private List<String> defaultedFields;

    private List<String> mappedFields;

}
