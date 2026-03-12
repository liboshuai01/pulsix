package cn.liboshuai.pulsix.module.risk.dal.mysql.release;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleasePageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.release.SceneReleaseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SceneReleaseMapper extends BaseMapperX<SceneReleaseDO> {

    default PageResult<SceneReleaseDO> selectPage(SceneReleasePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SceneReleaseDO>()
                .eqIfPresent(SceneReleaseDO::getSceneCode, reqVO.getSceneCode())
                .eqIfPresent(SceneReleaseDO::getPublishStatus, reqVO.getPublishStatus())
                .eqIfPresent(SceneReleaseDO::getValidationStatus, reqVO.getValidationStatus())
                .orderByDesc(SceneReleaseDO::getVersionNo)
                .orderByDesc(SceneReleaseDO::getId));
    }

    default Integer selectMaxVersionNo(String sceneCode) {
        List<SceneReleaseDO> list = selectList(new LambdaQueryWrapperX<SceneReleaseDO>()
                .select(SceneReleaseDO::getVersionNo)
                .eq(SceneReleaseDO::getSceneCode, sceneCode)
                .orderByDesc(SceneReleaseDO::getVersionNo)
                .last("limit 1"));
        return list.isEmpty() ? null : list.get(0).getVersionNo();
    }

}
