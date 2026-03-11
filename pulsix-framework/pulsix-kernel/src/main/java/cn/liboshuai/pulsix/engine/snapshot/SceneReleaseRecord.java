package cn.liboshuai.pulsix.engine.snapshot;

import cn.liboshuai.pulsix.engine.model.PublishType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SceneReleaseRecord implements Serializable {

    private Long id;

    @JsonAlias({"scene_code", "sceneCode"})
    private String sceneCode;

    @JsonAlias({"version_no", "versionNo"})
    private Integer versionNo;

    @JsonAlias({"snapshot_json", "snapshotJson"})
    private Object snapshotJson;

    private String checksum;

    @JsonAlias({"publish_type", "publishType"})
    private PublishType publishType;

    @JsonAlias({"publish_status", "publishStatus"})
    private String publishStatus;

    @JsonAlias({"published_at", "publishedAt"})
    private Instant publishedAt;

    @JsonAlias({"effective_from", "effectiveFrom"})
    private Instant effectiveFrom;

    @JsonAlias({"rollback_from_version", "rollbackFromVersion"})
    private Integer rollbackFromVersion;

}
