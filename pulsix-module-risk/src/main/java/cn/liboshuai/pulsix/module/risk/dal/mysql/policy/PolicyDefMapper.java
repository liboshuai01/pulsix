package cn.liboshuai.pulsix.module.risk.dal.mysql.policy;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyPageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.policy.PolicyDefDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PolicyDefMapper extends BaseMapperX<PolicyDefDO> {

    default PageResult<PolicyDefDO> selectPolicyPage(PolicyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PolicyDefDO>()
                .eqIfPresent(PolicyDefDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(PolicyDefDO::getPolicyCode, reqVO.getPolicyCode())
                .likeIfPresent(PolicyDefDO::getPolicyName, reqVO.getPolicyName())
                .eqIfPresent(PolicyDefDO::getStatus, reqVO.getStatus())
                .orderByDesc(PolicyDefDO::getUpdateTime)
                .orderByDesc(PolicyDefDO::getId));
    }

    default PolicyDefDO selectBySceneAndPolicyCode(String sceneCode, String policyCode) {
        return selectOne(PolicyDefDO::getSceneCode, sceneCode,
                PolicyDefDO::getPolicyCode, policyCode);
    }

}
