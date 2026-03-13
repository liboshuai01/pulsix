package cn.liboshuai.pulsix.module.risk.service.scene;

import cn.hutool.core.util.ObjectUtil;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.ScenePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.SceneSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import cn.liboshuai.pulsix.module.risk.service.auditlog.AuditLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_CREATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_UPDATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_UPDATE_STATUS;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.BIZ_TYPE_SCENE;

@Service
public class SceneServiceImpl implements SceneService {

    @Resource
    private SceneMapper sceneMapper;

    @Resource
    private AuditLogService auditLogService;

    @Override
    public Long createScene(SceneSaveReqVO createReqVO) {
        validateSceneCodeUnique(null, createReqVO.getSceneCode());
        SceneDO scene = BeanUtils.toBean(createReqVO, SceneDO.class);
        sceneMapper.insert(scene);
        auditLogService.createAuditLog(scene.getSceneCode(), BIZ_TYPE_SCENE, scene.getSceneCode(), ACTION_CREATE,
                null, sceneMapper.selectById(scene.getId()), "新增场景 " + scene.getSceneCode());
        return scene.getId();
    }

    @Override
    public void updateScene(SceneSaveReqVO updateReqVO) {
        SceneDO scene = validateSceneExists(updateReqVO.getId());
        SceneDO updateObj = BeanUtils.toBean(updateReqVO, SceneDO.class);
        updateObj.setSceneCode(scene.getSceneCode());
        sceneMapper.updateById(updateObj);
        auditLogService.createAuditLog(scene.getSceneCode(), BIZ_TYPE_SCENE, scene.getSceneCode(), ACTION_UPDATE,
                scene, sceneMapper.selectById(scene.getId()), "修改场景 " + scene.getSceneCode());
    }

    @Override
    public void updateSceneStatus(Long id, Integer status) {
        SceneDO scene = validateSceneExists(id);
        if (ObjectUtil.equal(scene.getStatus(), status)) {
            return;
        }
        SceneDO updateObj = new SceneDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        sceneMapper.updateById(updateObj);
        auditLogService.createAuditLog(scene.getSceneCode(), BIZ_TYPE_SCENE, scene.getSceneCode(), ACTION_UPDATE_STATUS,
                scene, sceneMapper.selectById(id), "更新场景状态为 " + status + "：" + scene.getSceneCode());
    }

    @Override
    public SceneDO getScene(Long id) {
        return sceneMapper.selectById(id);
    }

    @Override
    public PageResult<SceneDO> getScenePage(ScenePageReqVO pageReqVO) {
        return sceneMapper.selectPage(pageReqVO);
    }

    private SceneDO validateSceneExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(SCENE_NOT_EXISTS);
        }
        SceneDO scene = sceneMapper.selectById(id);
        if (scene == null) {
            throw ServiceExceptionUtil.exception(SCENE_NOT_EXISTS);
        }
        return scene;
    }

    private void validateSceneCodeUnique(Long id, String sceneCode) {
        SceneDO scene = sceneMapper.selectOne(SceneDO::getSceneCode, sceneCode);
        if (scene == null) {
            return;
        }
        if (id == null || !ObjectUtil.equal(scene.getId(), id)) {
            throw ServiceExceptionUtil.exception(SCENE_CODE_DUPLICATE);
        }
    }

}
