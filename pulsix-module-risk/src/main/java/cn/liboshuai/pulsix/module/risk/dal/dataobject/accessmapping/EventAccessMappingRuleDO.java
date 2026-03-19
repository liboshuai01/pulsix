package cn.liboshuai.pulsix.module.risk.dal.dataobject.accessmapping;

import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.RiskBaseDO;
import cn.liboshuai.pulsix.module.risk.enums.accessmapping.AccessMappingTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.accessmapping.AccessScriptEngineEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

/**
 * 接入映射标准化规则 DO
 */
@TableName(value = "event_access_mapping_rule", autoResultMap = true)
@KeySequence("event_access_mapping_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TenantIgnore
public class EventAccessMappingRuleDO extends RiskBaseDO {

    @TableId
    private Long id;

    private Long bindingId;

    private String targetFieldName;

    /**
     * 枚举 {@link AccessMappingTypeEnum}
     */
    private String mappingType;

    private String sourceFieldPath;

    private String constantValue;

    /**
     * 枚举 {@link AccessScriptEngineEnum}
     */
    private String scriptEngine;

    private String scriptContent;

    private String timePattern;

    @TableField(value = "enum_mapping_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, String> enumMappingJson;

    private String description;

}
