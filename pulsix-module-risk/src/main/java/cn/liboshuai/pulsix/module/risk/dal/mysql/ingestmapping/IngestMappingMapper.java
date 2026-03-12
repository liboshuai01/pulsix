package cn.liboshuai.pulsix.module.risk.dal.mysql.ingestmapping;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo.IngestMappingPageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingestmapping.IngestMappingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IngestMappingMapper extends BaseMapperX<IngestMappingDO> {

    default PageResult<IngestMappingDO> selectPage(IngestMappingPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IngestMappingDO>()
                .likeIfPresent(IngestMappingDO::getSourceCode, reqVO.getSourceCode())
                .likeIfPresent(IngestMappingDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(IngestMappingDO::getEventCode, reqVO.getEventCode())
                .likeIfPresent(IngestMappingDO::getTargetFieldCode, reqVO.getTargetFieldCode())
                .eqIfPresent(IngestMappingDO::getTransformType, reqVO.getTransformType())
                .eqIfPresent(IngestMappingDO::getStatus, reqVO.getStatus())
                .orderByAsc(IngestMappingDO::getSortNo)
                .orderByAsc(IngestMappingDO::getId));
    }

    default List<IngestMappingDO> selectEnabledList(String sourceCode, String sceneCode, String eventCode) {
        return selectList(new LambdaQueryWrapperX<IngestMappingDO>()
                .eq(IngestMappingDO::getSourceCode, sourceCode)
                .eq(IngestMappingDO::getSceneCode, sceneCode)
                .eq(IngestMappingDO::getEventCode, eventCode)
                .eq(IngestMappingDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByAsc(IngestMappingDO::getSortNo)
                .orderByAsc(IngestMappingDO::getId));
    }

}
