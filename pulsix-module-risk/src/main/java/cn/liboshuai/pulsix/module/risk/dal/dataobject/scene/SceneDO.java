package cn.liboshuai.pulsix.module.risk.dal.dataobject.scene;

import cn.liboshuai.pulsix.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("scene_def")
@Data
@EqualsAndHashCode(callSuper = true)
public class SceneDO extends BaseDO {

    private Long id;

    private String sceneCode;

    private String sceneName;

    private String sceneType;

    private String accessMode;

    private String defaultEventCode;

    private String defaultPolicyCode;

    private String standardTopicName;

    private String decisionTopicName;

    private Integer status;

    private String description;

}

