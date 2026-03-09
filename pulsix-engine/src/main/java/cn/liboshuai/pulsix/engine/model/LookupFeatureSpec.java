package cn.liboshuai.pulsix.engine.model;

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

    private Object defaultValue;

    private Integer timeoutMs;

    private Integer cacheTtlSeconds;

}
