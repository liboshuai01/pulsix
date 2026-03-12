package cn.liboshuai.pulsix.module.risk.service.policy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyRuleOptionRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicyRuleRefRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicySaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.policy.vo.PolicySortReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.policy.PolicyDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.policy.PolicyRuleRefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.rule.RuleDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.policy.PolicyDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.policy.PolicyRuleRefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.rule.RuleDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import cn.liboshuai.pulsix.module.risk.enums.policy.RiskPolicyDecisionModeEnum;
import cn.liboshuai.pulsix.module.risk.enums.policy.RiskPolicyScoreCalcModeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.POLICY_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.POLICY_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.POLICY_RULE_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_NOT_EXISTS;

@Service
public class PolicyServiceImpl implements PolicyService {

    private static final String DEFAULT_DECISION_MODE = RiskPolicyDecisionModeEnum.FIRST_HIT.getType();
    private static final String DEFAULT_SCORE_CALC_MODE = RiskPolicyScoreCalcModeEnum.NONE.getType();

    @Resource
    private PolicyDefMapper policyDefMapper;

    @Resource
    private PolicyRuleRefMapper policyRuleRefMapper;

    @Resource
    private RuleDefMapper ruleDefMapper;

    @Resource
    private SceneMapper sceneMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPolicy(PolicySaveReqVO createReqVO) {
        validateSceneExists(createReqVO.getSceneCode());
        validatePolicyCodeUnique(createReqVO.getSceneCode(), createReqVO.getPolicyCode(), null);
        List<String> ruleCodes = validateAndNormalizeRuleCodes(createReqVO.getSceneCode(), createReqVO.getRuleCodes());

        PolicyDefDO policy = buildPolicyDef(createReqVO, createReqVO.getSceneCode().trim(), createReqVO.getPolicyCode().trim());
        policy.setVersion(1);
        policyDefMapper.insert(policy);
        replacePolicyRuleRefs(policy.getSceneCode(), policy.getPolicyCode(), ruleCodes);
        return policy.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePolicy(PolicySaveReqVO updateReqVO) {
        PolicyDefDO policy = validatePolicyExists(updateReqVO.getId());
        List<String> ruleCodes = validateAndNormalizeRuleCodes(policy.getSceneCode(), updateReqVO.getRuleCodes());

        PolicyDefDO updatePolicy = buildPolicyDef(updateReqVO, policy.getSceneCode(), policy.getPolicyCode());
        updatePolicy.setId(policy.getId());
        updatePolicy.setVersion(policy.getVersion() == null ? 1 : policy.getVersion() + 1);
        policyDefMapper.updateById(updatePolicy);
        replacePolicyRuleRefs(policy.getSceneCode(), policy.getPolicyCode(), ruleCodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePolicy(Long id) {
        PolicyDefDO policy = validatePolicyExists(id);
        policyRuleRefMapper.deleteBySceneAndPolicyCode(policy.getSceneCode(), policy.getPolicyCode());
        policyDefMapper.deleteById(policy.getId());
    }

    @Override
    public PolicyRespVO getPolicy(Long id) {
        PolicyDefDO policy = validatePolicyExists(id);
        return buildRespVO(policy, buildRuleRefRespList(policy.getSceneCode(), policy.getPolicyCode()));
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
                .map(item -> buildRespVO(item, refMap.getOrDefault(buildPairKey(item.getSceneCode(), item.getPolicyCode()), Collections.emptyList())))
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
        List<String> ruleCodes = validateAndNormalizeRuleCodes(policy.getSceneCode(), reqVO.getRuleCodes());
        replacePolicyRuleRefs(policy.getSceneCode(), policy.getPolicyCode(), ruleCodes);

        PolicyDefDO updatePolicy = new PolicyDefDO();
        updatePolicy.setId(policy.getId());
        updatePolicy.setVersion(policy.getVersion() == null ? 1 : policy.getVersion() + 1);
        policyDefMapper.updateById(updatePolicy);
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

    private void replacePolicyRuleRefs(String sceneCode, String policyCode, List<String> ruleCodes) {
        policyRuleRefMapper.deleteBySceneAndPolicyCode(sceneCode, policyCode);
        int orderNo = 10;
        for (String ruleCode : ruleCodes) {
            PolicyRuleRefDO ref = new PolicyRuleRefDO();
            ref.setSceneCode(sceneCode);
            ref.setPolicyCode(policyCode);
            ref.setRuleCode(ruleCode);
            ref.setOrderNo(orderNo);
            ref.setEnabledFlag(1);
            ref.setBranchExpr(null);
            ref.setScoreWeight(null);
            ref.setStopOnHit(1);
            policyRuleRefMapper.insert(ref);
            orderNo += 10;
        }
    }

    private PolicyDefDO buildPolicyDef(PolicySaveReqVO reqVO, String sceneCode, String policyCode) {
        PolicyDefDO policy = new PolicyDefDO();
        policy.setSceneCode(sceneCode);
        policy.setPolicyCode(policyCode);
        policy.setPolicyName(reqVO.getPolicyName().trim());
        policy.setDecisionMode(DEFAULT_DECISION_MODE);
        policy.setDefaultAction(reqVO.getDefaultAction().trim());
        policy.setScoreCalcMode(DEFAULT_SCORE_CALC_MODE);
        policy.setStatus(reqVO.getStatus());
        policy.setDescription(trimToNull(reqVO.getDescription()));
        return policy;
    }

    private PolicyRespVO buildRespVO(PolicyDefDO policy, List<PolicyRuleRefRespVO> ruleRefs) {
        PolicyRespVO respVO = new PolicyRespVO();
        respVO.setId(policy.getId());
        respVO.setSceneCode(policy.getSceneCode());
        respVO.setPolicyCode(policy.getPolicyCode());
        respVO.setPolicyName(policy.getPolicyName());
        respVO.setDecisionMode(policy.getDecisionMode());
        respVO.setDefaultAction(policy.getDefaultAction());
        respVO.setStatus(policy.getStatus());
        respVO.setVersion(policy.getVersion());
        respVO.setDescription(policy.getDescription());
        respVO.setRuleRefs(ruleRefs);
        respVO.setRuleCodes(ruleRefs.stream().map(PolicyRuleRefRespVO::getRuleCode).toList());
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

    private PolicyRuleRefRespVO buildRuleRefRespVO(PolicyRuleRefDO ref, RuleDefDO rule) {
        PolicyRuleRefRespVO respVO = new PolicyRuleRefRespVO();
        respVO.setRuleCode(ref.getRuleCode());
        respVO.setOrderNo(ref.getOrderNo());
        if (rule != null) {
            respVO.setRuleName(rule.getRuleName());
            respVO.setHitAction(rule.getHitAction());
            respVO.setPriority(rule.getPriority());
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
        respVO.setStatus(rule.getStatus());
        return respVO;
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
