package cn.liboshuai.pulsix.module.risk.dal.mysql.dashboard;

import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo.DashboardQueryReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.dashboard.RiskMetricSnapshotDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface RiskMetricSnapshotMapper extends BaseMapperX<RiskMetricSnapshotDO> {

    default List<RiskMetricSnapshotDO> selectListByQuery(DashboardQueryReqVO reqVO, Collection<String> metricCodes) {
        return selectList(new LambdaQueryWrapperX<RiskMetricSnapshotDO>()
                .eqIfPresent(RiskMetricSnapshotDO::getSceneCode, reqVO.getSceneCode())
                .eqIfPresent(RiskMetricSnapshotDO::getStatGranularity, reqVO.getStatGranularity())
                .inIfPresent(RiskMetricSnapshotDO::getMetricCode, metricCodes)
                .betweenIfPresent(RiskMetricSnapshotDO::getStatTime, reqVO.getStatTime())
                .orderByAsc(RiskMetricSnapshotDO::getStatTime)
                .orderByAsc(RiskMetricSnapshotDO::getId));
    }

}
