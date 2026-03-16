package cn.liboshuai.pulsix.module.risk.dal.dataobject.scene;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import cn.liboshuai.pulsix.framework.tenant.core.aop.TenantIgnore;
import cn.liboshuai.pulsix.module.risk.enums.scene.SceneLogLevelEnum;
import cn.liboshuai.pulsix.module.risk.enums.scene.SceneRuntimeModeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 风控场景定义 DO
 */
@TableName("scene_def")
@KeySequence("scene_def_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TenantIgnore
public class SceneDO extends BaseDO {

    /**
     * 场景主键
     */
    @TableId
    private Long id;
    /**
     * 场景编码
     */
    private String sceneCode;
    /**
     * 场景名称
     */
    private String sceneName;
    /**
     * 场景类型
     */
    private String sceneType;
    /**
     * 运行模式
     *
     * 枚举 {@link SceneRuntimeModeEnum}
     */
    private String runtimeMode;
    /**
     * 默认策略编码
     */
    private String defaultPolicyCode;
    /**
     * 决策超时毫秒
     */
    private Integer decisionTimeoutMs;
    /**
     * 日志级别
     *
     * 枚举 {@link SceneLogLevelEnum}
     */
    private String logLevel;
    /**
     * 状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;
    /**
     * 描述
     */
    private String description;

}
