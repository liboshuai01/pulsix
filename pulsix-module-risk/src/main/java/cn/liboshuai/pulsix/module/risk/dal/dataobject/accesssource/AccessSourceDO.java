package cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 接入源定义 DO
 */
@TableName(value = "access_source_def", autoResultMap = true)
@KeySequence("access_source_def_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TenantIgnore
public class AccessSourceDO extends BaseDO {

    @TableId
    private Long id;

    private String sourceCode;

    private String sourceName;

    private String sourceType;

    private String topicName;

    private Integer rateLimitQps;

    @TableField(value = "allowed_scene_codes_json", typeHandler = JacksonTypeHandler.class)
    private List<String> allowedSceneCodes;

    @TableField(value = "ip_whitelist_json", typeHandler = JacksonTypeHandler.class)
    private List<String> ipWhitelist;

    /**
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;

    private String description;

}
