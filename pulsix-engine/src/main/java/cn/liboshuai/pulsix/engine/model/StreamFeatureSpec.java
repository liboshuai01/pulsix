package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StreamFeatureSpec extends FeatureSpec {

    private List<String> sourceEventTypes;

    private String entityType;

    private String entityKeyExpr;

    private AggType aggType;

    private String valueExpr;

    private String filterExpr;

    private WindowType windowType;

    private String windowSize;

    private String windowSlide;

    private Boolean includeCurrentEvent;

    private String ttl;

}
