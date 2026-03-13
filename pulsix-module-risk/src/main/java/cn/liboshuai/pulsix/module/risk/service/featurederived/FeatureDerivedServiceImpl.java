package cn.liboshuai.pulsix.module.risk.service.featurederived;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedDependencyOptionRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedValidateReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedValidateRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventfield.EventFieldDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureDerivedConfDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventfield.EventFieldMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.feature.FeatureDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.feature.FeatureDerivedConfMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureTypeEnum;
import cn.liboshuai.pulsix.module.risk.service.auditlog.AuditLogService;
import cn.liboshuai.pulsix.module.risk.service.expression.RiskExpressionValidationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.FEATURE_DERIVED_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.FEATURE_DERIVED_DEPENDENCY_CYCLE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.FEATURE_DERIVED_DEPENDENCY_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.FEATURE_DERIVED_EXPR_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.FEATURE_DERIVED_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_CREATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_DELETE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_UPDATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.BIZ_TYPE_FEATURE;

@Service
public class FeatureDerivedServiceImpl implements FeatureDerivedService {

    @Resource
    private FeatureDefMapper featureDefMapper;

    @Resource
    private FeatureDerivedConfMapper featureDerivedConfMapper;

    @Resource
    private EventFieldMapper eventFieldMapper;

    @Resource
    private SceneMapper sceneMapper;

    @Resource
    private RiskExpressionValidationService expressionValidationService;

    @Resource
    private AuditLogService auditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFeatureDerived(FeatureDerivedSaveReqVO createReqVO) {
        validateSceneExists(createReqVO.getSceneCode());
        validateFeatureCodeUnique(createReqVO.getSceneCode(), createReqVO.getFeatureCode(), null);
        doValidateOrThrow(toValidateReq(createReqVO));

        FeatureDefDO featureDef = new FeatureDefDO();
        featureDef.setSceneCode(createReqVO.getSceneCode().trim());
        featureDef.setFeatureCode(createReqVO.getFeatureCode().trim());
        featureDef.setFeatureName(createReqVO.getFeatureName().trim());
        featureDef.setFeatureType(RiskFeatureTypeEnum.DERIVED.getType());
        featureDef.setEntityType(null);
        featureDef.setEventCode(null);
        featureDef.setValueType(createReqVO.getValueType().trim());
        featureDef.setStatus(createReqVO.getStatus());
        featureDef.setVersion(1);
        featureDef.setDescription(trimToNull(createReqVO.getDescription()));
        featureDefMapper.insert(featureDef);

        FeatureDerivedConfDO conf = buildFeatureDerivedConf(createReqVO, featureDef.getSceneCode(), featureDef.getFeatureCode());
        featureDerivedConfMapper.insert(conf);
        auditLogService.createAuditLog(featureDef.getSceneCode(), BIZ_TYPE_FEATURE, featureDef.getFeatureCode(), ACTION_CREATE,
                null, getFeatureDerived(featureDef.getId()), "新增派生特征 " + featureDef.getFeatureCode());
        return featureDef.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFeatureDerived(FeatureDerivedSaveReqVO updateReqVO) {
        FeatureDefDO featureDef = validateFeatureDerivedExists(updateReqVO.getId());
        FeatureDerivedRespVO beforePayload = getFeatureDerived(featureDef.getId());
        FeatureDerivedConfDO conf = validateFeatureDerivedConfExists(featureDef.getSceneCode(), featureDef.getFeatureCode());
        FeatureDerivedValidateReqVO validateReq = toValidateReq(updateReqVO);
        validateReq.setFeatureCode(featureDef.getFeatureCode());
        doValidateOrThrow(validateReq);

        FeatureDefDO updateFeatureDef = new FeatureDefDO();
        updateFeatureDef.setId(featureDef.getId());
        updateFeatureDef.setSceneCode(featureDef.getSceneCode());
        updateFeatureDef.setFeatureCode(featureDef.getFeatureCode());
        updateFeatureDef.setFeatureName(updateReqVO.getFeatureName().trim());
        updateFeatureDef.setFeatureType(RiskFeatureTypeEnum.DERIVED.getType());
        updateFeatureDef.setValueType(updateReqVO.getValueType().trim());
        updateFeatureDef.setStatus(updateReqVO.getStatus());
        updateFeatureDef.setVersion(featureDef.getVersion() == null ? 1 : featureDef.getVersion() + 1);
        updateFeatureDef.setDescription(trimToNull(updateReqVO.getDescription()));
        featureDefMapper.updateById(updateFeatureDef);

        FeatureDerivedConfDO updateConf = buildFeatureDerivedConf(updateReqVO, featureDef.getSceneCode(), featureDef.getFeatureCode());
        updateConf.setId(conf.getId());
        featureDerivedConfMapper.updateById(updateConf);
        auditLogService.createAuditLog(featureDef.getSceneCode(), BIZ_TYPE_FEATURE, featureDef.getFeatureCode(), ACTION_UPDATE,
                beforePayload, getFeatureDerived(featureDef.getId()), "修改派生特征 " + featureDef.getFeatureCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFeatureDerived(Long id) {
        FeatureDerivedRespVO beforePayload = getFeatureDerived(id);
        FeatureDefDO featureDef = validateFeatureDerivedExists(id);
        FeatureDerivedConfDO conf = featureDerivedConfMapper.selectBySceneAndFeatureCode(featureDef.getSceneCode(), featureDef.getFeatureCode());
        if (conf != null) {
            featureDerivedConfMapper.deleteById(conf.getId());
        }
        featureDefMapper.deleteById(id);
        auditLogService.createAuditLog(featureDef.getSceneCode(), BIZ_TYPE_FEATURE, featureDef.getFeatureCode(), ACTION_DELETE,
                beforePayload, null, "删除派生特征 " + featureDef.getFeatureCode());
    }

    @Override
    public FeatureDerivedRespVO getFeatureDerived(Long id) {
        FeatureDefDO featureDef = validateFeatureDerivedExists(id);
        FeatureDerivedConfDO conf = validateFeatureDerivedConfExists(featureDef.getSceneCode(), featureDef.getFeatureCode());
        return buildRespVO(featureDef, conf);
    }

    @Override
    public PageResult<FeatureDerivedRespVO> getFeatureDerivedPage(FeatureDerivedPageReqVO pageReqVO) {
        PageResult<FeatureDefDO> pageResult = featureDefMapper.selectDerivedFeaturePage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        Set<String> sceneCodes = pageResult.getList().stream().map(FeatureDefDO::getSceneCode).collect(Collectors.toSet());
        Set<String> featureCodes = pageResult.getList().stream().map(FeatureDefDO::getFeatureCode).collect(Collectors.toSet());
        Map<String, FeatureDerivedConfDO> confMap = featureDerivedConfMapper.selectListBySceneCodesAndFeatureCodes(sceneCodes, featureCodes)
                .stream().collect(Collectors.toMap(this::buildPairKey, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<FeatureDerivedRespVO> respList = pageResult.getList().stream()
                .map(item -> buildRespVO(item, confMap.get(buildPairKey(item.getSceneCode(), item.getFeatureCode()))))
                .toList();
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public List<FeatureDerivedDependencyOptionRespVO> getDependencyOptions(String sceneCode, String currentFeatureCode) {
        validateSceneExists(sceneCode);
        List<FeatureDerivedDependencyOptionRespVO> options = new ArrayList<>();
        List<EventFieldDO> eventFields = eventFieldMapper.selectList(new LambdaQueryWrapperX<EventFieldDO>()
                .eq(EventFieldDO::getSceneCode, sceneCode)
                .orderByAsc(EventFieldDO::getSortNo)
                .orderByAsc(EventFieldDO::getId));
        for (EventFieldDO field : eventFields) {
            FeatureDerivedDependencyOptionRespVO option = new FeatureDerivedDependencyOptionRespVO();
            option.setCode(field.getFieldCode());
            option.setName(field.getFieldName());
            option.setDependencyType("FIELD");
            option.setValueType(field.getFieldType());
            option.setHint(StrUtil.format("{} / {}", field.getEventCode(), StrUtil.blankToDefault(field.getFieldPath(), field.getFieldCode())));
            options.add(option);
        }
        List<FeatureDefDO> featureDefs = featureDefMapper.selectList(new LambdaQueryWrapperX<FeatureDefDO>()
                .eq(FeatureDefDO::getSceneCode, sceneCode)
                .neIfPresent(FeatureDefDO::getFeatureCode, trimToNull(currentFeatureCode))
                .orderByAsc(FeatureDefDO::getFeatureType)
                .orderByAsc(FeatureDefDO::getFeatureCode));
        for (FeatureDefDO featureDef : featureDefs) {
            FeatureDerivedDependencyOptionRespVO option = new FeatureDerivedDependencyOptionRespVO();
            option.setCode(featureDef.getFeatureCode());
            option.setName(featureDef.getFeatureName());
            option.setDependencyType(StrUtil.blankToDefault(featureDef.getFeatureType(), "FEATURE"));
            option.setValueType(featureDef.getValueType());
            option.setHint(featureDef.getDescription());
            options.add(option);
        }
        return options;
    }

    @Override
    public FeatureDerivedValidateRespVO validateExpression(FeatureDerivedValidateReqVO reqVO) {
        validateSceneExists(reqVO.getSceneCode());
        FeatureDerivedValidateResult validationResult = validateDerivedDefinition(reqVO.getSceneCode(), trimToNull(reqVO.getFeatureCode()), reqVO.getDependsOnJson());
        FeatureDerivedValidateRespVO respVO = new FeatureDerivedValidateRespVO();
        respVO.setMissingDependencies(validationResult.missingDependencies());
        respVO.setCycleDetected(validationResult.cycleDetected());
        if (CollUtil.isNotEmpty(validationResult.missingDependencies())) {
            respVO.setValid(false);
            respVO.setMessage(StrUtil.format("缺失依赖：{}", String.join("、", validationResult.missingDependencies())));
            return respVO;
        }
        if (validationResult.cycleDetected()) {
            respVO.setValid(false);
            respVO.setMessage(StrUtil.format("存在循环依赖：{}", validationResult.cycleMessage()));
            return respVO;
        }
        try {
            expressionValidationService.validate(reqVO.getEngineType(), reqVO.getExprContent(), reqVO.getSandboxFlag());
            respVO.setValid(true);
            respVO.setMessage("表达式校验通过");
            return respVO;
        } catch (RuntimeException exception) {
            respVO.setValid(false);
            respVO.setMessage(rootMessage(exception));
            return respVO;
        }
    }

    private void doValidateOrThrow(FeatureDerivedValidateReqVO reqVO) {
        FeatureDerivedValidateRespVO respVO = validateExpression(reqVO);
        if (Boolean.TRUE.equals(respVO.getValid())) {
            return;
        }
        if (CollUtil.isNotEmpty(respVO.getMissingDependencies())) {
            throw ServiceExceptionUtil.exception(FEATURE_DERIVED_DEPENDENCY_INVALID, String.join("、", respVO.getMissingDependencies()));
        }
        if (Boolean.TRUE.equals(respVO.getCycleDetected())) {
            throw ServiceExceptionUtil.exception(FEATURE_DERIVED_DEPENDENCY_CYCLE, StrUtil.removePrefix(respVO.getMessage(), "存在循环依赖："));
        }
        throw ServiceExceptionUtil.exception(FEATURE_DERIVED_EXPR_INVALID, respVO.getMessage());
    }

    private SceneDO validateSceneExists(String sceneCode) {
        SceneDO scene = sceneMapper.selectOne(SceneDO::getSceneCode, sceneCode);
        if (scene == null) {
            throw ServiceExceptionUtil.exception(SCENE_NOT_EXISTS);
        }
        return scene;
    }

    private FeatureDefDO validateFeatureDerivedExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(FEATURE_DERIVED_NOT_EXISTS);
        }
        FeatureDefDO featureDef = featureDefMapper.selectById(id);
        if (featureDef == null || !ObjectUtil.equal(featureDef.getFeatureType(), RiskFeatureTypeEnum.DERIVED.getType())) {
            throw ServiceExceptionUtil.exception(FEATURE_DERIVED_NOT_EXISTS);
        }
        return featureDef;
    }

    private FeatureDerivedConfDO validateFeatureDerivedConfExists(String sceneCode, String featureCode) {
        FeatureDerivedConfDO conf = featureDerivedConfMapper.selectBySceneAndFeatureCode(sceneCode, featureCode);
        if (conf == null) {
            throw ServiceExceptionUtil.exception(FEATURE_DERIVED_NOT_EXISTS);
        }
        return conf;
    }

    private void validateFeatureCodeUnique(String sceneCode, String featureCode, Long id) {
        FeatureDefDO featureDef = featureDefMapper.selectOne(FeatureDefDO::getSceneCode, sceneCode,
                FeatureDefDO::getFeatureCode, featureCode);
        if (featureDef == null) {
            return;
        }
        if (id == null || !ObjectUtil.equal(featureDef.getId(), id)) {
            throw ServiceExceptionUtil.exception(FEATURE_DERIVED_CODE_DUPLICATE);
        }
    }

    private FeatureDerivedConfDO buildFeatureDerivedConf(FeatureDerivedSaveReqVO reqVO, String sceneCode, String featureCode) {
        FeatureDerivedConfDO conf = new FeatureDerivedConfDO();
        conf.setSceneCode(sceneCode);
        conf.setFeatureCode(featureCode);
        conf.setEngineType(reqVO.getEngineType().trim());
        conf.setExprContent(reqVO.getExprContent().trim());
        conf.setDependsOnJson(normalizeDependsOn(reqVO.getDependsOnJson()));
        conf.setValueType(reqVO.getValueType().trim());
        conf.setSandboxFlag(resolveSandboxFlag(reqVO.getEngineType(), reqVO.getSandboxFlag()));
        conf.setTimeoutMs(reqVO.getTimeoutMs());
        conf.setExtraJson(reqVO.getExtraJson());
        conf.setStatus(reqVO.getStatus());
        return conf;
    }

    private FeatureDerivedRespVO buildRespVO(FeatureDefDO featureDef, FeatureDerivedConfDO conf) {
        FeatureDerivedRespVO respVO = new FeatureDerivedRespVO();
        respVO.setId(featureDef.getId());
        respVO.setSceneCode(featureDef.getSceneCode());
        respVO.setFeatureCode(featureDef.getFeatureCode());
        respVO.setFeatureName(featureDef.getFeatureName());
        respVO.setFeatureType(featureDef.getFeatureType());
        respVO.setValueType(featureDef.getValueType());
        respVO.setStatus(featureDef.getStatus() != null ? featureDef.getStatus() : conf == null ? null : conf.getStatus());
        respVO.setVersion(featureDef.getVersion());
        respVO.setDescription(featureDef.getDescription());
        respVO.setCreator(featureDef.getCreator());
        respVO.setCreateTime(featureDef.getCreateTime());
        respVO.setUpdater(featureDef.getUpdater());
        respVO.setUpdateTime(featureDef.getUpdateTime());
        if (conf != null) {
            respVO.setEngineType(conf.getEngineType());
            respVO.setExprContent(conf.getExprContent());
            respVO.setDependsOnJson(conf.getDependsOnJson());
            respVO.setSandboxFlag(conf.getSandboxFlag());
            respVO.setTimeoutMs(conf.getTimeoutMs());
            respVO.setExtraJson(conf.getExtraJson());
        }
        return respVO;
    }

    private FeatureDerivedValidateReqVO toValidateReq(FeatureDerivedSaveReqVO reqVO) {
        FeatureDerivedValidateReqVO validateReq = new FeatureDerivedValidateReqVO();
        validateReq.setSceneCode(reqVO.getSceneCode());
        validateReq.setFeatureCode(reqVO.getFeatureCode());
        validateReq.setEngineType(reqVO.getEngineType());
        validateReq.setExprContent(reqVO.getExprContent());
        validateReq.setDependsOnJson(reqVO.getDependsOnJson());
        validateReq.setSandboxFlag(resolveSandboxFlag(reqVO.getEngineType(), reqVO.getSandboxFlag()));
        return validateReq;
    }

    private FeatureDerivedValidateResult validateDerivedDefinition(String sceneCode, String currentFeatureCode, List<String> rawDependsOn) {
        List<String> dependsOn = normalizeDependsOn(rawDependsOn);
        if (StrUtil.isNotBlank(currentFeatureCode) && dependsOn.contains(currentFeatureCode)) {
            return new FeatureDerivedValidateResult(Collections.emptyList(), true, currentFeatureCode);
        }

        Set<String> validDependencies = new LinkedHashSet<>();
        eventFieldMapper.selectList(new LambdaQueryWrapperX<EventFieldDO>()
                        .eq(EventFieldDO::getSceneCode, sceneCode))
                .stream().map(EventFieldDO::getFieldCode).filter(StrUtil::isNotBlank).forEach(validDependencies::add);
        featureDefMapper.selectList(new LambdaQueryWrapperX<FeatureDefDO>()
                        .eq(FeatureDefDO::getSceneCode, sceneCode))
                .stream().map(FeatureDefDO::getFeatureCode).filter(StrUtil::isNotBlank).forEach(validDependencies::add);
        if (StrUtil.isNotBlank(currentFeatureCode)) {
            validDependencies.add(currentFeatureCode);
        }
        List<String> missingDependencies = dependsOn.stream()
                .filter(item -> !validDependencies.contains(item))
                .toList();
        if (CollUtil.isNotEmpty(missingDependencies)) {
            return new FeatureDerivedValidateResult(missingDependencies, false, null);
        }

        List<FeatureDefDO> derivedDefs = featureDefMapper.selectList(new LambdaQueryWrapperX<FeatureDefDO>()
                .eq(FeatureDefDO::getSceneCode, sceneCode)
                .eq(FeatureDefDO::getFeatureType, RiskFeatureTypeEnum.DERIVED.getType()));
        Set<String> derivedCodes = derivedDefs.stream().map(FeatureDefDO::getFeatureCode).collect(Collectors.toCollection(LinkedHashSet::new));
        if (StrUtil.isNotBlank(currentFeatureCode)) {
            derivedCodes.add(currentFeatureCode);
        }
        Map<String, List<String>> dependencyGraph = new LinkedHashMap<>();
        if (CollUtil.isNotEmpty(derivedDefs)) {
            Set<String> featureCodes = derivedDefs.stream().map(FeatureDefDO::getFeatureCode).collect(Collectors.toSet());
            Map<String, FeatureDerivedConfDO> confMap = featureDerivedConfMapper.selectListBySceneCodesAndFeatureCodes(Set.of(sceneCode), featureCodes)
                    .stream().collect(Collectors.toMap(FeatureDerivedConfDO::getFeatureCode, item -> item, (left, right) -> left, LinkedHashMap::new));
            for (FeatureDefDO derivedDef : derivedDefs) {
                List<String> dependencyCodes = normalizeDependsOn(confMap.get(derivedDef.getFeatureCode()) == null
                        ? Collections.emptyList() : confMap.get(derivedDef.getFeatureCode()).getDependsOnJson());
                dependencyGraph.put(derivedDef.getFeatureCode(), dependencyCodes.stream()
                        .filter(derivedCodes::contains)
                        .toList());
            }
        }
        if (StrUtil.isNotBlank(currentFeatureCode)) {
            dependencyGraph.put(currentFeatureCode, dependsOn.stream().filter(derivedCodes::contains).toList());
        }
        String cycleMessage = detectCycle(dependencyGraph);
        return new FeatureDerivedValidateResult(Collections.emptyList(), cycleMessage != null, cycleMessage);
    }

    private String detectCycle(Map<String, List<String>> dependencyGraph) {
        Map<String, Integer> indegree = new LinkedHashMap<>();
        Map<String, Set<String>> reverseGraph = new LinkedHashMap<>();
        dependencyGraph.keySet().forEach(code -> {
            indegree.put(code, 0);
            reverseGraph.put(code, new LinkedHashSet<>());
        });
        dependencyGraph.forEach((code, dependencies) -> {
            for (String dependency : dependencies) {
                if (!indegree.containsKey(dependency)) {
                    continue;
                }
                reverseGraph.get(dependency).add(code);
                indegree.compute(code, (key, value) -> value == null ? 1 : value + 1);
            }
        });
        Deque<String> queue = new ArrayDeque<>();
        indegree.forEach((code, value) -> {
            if (value == 0) {
                queue.add(code);
            }
        });
        int visited = 0;
        while (!queue.isEmpty()) {
            String code = queue.removeFirst();
            visited++;
            for (String next : reverseGraph.getOrDefault(code, Collections.emptySet())) {
                int nextValue = indegree.compute(next, (key, value) -> value == null ? 0 : value - 1);
                if (nextValue == 0) {
                    queue.add(next);
                }
            }
        }
        if (visited == indegree.size()) {
            return null;
        }
        return indegree.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(" -> "));
    }

    private List<String> normalizeDependsOn(List<String> dependsOn) {
        if (CollUtil.isEmpty(dependsOn)) {
            return Collections.emptyList();
        }
        return dependsOn.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private Integer resolveSandboxFlag(String engineType, Integer sandboxFlag) {
        if (Objects.equals(StrUtil.blankToDefault(engineType, "AVIATOR").trim().toUpperCase(), "GROOVY")) {
            return 1;
        }
        return sandboxFlag == null ? 1 : sandboxFlag;
    }

    private String buildPairKey(FeatureDerivedConfDO conf) {
        return buildPairKey(conf.getSceneCode(), conf.getFeatureCode());
    }

    private String buildPairKey(String sceneCode, String featureCode) {
        return sceneCode + "::" + featureCode;
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
        return StrUtil.blankToDefault(message, "表达式校验失败");
    }

    private record FeatureDerivedValidateResult(List<String> missingDependencies,
                                                boolean cycleDetected,
                                                String cycleMessage) {
    }

}
