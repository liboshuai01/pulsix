package cn.liboshuai.pulsix.module.risk.service.preview;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StandardEventPreviewResult {

    private Map<String, Object> rawEventJson;

    private Map<String, Object> standardEventJson;

    private List<String> missingRequiredFields;

    private List<String> defaultedFields;

    private List<String> mappedFields;

}
