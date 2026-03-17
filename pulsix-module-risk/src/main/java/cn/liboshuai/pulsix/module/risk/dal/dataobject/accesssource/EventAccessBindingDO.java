package cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 事件接入绑定 DO
 */
@TableName("event_access_binding")
@KeySequence("event_access_binding_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TenantIgnore
public class EventAccessBindingDO extends BaseDO {

    @TableId
    private Long id;

    private String eventCode;

    private String sourceCode;

}
