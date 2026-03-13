package cn.liboshuai.pulsix.module.risk.service.policy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.engine.support.TemplateRenderer;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyRuleOptionRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyRuleRefRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicySaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyScoreBandRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyScoreBandSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyScorePreviewReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyScorePreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicySortReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.policy.PolicyDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.policy.PolicyRuleRefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.policy.PolicyScoreBandDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.rule.RuleDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.policy.PolicyDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.policy.PolicyRuleRefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.policy.PolicyScoreBandMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.rule.RuleDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import cn.liboshuai.pulsix.module.risk.enums.policy.RiskPolicyDecisionModeEnum;
import cn.liboshuai.pulsix.module.risk.enums.policy.RiskPolicyScoreCalcModeEnum;
import cn.liboshuai.pulsix.module.risk.service.auditlog.AuditLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.POLICY_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.POLICY_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.POLICY_RULE_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.POLICY_SCORE_BAND_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_CREATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_DELETE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_SORT;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_UPDATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.BIZ_TYPE_POLICY;

@Service
public class PolicyServiceImpl implements PolicyService {

    private static final String DEFAULT_DECISION_MODE = RiskPolicyDecisionModeEnum.FIRST_HIT.getType();
    private static final String DEFAULT_SCORE_CALC_MODE = RiskPolicyScoreCalcModeEnum.NONE.getType();
    private static final int SCORE_BAND_ORDER_STEP = 10;

    @Resource
    private PolicyDefMapper policyDefMapper;

    @Resource
    private PolicyRuleRefMapper policyRuleRefMapper;

    @Resource
    private PolicyScoreBandMapper policyScoreBandMapper;

    @Resource
    private RuleDefMapper ruleDefMapper;

    @Resource
    private SceneMapper sceneMapper;

    @Resource
    private AuditLogService auditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPolicy(PolicySaveReqVO createReqVO) {
        String sceneCode = createReqVO.getSceneCode().trim();
        String policyCode = createReqVO.getPolicyCode().trim();
        String decisionMode = resolveDecisionMode(createReqVO.getDecisionMode());
        validateSceneExists(sceneCode);
        validatePolicyCodeUnique(sceneCode, policyCode, null);
        List<String> ruleCodes = validateAndNormalizeRuleCodes(sceneCode, createReqVO.getRuleCodes());
        List<PolicyScoreBandDO> scoreBands = buildScoreBandDOs(sceneCode, policyCode, createReqVO.getScoreBands(), decisionMode);

        PolicyDefDO policy = buildPolicyDef(createReqVO, sceneCode, policyCode, decisionMode);
        policy.setVersion(1);
        policyDefMapper.insert(policy);
        replacePolicyRuleRefs(sceneCode, policyCode, ruleCodes, decisionMode, Collections.emptyMap());
        replacePolicyScoreBands(sceneCode, policyCode, scoreBands);
        auditLogService.createAuditLog(sceneCode, BIZ_TYPE_POLICY, policyCode, ACTION_CREATE,
                null, getPolicy(policy.getId()), "新增策略 " + policyCode);
        return policy.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePolicy(PolicySaveReqVO updateReqVO) {
        PolicyDefDO policy = validatePolicyExists(updateReqVO.getId());
        PolicyRespVO beforePayload = getPolicy(policy.getId());
        String decisionMode = resolveDecisionMode(updateReqVO.getDecisionMode());
        List<String> ruleCodes = validateAndNormalizeRuleCodes(policy.getSceneCode(), updateReqVO.getRuleCodes());
        List<PolicyScoreBandDO> scoreBands = buildScoreBandDOs(policy.getSceneCode(), policy.getPolicyCode(), updateReqVO.getScoreBands(), decisionMode);
        Map<String, PolicyRuleRefDO> existingRefMap = policyRuleRefMapper.selectListBySceneAndPolicyCode(policy.getSceneCode(), policy.getPolicyCode())
                .stream()
                .collect(Collectors.toMap(PolicyRuleRefDO::getRuleCode, item -> item, (left, right) -> left, LinkedHashMap::new));

        PolicyDefDO updatePolicy = buildPolicyDef(updateReqVO, policy.getSceneCode(), policy.getPolicyCode(), decisionMode);
        updatePolicy.setId(policy.getId());
        updatePolicy.setVersion(policy.getVersion() == null ? 1 : policy.getVersion() + 1);
        policyDefMapper.updateById(updatePolicy);
        replacePolicyRuleRefs(policy.getSceneCode(), policy.getPolicyCode(), ruleCodes, decisionMode, existingRefMap);
        replacePolicyScoreBands(policy.getSceneCode(), policy.getPolicyCode(), scoreBands);
        auditLogService.createAuditLog(policy.getSceneCode(), BIZ_TYPE_POLICY, policy.getPolicyCode(), ACTION_UPDATE,
                beforePayload, getPolicy(policy.getId()), "修改策略 " + policy.getPolicyCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePolicy(Long id) {
        PolicyRespVO beforePayload = getPolicy(id);
        PolicyDefDO policy = validatePolicyExists(id);
        policyRuleRefMapper.deleteBySceneAndPolicyCode(policy.getSceneCode(), policy.getPolicyCode());
        policyScoreBandMapper.deleteBySceneAndPolicyCode(policy.getSceneCode(), policy.getPolicyCode());
        policyDefMapper.deleteById(policy.getId());
        auditLogService.createAuditLog(policy.getSceneCode(), BIZ_TYPE_POLICY, policy.getPolicyCode(), ACTION_DELETE,
                beforePayload, null, "删除策略 " + policy.getPolicyCode());
    }

    @Override
    public PolicyRespVO getPolicy(Long id) {
        PolicyDefDO policy = validatePolicyExists(id);
        return buildRespVO(policy,
                buildRuleRefRespList(policy.getSceneCode(), policy.getPolicyCode()),
                buildScoreBandRespList(policy.getSceneCode(), policy.getPolicyCode()));
    }

    @Override
    public PageResult<PolicyRespVO> getPolicyPage(PolicyPageReqVO pageReqVO) {
        PageResult<PolicyDefDO> pageResult = policyDefMapper.selectPolicyPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        Set<String> sceneCodes = pageResult.getList().stream().map(PolicyDefDO::getSceneCode).collect(Collectors.toSet());
        Set<String> policyCodes = pageResult.getList().stream().map(PolicyDefDO::getPolicyCode).collect(Collectors.toSet());
        Map<String, List<PolicyRuleRefRespVO>> refMap = buildRuleRefRespMap(sceneCodes, policyCodes);
        List<PolicyRespVO> respList = pageResult.getList().stream()
                .map(item -> buildRespVO(item,
                        refMap.getOrDefault(buildPairKey(item.getSceneCode(), item.getPolicyCode()), Collections.emptyList()),
                        Collections.emptyList()))
                .toList();
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public List<PolicyRuleOptionRespVO> getRuleOptions(String sceneCode) {
        validateSceneExists(sceneCode);
        return ruleDefMapper.selectList(new LambdaQueryWrapperX<RuleDefDO>()
                        .eq(RuleDefDO::getSceneCode, sceneCode)
                        .orderByDesc(RuleDefDO::getPriority)
                        .orderByAsc(RuleDefDO::getRuleCode))
                .stream().map(this::buildRuleOptionRespVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortPolicyRules(PolicySortReqVO reqVO) {
        PolicyDefDO policy = validatePolicyExists(reqVO.getId());
        PolicyRespVO beforePayload = getPolicy(policy.getId());
        List<String> ruleCodes = validateAndNormalizeRuleCodes(policy.getSceneCode(), reqVO.getRuleCodes());
        Map<String, PolicyRuleRefDO> existingRefMap = policyRuleRefMapper.selectListBySceneAndPolicyCode(policy.getSceneCode(), policy.getPolicyCode())
                .stream()
                .collect(Collectors.toMap(PolicyRuleRefDO::getRuleCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        replacePolicyRuleRefs(policy.getSceneCode(), policy.getPolicyCode(), ruleCodes, resolveDecisionMode(policy.getDecisionMode()), existingRefMap);

        PolicyDefDO updatePolicy = new PolicyDefDO();
        updatePolicy.setId(policy.getId());
        updatePolicy.setVersion(policy.getVersion() == null ? 1 : policy.getVersion() + 1);
        policyDefMapper.updateById(updatePolicy);
        auditLogService.createAuditLog(policy.getSceneCode(), BIZ_TYPE_POLICY, policy.getPolicyCode(), ACTION_SORT,
                beforePayload, getPolicy(policy.getId()), "调整策略规则顺序 " + policy.getPolicyCode());
    }

    @Override
    public PolicyScorePreviewRespVO previewScoreCard(PolicyScorePreviewReqVO reqVO) {
        String decisionMode = resolveDecisionMode(reqVO.getDecisionMode());
        String defaultAction = StrUtil.trim(reqVO.getDefaultAction());
        List<PolicyScoreBandDO> scoreBands = buildScoreBandDOs(null, null, reqVO.getScoreBands(), decisionMode);
        return buildScorePreviewResp(decisionMode, defaultAction, reqVO.getTotalScore(), scoreBands);
    }

    private SceneDO validateSceneExists(String sceneCode) {
        SceneDO scene = sceneMapper.selectOne(SceneDO::getSceneCode, sceneCode);
        if (scene == null) {
            throw ServiceExceptionUtil.exception(SCENE_NOT_EXISTS);
        }
        return scene;
    }

    private PolicyDefDO validatePolicyExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(POLICY_NOT_EXISTS);
        }
        PolicyDefDO policy = policyDefMapper.selectById(id);
        if (policy == null) {
            throw ServiceExceptionUtil.exception(POLICY_NOT_EXISTS);
        }
        return policy;
    }

    private void validatePolicyCodeUnique(String sceneCode, String policyCode, Long id) {
        PolicyDefDO policy = policyDefMapper.selectBySceneAndPolicyCode(sceneCode, policyCode);
        if (policy == null) {
            return;
        }
        if (id == null || !ObjectUtil.equal(policy.getId(), id)) {
            throw ServiceExceptionUtil.exception(POLICY_CODE_DUPLICATE);
        }
    }

    private List<String> validateAndNormalizeRuleCodes(String sceneCode, List<String> rawRuleCodes) {
        List<String> ruleCodes = normalizeRuleCodes(rawRuleCodes);
        List<RuleDefDO> ruleList = ruleDefMapper.selectList(new LambdaQueryWrapperX<RuleDefDO>()
                .eq(RuleDefDO::getSceneCode, sceneCode)
                .inIfPresent(RuleDefDO::getRuleCode, ruleCodes));
        Set<String> existingCodes = ruleList.stream().map(RuleDefDO::getRuleCode).collect(Collectors.toSet());
        List<String> invalidCodes = ruleCodes.stream().filter(code -> !existingCodes.contains(code)).toList();
        if (CollUtil.isNotEmpty(invalidCodes)) {
            throw ServiceExceptionUtil.exception(POLICY_RULE_INVALID, String.join("、", invalidCodes));
        }
        return ruleCodes;
    }

    private List<PolicyScoreBandDO> buildScoreBandDOs(String sceneCode,
                                                      String policyCode,
                                                      List<PolicyScoreBandSaveReqVO> rawScoreBands,
                                                      String decisionMode) {
        if (!isScoreCard(decisionMode)) {
            return Collections.emptyList();
        }
        if (CollUtil.isEmpty(rawScoreBands)) {
            throw ServiceExceptionUtil.exception(POLICY_SCORE_BAND_INVALID, "SCORE_CARD 至少需要配置一个评分段");
        }
        List<PolicyScoreBandDO> result = new ArrayList<>();
        int bandNo = SCORE_BAND_ORDER_STEP;
        int rowNo = 1;
        for (PolicyScoreBandSaveReqVO rawScoreBand : rawScoreBands) {
            if (rawScoreBand == null) {
                rowNo++;
                continue;
            }
            if (rawScoreBand.getMinScore() == null || rawScoreBand.getMaxScore() == null) {
                throw ServiceExceptionUtil.exception(POLICY_SCORE_BAND_INVALID, "第 " + rowNo + " 个评分段缺少分值范围");
            }
            if (rawScoreBand.getMinScore() > rawScoreBand.getMaxScore()) {
                throw ServiceExceptionUtil.exception(POLICY_SCORE_BAND_INVALID,
                        "第 " + rowNo + " 个评分段最小分值不能大于最大分值");
            }
            PolicyScoreBandDO scoreBand = new PolicyScoreBandDO();
            scoreBand.setSceneCode(sceneCode);
            scoreBand.setPolicyCode(policyCode);
            scoreBand.setBandNo(bandNo);
            scoreBand.setMinScore(rawScoreBand.getMinScore());
            scoreBand.setMaxScore(rawScoreBand.getMaxScore());
            scoreBand.setHitAction(StrUtil.trim(rawScoreBand.getHitAction()));
            scoreBand.setHitReasonTemplate(trimToNull(rawScoreBand.getHitReasonTemplate()));
            scoreBand.setEnabledFlag(1);
            result.add(scoreBand);
            bandNo += SCORE_BAND_ORDER_STEP;
            rowNo++;
        }
        if (CollUtil.isEmpty(result)) {
            throw ServiceExceptionUtil.exception(POLICY_SCORE_BAND_INVALID, "SCORE_CARD 至少需要配置一个评分段");
        }
        validateScoreBandOverlap(result);
        return result;
    }

    private void validateScoreBandOverlap(List<PolicyScoreBandDO> scoreBands) {
        List<PolicyScoreBandDO> sortedBands = scoreBands.stream()
                .sorted(Comparator.comparing(PolicyScoreBandDO::getMinScore)
                        .thenComparing(PolicyScoreBandDO::getMaxScore)
                        .thenComparing(PolicyScoreBandDO::getBandNo))
                .toList();
        PolicyScoreBandDO previous = null;
        for (PolicyScoreBandDO current : sortedBands) {
            if (previous != null && current.getMinScore() <= previous.getMaxScore()) {
                throw ServiceExceptionUtil.exception(POLICY_SCORE_BAND_INVALID,
                        String.format("评分段区间存在重叠：[%d,%d] 与 [%d,%d]",
                                previous.getMinScore(), previous.getMaxScore(),
                                current.getMinScore(), current.getMaxScore()));
            }
            previous = current;
        }
    }

    private void replacePolicyRuleRefs(String sceneCode,
                                       String policyCode,
                                       List<String> ruleCodes,
                                       String decisionMode,
                                       Map<String, PolicyRuleRefDO> existingRefMap) {
        policyRuleRefMapper.deleteBySceneAndPolicyCode(sceneCode, policyCode);
        int orderNo = SCORE_BAND_ORDER_STEP;
        for (String ruleCode : ruleCodes) {
            PolicyRuleRefDO existingRef = existingRefMap == null ? null : existingRefMap.get(ruleCode);
            PolicyRuleRefDO ref = new PolicyRuleRefDO();
            ref.setSceneCode(sceneCode);
            ref.setPolicyCode(policyCode);
            ref.setRuleCode(ruleCode);
            ref.setOrderNo(orderNo);
            ref.setEnabledFlag(existingRef == null ? 1 : ObjectUtil.defaultIfNull(existingRef.getEnabledFlag(), 1));
            ref.setBranchExpr(existingRef == null ? null : trimToNull(existingRef.getBranchExpr()));
            ref.setScoreWeight(existingRef == null ? null : existingRef.getScoreWeight());
            ref.setStopOnHit(resolveStopOnHit(existingRef, decisionMode));
            policyRuleRefMapper.insert(ref);
            orderNo += SCORE_BAND_ORDER_STEP;
        }
    }

    private Integer resolveStopOnHit(PolicyRuleRefDO existingRef, String decisionMode) {
        if (existingRef != null && existingRef.getStopOnHit() != null) {
            return existingRef.getStopOnHit();
        }
        return isScoreCard(decisionMode) ? 0 : 1;
    }

    private void replacePolicyScoreBands(String sceneCode, String policyCode, List<PolicyScoreBandDO> scoreBands) {
        policyScoreBandMapper.deleteBySceneAndPolicyCode(sceneCode, policyCode);
        for (PolicyScoreBandDO scoreBand : scoreBands) {
            PolicyScoreBandDO insertBand = new PolicyScoreBandDO();
            insertBand.setSceneCode(sceneCode);
            insertBand.setPolicyCode(policyCode);
            insertBand.setBandNo(scoreBand.getBandNo());
            insertBand.setMinScore(scoreBand.getMinScore());
            insertBand.setMaxScore(scoreBand.getMaxScore());
            insertBand.setHitAction(scoreBand.getHitAction());
            insertBand.setHitReasonTemplate(scoreBand.getHitReasonTemplate());
            insertBand.setEnabledFlag(1);
            policyScoreBandMapper.insert(insertBand);
        }
    }

    private PolicyDefDO buildPolicyDef(PolicySaveReqVO reqVO, String sceneCode, String policyCode, String decisionMode) {
        PolicyDefDO policy = new PolicyDefDO();
        policy.setSceneCode(sceneCode);
        policy.setPolicyCode(policyCode);
        policy.setPolicyName(reqVO.getPolicyName().trim());
        policy.setDecisionMode(decisionMode);
        policy.setDefaultAction(reqVO.getDefaultAction().trim());
        policy.setScoreCalcMode(resolveScoreCalcMode(decisionMode));
        policy.setStatus(reqVO.getStatus());
        policy.setDescription(trimToNull(reqVO.getDescription()));
        return policy;
    }

    private PolicyRespVO buildRespVO(PolicyDefDO policy,
                                     List<PolicyRuleRefRespVO> ruleRefs,
                                     List<PolicyScoreBandRespVO> scoreBands) {
        PolicyRespVO respVO = new PolicyRespVO();
        respVO.setId(policy.getId());
        respVO.setSceneCode(policy.getSceneCode());
        respVO.setPolicyCode(policy.getPolicyCode());
        respVO.setPolicyName(policy.getPolicyName());
        respVO.setDecisionMode(policy.getDecisionMode());
        respVO.setDefaultAction(policy.getDefaultAction());
        respVO.setScoreCalcMode(policy.getScoreCalcMode());
        respVO.setStatus(policy.getStatus());
        respVO.setVersion(policy.getVersion());
        respVO.setDescription(policy.getDescription());
        respVO.setRuleRefs(ruleRefs);
        respVO.setRuleCodes(ruleRefs.stream().map(PolicyRuleRefRespVO::getRuleCode).toList());
        respVO.setScoreBands(scoreBands);
        respVO.setCreator(policy.getCreator());
        respVO.setCreateTime(policy.getCreateTime());
        respVO.setUpdater(policy.getUpdater());
        respVO.setUpdateTime(policy.getUpdateTime());
        return respVO;
    }

    private List<PolicyRuleRefRespVO> buildRuleRefRespList(String sceneCode, String policyCode) {
        List<PolicyRuleRefDO> refList = policyRuleRefMapper.selectListBySceneAndPolicyCode(sceneCode, policyCode);
        if (CollUtil.isEmpty(refList)) {
            return Collections.emptyList();
        }
        Map<String, RuleDefDO> ruleMap = ruleDefMapper.selectList(new LambdaQueryWrapperX<RuleDefDO>()
                        .eq(RuleDefDO::getSceneCode, sceneCode)
                        .inIfPresent(RuleDefDO::getRuleCode, refList.stream().map(PolicyRuleRefDO::getRuleCode).toList()))
                .stream().collect(Collectors.toMap(RuleDefDO::getRuleCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        return refList.stream().map(item -> buildRuleRefRespVO(item, ruleMap.get(item.getRuleCode()))).toList();
    }

    private Map<String, List<PolicyRuleRefRespVO>> buildRuleRefRespMap(Set<String> sceneCodes, Set<String> policyCodes) {
        List<PolicyRuleRefDO> refList = policyRuleRefMapper.selectListBySceneCodesAndPolicyCodes(sceneCodes, policyCodes);
        if (CollUtil.isEmpty(refList)) {
            return Collections.emptyMap();
        }
        Map<String, List<PolicyRuleRefDO>> groupMap = refList.stream()
                .collect(Collectors.groupingBy(item -> buildPairKey(item.getSceneCode(), item.getPolicyCode()), LinkedHashMap::new, Collectors.toList()));
        Set<String> pairSceneCodes = refList.stream().map(PolicyRuleRefDO::getSceneCode).collect(Collectors.toSet());
        Set<String> ruleCodes = refList.stream().map(PolicyRuleRefDO::getRuleCode).collect(Collectors.toSet());
        Map<String, RuleDefDO> ruleMap = ruleDefMapper.selectList(new LambdaQueryWrapperX<RuleDefDO>()
                        .inIfPresent(RuleDefDO::getSceneCode, pairSceneCodes)
                        .inIfPresent(RuleDefDO::getRuleCode, ruleCodes))
                .stream().collect(Collectors.toMap(item -> buildPairKey(item.getSceneCode(), item.getRuleCode()), item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<PolicyRuleRefRespVO>> respMap = new LinkedHashMap<>();
        groupMap.forEach((key, refs) -> {
            List<PolicyRuleRefRespVO> items = refs.stream()
                    .map(item -> buildRuleRefRespVO(item, ruleMap.get(buildPairKey(item.getSceneCode(), item.getRuleCode()))))
                    .toList();
            respMap.put(key, items);
        });
        return respMap;
    }

    private List<PolicyScoreBandRespVO> buildScoreBandRespList(String sceneCode, String policyCode) {
        return policyScoreBandMapper.selectListBySceneAndPolicyCode(sceneCode, policyCode)
                .stream().map(this::buildScoreBandRespVO).toList();
    }

    private PolicyRuleRefRespVO buildRuleRefRespVO(PolicyRuleRefDO ref, RuleDefDO rule) {
        PolicyRuleRefRespVO respVO = new PolicyRuleRefRespVO();
        respVO.setRuleCode(ref.getRuleCode());
        respVO.setOrderNo(ref.getOrderNo());
        respVO.setScoreWeight(ref.getScoreWeight());
        respVO.setStopOnHit(ref.getStopOnHit());
        if (rule != null) {
            respVO.setRuleName(rule.getRuleName());
            respVO.setHitAction(rule.getHitAction());
            respVO.setPriority(rule.getPriority());
            respVO.setRiskScore(rule.getRiskScore());
            respVO.setStatus(rule.getStatus());
        }
        return respVO;
    }

    private PolicyRuleOptionRespVO buildRuleOptionRespVO(RuleDefDO rule) {
        PolicyRuleOptionRespVO respVO = new PolicyRuleOptionRespVO();
        respVO.setRuleCode(rule.getRuleCode());
        respVO.setRuleName(rule.getRuleName());
        respVO.setHitAction(rule.getHitAction());
        respVO.setPriority(rule.getPriority());
        respVO.setRiskScore(rule.getRiskScore());
        respVO.setStatus(rule.getStatus());
        return respVO;
    }

    private PolicyScoreBandRespVO buildScoreBandRespVO(PolicyScoreBandDO scoreBand) {
        PolicyScoreBandRespVO respVO = new PolicyScoreBandRespVO();
        respVO.setBandNo(scoreBand.getBandNo());
        respVO.setMinScore(scoreBand.getMinScore());
        respVO.setMaxScore(scoreBand.getMaxScore());
        respVO.setHitAction(scoreBand.getHitAction());
        respVO.setHitReasonTemplate(scoreBand.getHitReasonTemplate());
        return respVO;
    }

    private PolicyScorePreviewRespVO buildScorePreviewResp(String decisionMode,
                                                           String defaultAction,
                                                           Integer totalScore,
                                                           List<PolicyScoreBandDO> scoreBands) {
        PolicyScorePreviewRespVO respVO = new PolicyScorePreviewRespVO();
        respVO.setDecisionMode(decisionMode);
        respVO.setTotalScore(totalScore);
        respVO.setDefaultAction(defaultAction);
        respVO.setMatched(Boolean.FALSE);
        respVO.setFinalAction(defaultAction);
        if (!isScoreCard(decisionMode)) {
            respVO.setReason("当前策略不是 SCORE_CARD，预览返回默认动作");
            return respVO;
        }
        PolicyScoreBandDO matchedBand = scoreBands.stream()
                .sorted(Comparator.comparing(PolicyScoreBandDO::getBandNo, Comparator.nullsLast(Integer::compareTo)))
                .filter(item -> item.getMinScore() != null && item.getMaxScore() != null
                        && totalScore >= item.getMinScore() && totalScore <= item.getMaxScore())
                .findFirst()
                .orElse(null);
        if (matchedBand == null) {
            respVO.setReason("未命中任何评分段，返回默认动作");
            return respVO;
        }
        respVO.setMatched(Boolean.TRUE);
        respVO.setMatchedBandNo(matchedBand.getBandNo());
        respVO.setMatchedMinScore(matchedBand.getMinScore());
        respVO.setMatchedMaxScore(matchedBand.getMaxScore());
        respVO.setFinalAction(matchedBand.getHitAction());
        respVO.setReason(renderScoreBandReason(matchedBand, totalScore));
        return respVO;
    }

    private String renderScoreBandReason(PolicyScoreBandDO matchedBand, Integer totalScore) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("totalScore", totalScore);
        values.put("matchedBandCode", matchedBand.getBandNo() == null ? null : "BAND_" + matchedBand.getBandNo());
        values.put("matchedBandAction", matchedBand.getHitAction());
        return trimToNull(TemplateRenderer.render(matchedBand.getHitReasonTemplate(), values));
    }

    private String resolveDecisionMode(String decisionMode) {
        return StrUtil.blankToDefault(StrUtil.trim(decisionMode), DEFAULT_DECISION_MODE);
    }

    private String resolveScoreCalcMode(String decisionMode) {
        return isScoreCard(decisionMode)
                ? RiskPolicyScoreCalcModeEnum.SUM_HIT_SCORE.getType()
                : DEFAULT_SCORE_CALC_MODE;
    }

    private boolean isScoreCard(String decisionMode) {
        return StrUtil.equalsIgnoreCase(RiskPolicyDecisionModeEnum.SCORE_CARD.getType(), decisionMode);
    }

    private List<String> normalizeRuleCodes(List<String> ruleCodes) {
        if (CollUtil.isEmpty(ruleCodes)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(ruleCodes.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private String buildPairKey(String sceneCode, String code) {
        return sceneCode + "::" + code;
    }

    private String trimToNull(String text) {
        return StrUtil.emptyToNull(StrUtil.trim(text));
    }

}
