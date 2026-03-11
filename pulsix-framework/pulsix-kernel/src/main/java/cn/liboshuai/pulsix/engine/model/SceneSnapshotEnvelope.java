package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfoFactories;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.api.common.typeinfo.TypeInfo;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@TypeInfo(EngineTypeInfoFactories.SceneSnapshotEnvelopeTypeInfoFactory.class)
public class SceneSnapshotEnvelope implements Serializable {

    private String sceneCode;

    private Integer version;

    private String checksum;

    private PublishType publishType;

    private Instant publishedAt;

    private Instant effectiveFrom;

    private SceneSnapshot snapshot;

}
