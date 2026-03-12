package cn.liboshuai.pulsix.module.risk.dal.dataobject.entitytype;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("entity_type_def")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class EntityTypeDO extends BaseDO {

    private Long id;

    private String entityType;

    private String entityName;

    private String keyFieldName;

    private Integer status;

    private String description;

}
