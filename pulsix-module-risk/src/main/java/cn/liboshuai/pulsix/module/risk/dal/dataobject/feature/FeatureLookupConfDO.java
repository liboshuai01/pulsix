package cn.liboshuai.pulsix.module.risk.dal.dataobject.feature;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@TableName(value = "feature_lookup_conf", autoResultMap = true)
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class FeatureLookupConfDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private String featureCode;

    private String lookupType;

    private String keyExpr;

    private String sourceRef;

    private String defaultValue;

    private Long cacheTtlSeconds;

    private Integer timeoutMs;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraJson;

    private Integer status;

}
