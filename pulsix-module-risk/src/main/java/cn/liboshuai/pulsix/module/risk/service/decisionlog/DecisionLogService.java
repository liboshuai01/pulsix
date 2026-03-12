package cn.liboshuai.pulsix.module.risk.service.decisionlog;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo.DecisionLogDetailRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo.DecisionLogPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo.DecisionLogRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo.RuleHitLogRespVO;

import java.util.List;

public interface DecisionLogService {

    PageResult<DecisionLogRespVO> getDecisionLogPage(DecisionLogPageReqVO pageReqVO);

    DecisionLogDetailRespVO getDecisionLog(Long id);

    List<RuleHitLogRespVO> getRuleHitLogList(Long decisionId);

}
