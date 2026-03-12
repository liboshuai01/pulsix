package cn.liboshuai.pulsix.module.risk.enums.list;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum RiskListItemSourceTypeEnum implements ArrayValuable<String> {

    MANUAL("MANUAL", "手工录入"),
    IMPORT_FILE("IMPORT_FILE", "文件导入"),
    API_SYNC("API_SYNC", "接口同步");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(RiskListItemSourceTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
