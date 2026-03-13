package cn.liboshuai.pulsix.module.risk.service.rule;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RulePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RuleRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RuleSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RuleValidateReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RuleValidateRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventfield.EventFieldDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.rule.RuleDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventfield.EventFieldMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.feature.FeatureDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.rule.RuleDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import cn.liboshuai.pulsix.module.risk.service.auditlog.AuditLogService;
import cn.liboshuai.pulsix.module.risk.service.expression.RiskExpressionValidationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.RULE_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.RULE_EXPR_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.RULE_HIT_REASON_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.RULE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_CREATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_DELETE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_UPDATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.BIZ_TYPE_RULE;

@Service
public class RuleServiceImpl implements RuleService {

    private static final Pattern HIT_REASON_PLACEHOLDER_PATTERN = Pattern.compile("\\{([A-Za-z][A-Za-z0-9_]*)}");

    @Resource
    private RuleDefMapper ruleDefMapper;

    @Resource
    private SceneMapper sceneMapper;

    @Resource
    private EventFieldMapper eventFieldMapper;

    @Resource
    private FeatureDefMapper featureDefMapper;

    @Resource
    private RiskExpressionValidationService expressionValidationService;

    @Resource
    private AuditLogService auditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRule(RuleSaveReqVO createReqVO) {
        validateSceneExists(createReqVO.getSceneCode());
        validateRuleCodeUnique(createReqVO.getSceneCode(), createReqVO.getRuleCode(), null);
        doValidateOrThrow(toValidateReq(createReqVO));

        RuleDefDO rule = buildRuleDef(createReqVO, createReqVO.getSceneCode().trim(), createReqVO.getRuleCode().trim());
        rule.setVersion(1);
        ruleDefMapper.insert(rule);
        auditLogService.createAuditLog(rule.getSceneCode(), BIZ_TYPE_RULE, rule.getRuleCode(), ACTION_CREATE,
                null, buildRespVO(ruleDefMapper.selectById(rule.getId())), "新增规则 " + rule.getRuleCode());
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(RuleSaveReqVO updateReqVO) {
        RuleDefDO rule = validateRuleExists(updateReqVO.getId());
        RuleValidateReqVO validateReq = toValidateReq(updateReqVO);
        validateReq.setSceneCode(rule.getSceneCode());
        doValidateOrThrow(validateReq);

        RuleDefDO updateRule = buildRuleDef(updateReqVO, rule.getSceneCode(), rule.getRuleCode());
        updateRule.setId(rule.getId());
        updateRule.setVersion(rule.getVersion() == null ? 1 : rule.getVersion() + 1);
        ruleDefMapper.updateById(updateRule);
        auditLogService.createAuditLog(rule.getSceneCode(), BIZ_TYPE_RULE, rule.getRuleCode(), ACTION_UPDATE,
                buildRespVO(rule), buildRespVO(ruleDefMapper.selectById(rule.getId())), "修改规则 " + rule.getRuleCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        RuleDefDO rule = validateRuleExists(id);
        ruleDefMapper.deleteById(rule.getId());
        auditLogService.createAuditLog(rule.getSceneCode(), BIZ_TYPE_RULE, rule.getRuleCode(), ACTION_DELETE,
                buildRespVO(rule), null, "删除规则 " + rule.getRuleCode());
    }

    @Override
    public RuleRespVO getRule(Long id) {
        return buildRespVO(validateRuleExists(id));
    }

    @Override
    public PageResult<RuleRespVO> getRulePage(RulePageReqVO pageReqVO) {
        PageResult<RuleDefDO> pageResult = ruleDefMapper.selectRulePage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        return new PageResult<>(pageResult.getList().stream().map(this::buildRespVO).toList(), pageResult.getTotal());
    }

    @Override
    public RuleValidateRespVO validateRule(RuleValidateReqVO reqVO) {
        validateSceneExists(reqVO.getSceneCode());
        RuleValidateRespVO respVO = new RuleValidateRespVO();
        List<String> invalidPlaceholders = validateHitReasonTemplate(reqVO.getSceneCode(), reqVO.getHitReasonTemplate());
        respVO.setInvalidPlaceholders(invalidPlaceholders);
        if (CollUtil.isNotEmpty(invalidPlaceholders)) {
            respVO.setValid(false);
            respVO.setMessage(StrUtil.format("命中原因模板包含未知占位符：{}", String.join("、", invalidPlaceholders)));
            return respVO;
        }
        try {
            expressionValidationService.validate(reqVO.getEngineType(), reqVO.getExprContent(), 1);
            respVO.setValid(true);
            respVO.setMessage("规则表达式校验通过");
            return respVO;
        } catch (RuntimeException exception) {
            respVO.setValid(false);
            respVO.setMessage(rootMessage(exception));
            return respVO;
        }
    }

    private void doValidateOrThrow(RuleValidateReqVO reqVO) {
        RuleValidateRespVO respVO = validateRule(reqVO);
        if (Boolean.TRUE.equals(respVO.getValid())) {
            return;
        }
        if (CollUtil.isNotEmpty(respVO.getInvalidPlaceholders())) {
            throw ServiceExceptionUtil.exception(RULE_HIT_REASON_INVALID, String.join("、", respVO.getInvalidPlaceholders()));
        }
        throw ServiceExceptionUtil.exception(RULE_EXPR_INVALID, respVO.getMessage());
    }

    private SceneDO validateSceneExists(String sceneCode) {
        SceneDO scene = sceneMapper.selectOne(SceneDO::getSceneCode, sceneCode);
        if (scene == null) {
            throw ServiceExceptionUtil.exception(SCENE_NOT_EXISTS);
        }
        return scene;
    }

    private RuleDefDO validateRuleExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(RULE_NOT_EXISTS);
        }
        RuleDefDO rule = ruleDefMapper.selectById(id);
        if (rule == null) {
            throw ServiceExceptionUtil.exception(RULE_NOT_EXISTS);
        }
        return rule;
    }

    private void validateRuleCodeUnique(String sceneCode, String ruleCode, Long id) {
        RuleDefDO rule = ruleDefMapper.selectBySceneAndRuleCode(sceneCode, ruleCode);
        if (rule == null) {
            return;
        }
        if (id == null || !ObjectUtil.equal(rule.getId(), id)) {
            throw ServiceExceptionUtil.exception(RULE_CODE_DUPLICATE);
        }
    }

    private RuleDefDO buildRuleDef(RuleSaveReqVO reqVO, String sceneCode, String ruleCode) {
        RuleDefDO rule = new RuleDefDO();
        rule.setSceneCode(sceneCode);
        rule.setRuleCode(ruleCode);
        rule.setRuleName(reqVO.getRuleName().trim());
        rule.setRuleType(reqVO.getRuleType().trim());
        rule.setEngineType(reqVO.getEngineType().trim());
        rule.setExprContent(reqVO.getExprContent().trim());
        rule.setPriority(reqVO.getPriority());
        rule.setHitAction(reqVO.getHitAction().trim());
        rule.setRiskScore(reqVO.getRiskScore());
        rule.setHitReasonTemplate(trimToNull(reqVO.getHitReasonTemplate()));
        rule.setStatus(reqVO.getStatus());
        rule.setDescription(trimToNull(reqVO.getDescription()));
        return rule;
    }

    private RuleRespVO buildRespVO(RuleDefDO rule) {
        RuleRespVO respVO = new RuleRespVO();
        respVO.setId(rule.getId());
        respVO.setSceneCode(rule.getSceneCode());
        respVO.setRuleCode(rule.getRuleCode());
        respVO.setRuleName(rule.getRuleName());
        respVO.setRuleType(rule.getRuleType());
        respVO.setEngineType(rule.getEngineType());
        respVO.setExprContent(rule.getExprContent());
        respVO.setPriority(rule.getPriority());
        respVO.setHitAction(rule.getHitAction());
        respVO.setRiskScore(rule.getRiskScore());
        respVO.setHitReasonTemplate(rule.getHitReasonTemplate());
        respVO.setStatus(rule.getStatus());
        respVO.setVersion(rule.getVersion());
        respVO.setDescription(rule.getDescription());
        respVO.setCreator(rule.getCreator());
        respVO.setCreateTime(rule.getCreateTime());
        respVO.setUpdater(rule.getUpdater());
        respVO.setUpdateTime(rule.getUpdateTime());
        return respVO;
    }

    private RuleValidateReqVO toValidateReq(RuleSaveReqVO reqVO) {
        RuleValidateReqVO validateReq = new RuleValidateReqVO();
        validateReq.setSceneCode(reqVO.getSceneCode());
        validateReq.setEngineType(reqVO.getEngineType());
        validateReq.setExprContent(reqVO.getExprContent());
        validateReq.setHitReasonTemplate(reqVO.getHitReasonTemplate());
        return validateReq;
    }

    private List<String> validateHitReasonTemplate(String sceneCode, String hitReasonTemplate) {
        if (StrUtil.isBlank(hitReasonTemplate)) {
            return Collections.emptyList();
        }
        Set<String> availableCodes = new LinkedHashSet<>();
        eventFieldMapper.selectList(new LambdaQueryWrapperX<EventFieldDO>()
                        .eq(EventFieldDO::getSceneCode, sceneCode))
                .stream().map(EventFieldDO::getFieldCode).filter(StrUtil::isNotBlank).forEach(availableCodes::add);
        featureDefMapper.selectList(new LambdaQueryWrapperX<FeatureDefDO>()
                        .eq(FeatureDefDO::getSceneCode, sceneCode))
                .stream().map(FeatureDefDO::getFeatureCode).filter(StrUtil::isNotBlank).forEach(availableCodes::add);
        return extractPlaceholders(hitReasonTemplate).stream()
                .filter(code -> !availableCodes.contains(code))
                .toList();
    }

    private List<String> extractPlaceholders(String template) {
        Matcher matcher = HIT_REASON_PLACEHOLDER_PATTERN.matcher(template);
        Set<String> placeholders = new LinkedHashSet<>();
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        return placeholders.stream().collect(Collectors.toList());
    }

    private String trimToNull(String text) {
        return StrUtil.emptyToNull(StrUtil.trim(text));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        String message = null;
        while (current != null) {
            if (StrUtil.isNotBlank(current.getMessage())) {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        return StrUtil.blankToDefault(message, "规则表达式校验失败");
    }

}
