package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.support.CollectionCopier;
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

    public void setSourceEventTypes(List<String> sourceEventTypes) {
        this.sourceEventTypes = CollectionCopier.copyList(sourceEventTypes);
    }

}
