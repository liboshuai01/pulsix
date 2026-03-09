package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DerivedFeatureSpec extends FeatureSpec {

    private EngineType engineType;

    private String expr;

    private List<String> dependsOn;

}
