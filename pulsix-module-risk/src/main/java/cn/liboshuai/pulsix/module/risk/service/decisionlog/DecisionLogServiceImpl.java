package cn.liboshuai.pulsix.module.risk.service.decisionlog;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo.DecisionLogDetailRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo.DecisionLogPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo.DecisionLogRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.decisionlog.vo.RuleHitLogRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.decisionlog.DecisionLogDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.decisionlog.RuleHitLogDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.decisionlog.DecisionLogMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.decisionlog.RuleHitLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.DECISION_LOG_NOT_EXISTS;

@Service
public class DecisionLogServiceImpl implements DecisionLogService {

    @Resource
    private DecisionLogMapper decisionLogMapper;

    @Resource
    private RuleHitLogMapper ruleHitLogMapper;

    @Override
    public PageResult<DecisionLogRespVO> getDecisionLogPage(DecisionLogPageReqVO pageReqVO) {
        PageResult<DecisionLogDO> pageResult = decisionLogMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        List<DecisionLogRespVO> list = pageResult.getList().stream()
                .map(this::buildDecisionLogRespVO)
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public DecisionLogDetailRespVO getDecisionLog(Long id) {
        DecisionLogDO decisionLog = validateDecisionLogExists(id);
        DecisionLogDetailRespVO respVO = BeanUtils.toBean(decisionLog, DecisionLogDetailRespVO.class);
        respVO.setHitRuleCodes(extractHitRuleCodes(decisionLog.getHitRulesJson()));
        return respVO;
    }

    @Override
    public List<RuleHitLogRespVO> getRuleHitLogList(Long decisionId) {
        validateDecisionLogExists(decisionId);
        return ruleHitLogMapper.selectListByDecisionId(decisionId).stream()
                .map(item -> BeanUtils.toBean(item, RuleHitLogRespVO.class))
                .toList();
    }

    private DecisionLogRespVO buildDecisionLogRespVO(DecisionLogDO decisionLog) {
        DecisionLogRespVO respVO = BeanUtils.toBean(decisionLog, DecisionLogRespVO.class);
        respVO.setHitRuleCodes(extractHitRuleCodes(decisionLog.getHitRulesJson()));
        return respVO;
    }

    private List<String> extractHitRuleCodes(List<Map<String, Object>> hitRulesJson) {
        if (CollUtil.isEmpty(hitRulesJson)) {
            return Collections.emptyList();
        }
        List<String> hitRuleCodes = new ArrayList<>();
        for (Map<String, Object> item : hitRulesJson) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            Object ruleCode = item.get("ruleCode");
            if (ruleCode == null) {
                ruleCode = item.get("code");
            }
            if (ruleCode == null) {
                continue;
            }
            String value = StrUtil.trim(String.valueOf(ruleCode));
            if (StrUtil.isNotBlank(value)) {
                hitRuleCodes.add(value);
            }
        }
        return hitRuleCodes;
    }

    private DecisionLogDO validateDecisionLogExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(DECISION_LOG_NOT_EXISTS);
        }
        DecisionLogDO decisionLog = decisionLogMapper.selectById(id);
        if (decisionLog == null) {
            throw ServiceExceptionUtil.exception(DECISION_LOG_NOT_EXISTS);
        }
        return decisionLog;
    }

}
