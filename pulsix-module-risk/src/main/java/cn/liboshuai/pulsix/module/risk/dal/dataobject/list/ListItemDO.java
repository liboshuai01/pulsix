package cn.liboshuai.pulsix.module.risk.dal.dataobject.list;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

@TableName(value = "list_item", autoResultMap = true)
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
public class ListItemDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private String listCode;

    private String matchKey;

    private String matchValue;

    private LocalDateTime expireAt;

    private Integer status;

    private String sourceType;

    private String batchNo;

    private String remark;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extJson;

}
