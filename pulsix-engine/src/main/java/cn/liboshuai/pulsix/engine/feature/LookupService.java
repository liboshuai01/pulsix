package cn.liboshuai.pulsix.engine.feature;

import cn.liboshuai.pulsix.engine.model.LookupType;

public interface LookupService {

    Object lookup(LookupType lookupType, String sourceRef, String key);

}
