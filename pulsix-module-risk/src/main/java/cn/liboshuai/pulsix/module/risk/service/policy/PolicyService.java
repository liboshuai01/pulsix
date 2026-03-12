package cn.liboshuai.pulsix.module.risk.service.policy;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyRuleOptionRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicySaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicySortReqVO;

import java.util.List;

public interface PolicyService {

    Long createPolicy(PolicySaveReqVO createReqVO);

    void updatePolicy(PolicySaveReqVO updateReqVO);

    void deletePolicy(Long id);

    PolicyRespVO getPolicy(Long id);

    PageResult<PolicyRespVO> getPolicyPage(PolicyPageReqVO pageReqVO);

    List<PolicyRuleOptionRespVO> getRuleOptions(String sceneCode);

    void sortPolicyRules(PolicySortReqVO reqVO);

}
