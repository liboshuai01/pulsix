package cn.liboshuai.pulsix.module.risk.dal.mysql.replay;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobPageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.replay.ReplayJobDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReplayJobMapper extends BaseMapperX<ReplayJobDO> {

    default PageResult<ReplayJobDO> selectPage(ReplayJobPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ReplayJobDO>()
                .eqIfPresent(ReplayJobDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(ReplayJobDO::getJobCode, reqVO.getJobCode())
                .eqIfPresent(ReplayJobDO::getJobStatus, reqVO.getJobStatus())
                .orderByDesc(ReplayJobDO::getCreateTime)
                .orderByDesc(ReplayJobDO::getId));
    }

}
