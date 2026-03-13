package cn.liboshuai.pulsix.access.ingest.dal.mysql.ingestmapping;

import cn.liboshuai.pulsix.access.ingest.dal.dataobject.ingestmapping.IngestMappingDO;
import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IngestMappingMapper extends BaseMapperX<IngestMappingDO> {

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
