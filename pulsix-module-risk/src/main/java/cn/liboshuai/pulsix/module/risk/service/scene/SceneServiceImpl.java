package cn.liboshuai.pulsix.module.risk.service.scene;

import cn.hutool.core.util.ObjectUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.ScenePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.SceneSaveReqVO;
import cn.liboshuai.pulsix.module.risk.convert.scene.SceneConvert;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.*;

@Service
@Validated
public class SceneServiceImpl implements SceneService {

    @Resource
    private SceneMapper sceneMapper;

    @Override
    public Long createScene(SceneSaveReqVO createReqVO) {
        validateSceneCodeUnique(null, createReqVO.getSceneCode());

        SceneDO scene = SceneConvert.INSTANCE.convert(createReqVO);
        sceneMapper.insert(scene);
        return scene.getId();
    }

    @Override
    public void updateScene(SceneSaveReqVO updateReqVO) {
        SceneDO scene = validateSceneExists(updateReqVO.getId());
        if (scene == null) {
            throw exception(SCENE_NOT_EXISTS);
        }
        validateSceneCodeUnique(updateReqVO.getId(), updateReqVO.getSceneCode());
        validateSceneCodeImmutable(scene, updateReqVO.getSceneCode());

        SceneDO updateObj = SceneConvert.INSTANCE.convert(updateReqVO);
        updateObj.setStatus(scene.getStatus());
        sceneMapper.updateById(updateObj);
    }

    @Override
    public void updateSceneStatus(Long id, Integer status) {
        validateSceneExists(id);

        SceneDO updateObj = new SceneDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        sceneMapper.updateById(updateObj);
    }

    @Override
    public void deleteScene(Long id) {
        SceneDO scene = validateSceneExists(id);
        validateSceneDeleteAllowed(scene);
        sceneMapper.deleteById(id);
    }

    @Override
    public SceneDO getScene(Long id) {
        return sceneMapper.selectById(id);
    }

    @Override
    public PageResult<SceneDO> getScenePage(ScenePageReqVO pageReqVO) {
        return sceneMapper.selectPage(pageReqVO);
    }

    @Override
    public List<SceneDO> getSimpleSceneList() {
        return sceneMapper.selectEnabledList();
    }

    @VisibleForTesting
    public SceneDO validateSceneExists(Long id) {
        if (id == null) {
            return null;
        }
        SceneDO scene = sceneMapper.selectById(id);
        if (scene == null) {
            throw exception(SCENE_NOT_EXISTS);
        }
        return scene;
    }

    @VisibleForTesting
    public void validateSceneCodeUnique(Long id, String sceneCode) {
        SceneDO scene = sceneMapper.selectBySceneCode(sceneCode);
        if (scene == null) {
            return;
        }
        if (id == null || !ObjectUtil.equal(scene.getId(), id)) {
            throw exception(SCENE_CODE_DUPLICATE);
        }
    }

    @VisibleForTesting
    public void validateSceneCodeImmutable(SceneDO scene, String sceneCode) {
        if (!ObjectUtil.equal(scene.getSceneCode(), sceneCode)) {
            throw exception(SCENE_CODE_IMMUTABLE);
        }
    }

    @VisibleForTesting
    public void validateSceneDeleteAllowed(SceneDO scene) {
        String dependencyName = findFirstDependency(scene.getSceneCode());
        if (dependencyName != null) {
            throw exception(SCENE_DELETE_DENIED, scene.getSceneName(), dependencyName);
        }
    }

    private String findFirstDependency(String sceneCode) {
        if (sceneMapper.selectEventSchemaCountBySceneCode(sceneCode) > 0) {
            return "事件模型";
        }
        if (sceneMapper.selectListSetCountBySceneCode(sceneCode) > 0) {
            return "名单";
        }
        if (sceneMapper.selectFeatureCountBySceneCode(sceneCode) > 0) {
            return "特征";
        }
        if (sceneMapper.selectRuleCountBySceneCode(sceneCode) > 0) {
            return "规则";
        }
        if (sceneMapper.selectPolicyCountBySceneCode(sceneCode) > 0) {
            return "策略";
        }
        if (sceneMapper.selectSceneReleaseCountBySceneCode(sceneCode) > 0) {
            return "发布记录";
        }
        if (sceneMapper.selectSimulationCaseCountBySceneCode(sceneCode) > 0) {
            return "仿真用例";
        }
        if (sceneMapper.selectAlertRuleCountBySceneCode(sceneCode) > 0) {
            return "告警规则";
        }
        if (sceneMapper.selectAccessSourceCountBySceneCode(sceneCode) > 0) {
            return "接入源";
        }
        return null;
    }

}
