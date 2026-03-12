package cn.liboshuai.pulsix.module.risk.dal.dataobject.simulation;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@TableName(value = "simulation_report", autoResultMap = true)
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class SimulationReportDO extends BaseDO {

    private Long id;

    private Long caseId;

    private String sceneCode;

    private Integer versionNo;

    private String traceId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> resultJson;

    private Integer passFlag;

    private Long durationMs;

}
