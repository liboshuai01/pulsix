package cn.liboshuai.pulsix.module.risk.dal.dataobject.replay;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@TableName(value = "replay_job", autoResultMap = true)
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class ReplayJobDO extends BaseDO {

    private Long id;

    private String jobCode;

    private String sceneCode;

    private Integer baselineVersionNo;

    private Integer targetVersionNo;

    private String inputSourceType;

    private String inputRef;

    private String jobStatus;

    private Integer eventTotalCount;

    private Integer diffEventCount;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> summaryJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> sampleDiffJson;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String remark;

}
