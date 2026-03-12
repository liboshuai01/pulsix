package cn.liboshuai.pulsix.engine.flink;

import cn.liboshuai.pulsix.engine.feature.LookupService;

import java.io.Serializable;

@FunctionalInterface
public interface EngineLookupServiceFactory extends Serializable {

    LookupService create();

}
