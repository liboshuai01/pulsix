package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
public class SceneSnapshotEnvelope implements Serializable {

    private String sceneCode;

    private Integer version;

    private String checksum;

    private PublishType publishType;

    private Instant publishedAt;

    private Instant effectiveFrom;

    private SceneSnapshot snapshot;

}
