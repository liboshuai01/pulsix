package cn.liboshuai.pulsix.module.risk.dal.dataobject.feature;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("feature_def")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class FeatureDefDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private String featureCode;

    private String featureName;

    private String featureType;

    private String entityType;

    private String eventCode;

    private String valueType;

    private Integer status;

    private Integer version;

    private String description;

}
