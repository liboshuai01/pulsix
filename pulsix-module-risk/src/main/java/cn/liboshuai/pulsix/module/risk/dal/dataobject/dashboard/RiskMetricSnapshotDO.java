package cn.liboshuai.pulsix.module.risk.dal.dataobject.dashboard;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@TableName(value = "risk_metric_snapshot", autoResultMap = true)
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class RiskMetricSnapshotDO extends BaseDO {

    private Long id;

    private LocalDateTime statTime;

    private String statGranularity;

    private String sceneCode;

    private String metricDomain;

    private String metricCode;

    private String metricName;

    private BigDecimal metricValue;

    private String metricUnit;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metricTagsJson;

}
