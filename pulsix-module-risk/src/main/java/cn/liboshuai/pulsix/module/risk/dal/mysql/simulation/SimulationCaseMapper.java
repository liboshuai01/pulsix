package cn.liboshuai.pulsix.module.risk.dal.mysql.simulation;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationCasePageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.simulation.SimulationCaseDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SimulationCaseMapper extends BaseMapperX<SimulationCaseDO> {

    default PageResult<SimulationCaseDO> selectPage(SimulationCasePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SimulationCaseDO>()
                .likeIfPresent(SimulationCaseDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(SimulationCaseDO::getCaseCode, reqVO.getCaseCode())
                .likeIfPresent(SimulationCaseDO::getCaseName, reqVO.getCaseName())
                .eqIfPresent(SimulationCaseDO::getVersionSelectMode, reqVO.getVersionSelectMode())
                .eqIfPresent(SimulationCaseDO::getStatus, reqVO.getStatus())
                .orderByDesc(SimulationCaseDO::getUpdateTime)
                .orderByDesc(SimulationCaseDO::getId));
    }

}
