package cn.liboshuai.pulsix.module.risk.dal.mysql.decisionlog;

import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.decisionlog.RuleHitLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RuleHitLogMapper extends BaseMapperX<RuleHitLogDO> {

    default List<RuleHitLogDO> selectListByDecisionId(Long decisionId) {
        return selectList(new LambdaQueryWrapperX<RuleHitLogDO>()
                .eq(RuleHitLogDO::getDecisionId, decisionId)
                .orderByAsc(RuleHitLogDO::getRuleOrderNo)
                .orderByAsc(RuleHitLogDO::getId));
    }

}
