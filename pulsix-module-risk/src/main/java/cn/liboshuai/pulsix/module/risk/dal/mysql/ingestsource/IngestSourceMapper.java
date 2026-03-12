package cn.liboshuai.pulsix.module.risk.dal.mysql.ingestsource;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestsource.vo.IngestSourcePageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingestsource.IngestSourceDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IngestSourceMapper extends BaseMapperX<IngestSourceDO> {

    default PageResult<IngestSourceDO> selectPage(IngestSourcePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IngestSourceDO>()
                .likeIfPresent(IngestSourceDO::getSourceCode, reqVO.getSourceCode())
                .likeIfPresent(IngestSourceDO::getSourceName, reqVO.getSourceName())
                .eqIfPresent(IngestSourceDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(IngestSourceDO::getAuthType, reqVO.getAuthType())
                .eqIfPresent(IngestSourceDO::getStatus, reqVO.getStatus())
                .orderByAsc(IngestSourceDO::getSourceCode)
                .orderByAsc(IngestSourceDO::getId));
    }

}
