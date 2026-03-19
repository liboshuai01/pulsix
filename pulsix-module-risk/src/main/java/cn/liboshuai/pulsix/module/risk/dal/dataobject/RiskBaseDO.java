package cn.liboshuai.pulsix.module.risk.dal.dataobject;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.AuditBaseDO;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 风控模块基础实体对象
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class RiskBaseDO extends AuditBaseDO {
}
