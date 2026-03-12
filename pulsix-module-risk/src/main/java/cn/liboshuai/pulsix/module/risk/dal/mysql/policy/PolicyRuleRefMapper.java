package cn.liboshuai.pulsix.module.risk.dal.mysql.policy;

import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.policy.PolicyRuleRefDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface PolicyRuleRefMapper extends BaseMapperX<PolicyRuleRefDO> {

    default List<PolicyRuleRefDO> selectListBySceneAndPolicyCode(String sceneCode, String policyCode) {
        return selectList(new LambdaQueryWrapperX<PolicyRuleRefDO>()
                .eq(PolicyRuleRefDO::getSceneCode, sceneCode)
                .eq(PolicyRuleRefDO::getPolicyCode, policyCode)
                .orderByAsc(PolicyRuleRefDO::getOrderNo)
                .orderByAsc(PolicyRuleRefDO::getId));
    }

    default List<PolicyRuleRefDO> selectListBySceneCodesAndPolicyCodes(Collection<String> sceneCodes,
                                                                       Collection<String> policyCodes) {
        return selectList(new LambdaQueryWrapperX<PolicyRuleRefDO>()
                .inIfPresent(PolicyRuleRefDO::getSceneCode, sceneCodes)
                .inIfPresent(PolicyRuleRefDO::getPolicyCode, policyCodes)
                .orderByAsc(PolicyRuleRefDO::getOrderNo)
                .orderByAsc(PolicyRuleRefDO::getId));
    }

    default void deleteBySceneAndPolicyCode(String sceneCode, String policyCode) {
        delete(new LambdaQueryWrapperX<PolicyRuleRefDO>()
                .eq(PolicyRuleRefDO::getSceneCode, sceneCode)
                .eq(PolicyRuleRefDO::getPolicyCode, policyCode));
    }

}
