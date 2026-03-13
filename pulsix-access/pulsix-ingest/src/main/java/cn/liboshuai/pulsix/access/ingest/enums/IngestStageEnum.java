package cn.liboshuai.pulsix.access.ingest.enums;

import cn.liboshuai.pulsix.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum IngestStageEnum implements ArrayValuable<String> {

    AUTH("AUTH", "鉴权"),
    PARSE("PARSE", "解析"),
    NORMALIZE("NORMALIZE", "标准化"),
    VALIDATE("VALIDATE", "校验"),
    PRODUCE("PRODUCE", "投递");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(IngestStageEnum::getStage).toArray(String[]::new);

    private final String stage;
    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
