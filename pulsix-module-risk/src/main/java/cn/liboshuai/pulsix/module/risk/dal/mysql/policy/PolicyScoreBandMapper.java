package cn.liboshuai.pulsix.module.risk.dal.mysql.policy;

import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.policy.PolicyScoreBandDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PolicyScoreBandMapper extends BaseMapperX<PolicyScoreBandDO> {

    default List<PolicyScoreBandDO> selectListBySceneAndPolicyCode(String sceneCode, String policyCode) {
        return selectList(new LambdaQueryWrapperX<PolicyScoreBandDO>()
                .eq(PolicyScoreBandDO::getSceneCode, sceneCode)
                .eq(PolicyScoreBandDO::getPolicyCode, policyCode)
                .eq(PolicyScoreBandDO::getEnabledFlag, 1)
                .orderByAsc(PolicyScoreBandDO::getBandNo)
                .orderByAsc(PolicyScoreBandDO::getId));
    }

    default void deleteBySceneAndPolicyCode(String sceneCode, String policyCode) {
        delete(new LambdaQueryWrapperX<PolicyScoreBandDO>()
                .eq(PolicyScoreBandDO::getSceneCode, sceneCode)
                .eq(PolicyScoreBandDO::getPolicyCode, policyCode));
    }

}
