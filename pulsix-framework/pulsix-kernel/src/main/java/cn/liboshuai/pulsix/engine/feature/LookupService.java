package cn.liboshuai.pulsix.engine.feature;

import cn.liboshuai.pulsix.engine.context.EvalContext;
import cn.liboshuai.pulsix.engine.model.LookupType;
import cn.liboshuai.pulsix.engine.runtime.CompiledSceneRuntime;
import cn.liboshuai.pulsix.engine.support.ValueConverter;

public interface LookupService {

    default Object lookup(LookupType lookupType, String sourceRef, String key) {
        throw new UnsupportedOperationException("lookup by raw key is not implemented");
    }

    default LookupResult lookup(CompiledSceneRuntime.CompiledLookupFeature feature, EvalContext context) {
        if (feature == null || feature.getSpec() == null) {
            return LookupResult.success(null, null);
        }
        String key = feature.getKeyScript() == null ? null : ValueConverter.asString(feature.getKeyScript().execute(context));
        Object defaultValue = ValueConverter.coerce(feature.getSpec().getDefaultValue(), feature.getSpec().getValueType());
        if (key == null || key.isBlank()) {
            return LookupResult.fallback(defaultValue,
                    key,
                    LookupResult.ERROR_KEY_MISSING,
                    "lookup key is blank for feature: " + feature.getSpec().getCode(),
                    LookupResult.FALLBACK_DEFAULT_VALUE);
        }
        Object value = lookup(feature.getSpec().getLookupType(), feature.getSpec().getSourceRef(), key);
        if (value != null) {
            return LookupResult.success(ValueConverter.coerce(value, feature.getSpec().getValueType()), key);
        }
        return LookupResult.fallback(defaultValue,
                key,
                LookupResult.ERROR_VALUE_MISSING,
                "lookup value not found for sourceRef=" + feature.getSpec().getSourceRef(),
                LookupResult.FALLBACK_DEFAULT_VALUE);
    }

}
