package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfoFactories;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.flink.api.common.typeinfo.TypeInfo;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeInfo(EngineTypeInfoFactories.DerivedFeatureSpecTypeInfoFactory.class)
public class DerivedFeatureSpec extends FeatureSpec {

    private EngineType engineType;

    private String expr;

    private List<String> dependsOn;

}
