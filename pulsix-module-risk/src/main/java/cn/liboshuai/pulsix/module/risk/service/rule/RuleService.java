package cn.liboshuai.pulsix.module.risk.service.rule;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RulePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RuleRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RuleSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RuleValidateReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RuleValidateRespVO;

public interface RuleService {

    Long createRule(RuleSaveReqVO createReqVO);

    void updateRule(RuleSaveReqVO updateReqVO);

    void deleteRule(Long id);

    RuleRespVO getRule(Long id);

    PageResult<RuleRespVO> getRulePage(RulePageReqVO pageReqVO);

    RuleValidateRespVO validateRule(RuleValidateReqVO reqVO);

}
