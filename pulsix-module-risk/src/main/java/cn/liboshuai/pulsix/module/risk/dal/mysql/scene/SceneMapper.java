package cn.liboshuai.pulsix.module.risk.dal.mysql.scene;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.ScenePageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SceneMapper extends BaseMapperX<SceneDO> {

    default PageResult<SceneDO> selectPage(ScenePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SceneDO>()
                .likeIfPresent(SceneDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(SceneDO::getSceneName, reqVO.getSceneName())
                .eqIfPresent(SceneDO::getStatus, reqVO.getStatus())
                .orderByDesc(SceneDO::getUpdateTime)
                .orderByDesc(SceneDO::getId));
    }

}

