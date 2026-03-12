package cn.liboshuai.pulsix.module.risk.service.release;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleaseCompileReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleasePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleasePublishReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleaseRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleaseRollbackReqVO;

public interface SceneReleaseService {

    PageResult<SceneReleaseRespVO> getSceneReleasePage(SceneReleasePageReqVO pageReqVO);

    SceneReleaseRespVO getSceneRelease(Long id);

    SceneReleaseRespVO compileSceneRelease(SceneReleaseCompileReqVO reqVO);

    SceneReleaseRespVO publishSceneRelease(SceneReleasePublishReqVO reqVO);

    SceneReleaseRespVO rollbackSceneRelease(SceneReleaseRollbackReqVO reqVO);

}
