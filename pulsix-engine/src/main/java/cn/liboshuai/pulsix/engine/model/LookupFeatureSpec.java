package cn.liboshuai.pulsix.engine.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LookupFeatureSpec extends FeatureSpec {

    private LookupType lookupType;

    private String keyExpr;

    private String sourceRef;

    private String defaultValue;

    private Integer timeoutMs;

    private Integer cacheTtlSeconds;

    @JsonSetter("defaultValue")
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue == null ? null : String.valueOf(defaultValue);
    }

}
