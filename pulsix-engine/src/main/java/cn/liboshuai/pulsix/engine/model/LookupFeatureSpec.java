package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfoFactories;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.flink.api.common.typeinfo.TypeInfo;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeInfo(EngineTypeInfoFactories.LookupFeatureSpecTypeInfoFactory.class)
public class LookupFeatureSpec extends FeatureSpec {

    private LookupType lookupType;

    private String keyExpr;

    private String sourceRef;

    private String defaultValue;

    private Integer timeoutMs;

    private Integer cacheTtlSeconds;

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue == null ? null : String.valueOf(defaultValue);
    }

}
