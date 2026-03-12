package cn.liboshuai.pulsix.module.risk.dal.dataobject.release;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

@TableName(value = "scene_release", autoResultMap = true)
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class SceneReleaseDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private Integer versionNo;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> snapshotJson;

    private String checksum;

    private String publishStatus;

    private String validationStatus;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> validationReportJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> dependencyDigestJson;

    private Long compileDurationMs;

    private Integer compiledFeatureCount;

    private Integer compiledRuleCount;

    private Integer compiledPolicyCount;

    private String publishedBy;

    private LocalDateTime publishedAt;

    private LocalDateTime effectiveFrom;

    private Integer rollbackFromVersion;

    private String remark;

}
