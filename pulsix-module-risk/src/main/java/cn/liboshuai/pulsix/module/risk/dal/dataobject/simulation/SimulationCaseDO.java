package cn.liboshuai.pulsix.module.risk.dal.dataobject.simulation;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@TableName(value = "simulation_case", autoResultMap = true)
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class SimulationCaseDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private String caseCode;

    private String caseName;

    private String versionSelectMode;

    private Integer versionNo;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> inputEventJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> mockFeatureJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> mockLookupJson;

    private String expectedAction;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> expectedHitRules;

    private Integer status;

    private String description;

}
