package cn.liboshuai.pulsix.engine.feature;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import cn.liboshuai.pulsix.engine.model.LookupType;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.support.ValueConverter;

public interface LookupService {

    Object lookup(LookupType lookupType, String sourceRef, String key);

    default Object lookup(CompiledSceneRuntime.CompiledLookupFeature feature, EvalContext context) {
        if (feature == null || feature.getSpec() == null) {
            return null;
        }
        String key = feature.getKeyScript() == null ? null : ValueConverter.asString(feature.getKeyScript().execute(context));
        return lookup(feature.getSpec().getLookupType(), feature.getSpec().getSourceRef(), key);
    }

}
