package cn.liboshuai.pulsix.module.risk.dal.dataobject.feature;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@TableName(value = "feature_stream_conf", autoResultMap = true)
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class FeatureStreamConfDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private String featureCode;

    private String sourceEventCodes;

    private String entityKeyExpr;

    private String aggType;

    private String valueExpr;

    private String filterExpr;

    private String windowType;

    private String windowSize;

    private String windowSlide;

    private Integer includeCurrentEvent;

    private Long ttlSeconds;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> stateHintJson;

    private Integer status;

}
