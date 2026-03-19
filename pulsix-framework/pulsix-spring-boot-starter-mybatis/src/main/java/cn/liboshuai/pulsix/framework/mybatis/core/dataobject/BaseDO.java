package cn.liboshuai.pulsix.framework.mybatis.core.dataobject;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 基础实体对象
 *
 * @author 芋道源码
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class BaseDO extends AuditBaseDO {

    /**
     * 是否删除
     */
    @TableLogic
    private Boolean deleted;

}
