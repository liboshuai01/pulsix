package cn.liboshuai.pulsix.module.risk.service.scene;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.ScenePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.SceneSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;

import java.util.List;

public interface SceneService {

    Long createScene(SceneSaveReqVO createReqVO);

    void updateScene(SceneSaveReqVO updateReqVO);

    void updateSceneStatus(Long id, Integer status);

    void deleteScene(Long id);

    SceneDO getScene(Long id);

    PageResult<SceneDO> getScenePage(ScenePageReqVO pageReqVO);

    List<SceneDO> getSimpleSceneList();

    String getDeleteBlockedReason(SceneDO scene);

}
