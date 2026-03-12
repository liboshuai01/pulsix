package cn.liboshuai.pulsix.module.risk.dal.dataobject.list;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("list_set")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class ListSetDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private String listCode;

    private String listName;

    private String matchType;

    private String listType;

    private String storageType;

    private String syncMode;

    private String syncStatus;

    private LocalDateTime lastSyncTime;

    private Integer status;

    private String description;

}
