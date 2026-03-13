package cn.liboshuai.pulsix.framework.common.biz.risk.access.normalize;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardEventFieldDefinition {

    private String fieldCode;

    private String fieldType;

    private String fieldPath;

    private Integer requiredFlag;

    private String defaultValue;

}
