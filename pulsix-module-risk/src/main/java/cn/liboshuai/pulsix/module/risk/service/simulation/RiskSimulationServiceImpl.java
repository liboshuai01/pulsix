package cn.liboshuai.pulsix.module.risk.service.simulation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.engine.core.DecisionExecutor;
import cn.liboshuai.pulsix.engine.feature.InMemoryLookupService;
import cn.liboshuai.pulsix.engine.feature.InMemoryStreamFeatureStateStore;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.runtime.RuntimeCompiler;
import cn.liboshuai.pulsix.engine.script.DefaultScriptCompiler;
import cn.liboshuai.pulsix.engine.simulation.LocalSimulationRunner;
import cn.liboshuai.pulsix.engine.snapshot.SceneSnapshotEnvelopes;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.json.JsonUtils;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationCasePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationCaseRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationCaseSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationExecuteReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationReportRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.release.SceneReleaseDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.simulation.SimulationCaseDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.simulation.SimulationReportDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.release.SceneReleaseMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.simulation.SimulationCaseMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.simulation.SimulationReportMapper;
import cn.liboshuai.pulsix.module.risk.service.auditlog.AuditLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SIMULATION_CASE_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SIMULATION_CASE_CONFIG_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SIMULATION_CASE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SIMULATION_RELEASE_NOT_AVAILABLE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SIMULATION_REPORT_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_CREATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_DELETE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_EXECUTE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_UPDATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.BIZ_TYPE_SIMULATION;

@Service
public class RiskSimulationServiceImpl implements RiskSimulationService {

    private static final String VERSION_SELECT_MODE_LATEST = "LATEST";
    private static final String VERSION_SELECT_MODE_FIXED = "FIXED";
    private static final Set<String> LATEST_AVAILABLE_PUBLISH_STATUSES = Set.of("PUBLISHED", "ACTIVE");

    private final LocalSimulationRunner simulationRunner = new LocalSimulationRunner(
            new RuntimeCompiler(new DefaultScriptCompiler()),
            InMemoryStreamFeatureStateStore::new,
            InMemoryLookupService::new,
            new DecisionExecutor());

    private final SimulationReportKernelViewBuilder simulationReportKernelViewBuilder = new SimulationReportKernelViewBuilder();

    @Resource
    private SimulationCaseMapper simulationCaseMapper;

    @Resource
    private SimulationReportMapper simulationReportMapper;

    @Resource
    private SceneReleaseMapper sceneReleaseMapper;

    @Resource
    private SceneMapper sceneMapper;

    @Resource
    private AuditLogService auditLogService;

    @Override
    public Long createSimulationCase(SimulationCaseSaveReqVO createReqVO) {
        SimulationCaseSaveReqVO normalizedReqVO = normalizeSaveReqVO(createReqVO);
        validateSimulationCaseCodeUnique(normalizedReqVO.getSceneCode(), normalizedReqVO.getCaseCode(), null);
        SimulationCaseDO simulationCase = BeanUtils.toBean(normalizedReqVO, SimulationCaseDO.class);
        simulationCaseMapper.insert(simulationCase);
        auditLogService.createAuditLog(simulationCase.getSceneCode(), BIZ_TYPE_SIMULATION, simulationCase.getCaseCode(), ACTION_CREATE,
                null, getSimulationCase(simulationCase.getId()), "新增仿真用例 " + simulationCase.getCaseCode());
        return simulationCase.getId();
    }

    @Override
    public void updateSimulationCase(SimulationCaseSaveReqVO updateReqVO) {
        SimulationCaseDO simulationCase = validateSimulationCaseExists(updateReqVO.getId());
        SimulationCaseRespVO beforePayload = getSimulationCase(simulationCase.getId());
        SimulationCaseSaveReqVO normalizedReqVO = normalizeSaveReqVO(updateReqVO);
        validateSimulationCaseCodeUnique(normalizedReqVO.getSceneCode(), normalizedReqVO.getCaseCode(), simulationCase.getId());
        SimulationCaseDO updateObj = BeanUtils.toBean(normalizedReqVO, SimulationCaseDO.class);
        simulationCaseMapper.updateById(updateObj);
        auditLogService.createAuditLog(simulationCase.getSceneCode(), BIZ_TYPE_SIMULATION, simulationCase.getCaseCode(), ACTION_UPDATE,
                beforePayload, getSimulationCase(simulationCase.getId()), "修改仿真用例 " + simulationCase.getCaseCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSimulationCase(Long id) {
        SimulationCaseRespVO beforePayload = getSimulationCase(id);
        SimulationCaseDO simulationCase = validateSimulationCaseExists(id);
        simulationCaseMapper.deleteById(id);
        simulationReportMapper.delete(SimulationReportDO::getCaseId, id);
        auditLogService.createAuditLog(simulationCase.getSceneCode(), BIZ_TYPE_SIMULATION, simulationCase.getCaseCode(), ACTION_DELETE,
                beforePayload, null, "删除仿真用例 " + simulationCase.getCaseCode());
    }

    @Override
    public SimulationCaseRespVO getSimulationCase(Long id) {
        SimulationCaseDO simulationCase = validateSimulationCaseExists(id);
        Map<Long, SimulationReportDO> latestReportMap = buildLatestReportMap(List.of(simulationCase.getId()));
        return buildSimulationCaseRespVO(simulationCase, latestReportMap.get(simulationCase.getId()));
    }

    @Override
    public PageResult<SimulationCaseRespVO> getSimulationCasePage(SimulationCasePageReqVO pageReqVO) {
        PageResult<SimulationCaseDO> pageResult = simulationCaseMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        Map<Long, SimulationReportDO> latestReportMap = buildLatestReportMap(
                pageResult.getList().stream().map(SimulationCaseDO::getId).toList());
        List<SimulationCaseRespVO> list = pageResult.getList().stream()
                .map(item -> buildSimulationCaseRespVO(item, latestReportMap.get(item.getId())))
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SimulationReportRespVO executeSimulation(SimulationExecuteReqVO reqVO) {
        SimulationCaseDO simulationCase = validateSimulationCaseExists(reqVO.getCaseId());
        SimulationCaseRespVO beforePayload = getSimulationCase(simulationCase.getId());
        SceneReleaseDO release = resolveSceneRelease(simulationCase);
        SceneSnapshot snapshot = EngineJson.read(EngineJson.write(release.getSnapshotJson()), SceneSnapshot.class);
        RiskEvent riskEvent = buildRiskEvent(simulationCase, snapshot);
        LocalSimulationRunner.SimulationOverrides overrides = buildOverrides(simulationCase);

        long startNanos = System.nanoTime();
        LocalSimulationRunner.SimulationReport report = simulationRunner.simulate(
                SceneSnapshotEnvelopes.fromSnapshot(snapshot), List.of(riskEvent), overrides);
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        SimulationReportDO reportDO = new SimulationReportDO();
        reportDO.setCaseId(simulationCase.getId());
        reportDO.setSceneCode(simulationCase.getSceneCode());
        reportDO.setVersionNo(ObjectUtil.defaultIfNull(report.getUsedVersion(), release.getVersionNo()));
        reportDO.setTraceId(firstNonBlank(report.getFinalResult() == null ? null : report.getFinalResult().getTraceId(),
                riskEvent.getTraceId()));
        reportDO.setResultJson(JsonUtils.parseObject(EngineJson.write(report), new TypeReference<Map<String, Object>>() {
        }));
        reportDO.setPassFlag(matchExpectation(simulationCase, report) ? 1 : 0);
        reportDO.setDurationMs(durationMs);
        simulationReportMapper.insert(reportDO);
        SimulationReportRespVO respVO = buildSimulationReportRespVO(reportDO, simulationCase);
        auditLogService.createAuditLog(simulationCase.getSceneCode(), BIZ_TYPE_SIMULATION, simulationCase.getCaseCode(), ACTION_EXECUTE,
                buildSimulationExecuteAuditPayload(beforePayload, null),
                buildSimulationExecuteAuditPayload(getSimulationCase(simulationCase.getId()), respVO),
                "执行仿真用例 " + simulationCase.getCaseCode());
        return respVO;
    }

    @Override
    public SimulationReportRespVO getSimulationReport(Long id) {
        SimulationReportDO report = validateSimulationReportExists(id);
        SimulationCaseDO simulationCase = simulationCaseMapper.selectById(report.getCaseId());
        return buildSimulationReportRespVO(report, simulationCase);
    }

    private SimulationCaseSaveReqVO normalizeSaveReqVO(SimulationCaseSaveReqVO reqVO) {
        SimulationCaseSaveReqVO normalized = BeanUtils.toBean(reqVO, SimulationCaseSaveReqVO.class);
        normalized.setSceneCode(trimToNull(normalized.getSceneCode()));
        normalized.setCaseCode(trimToNull(normalized.getCaseCode()));
        normalized.setCaseName(trimToNull(normalized.getCaseName()));
        normalized.setVersionSelectMode(normalizeVersionSelectMode(normalized.getVersionSelectMode()));
        normalized.setExpectedAction(normalizeExpectedAction(normalized.getExpectedAction()));
        normalized.setExpectedHitRules(normalizeExpectedHitRules(normalized.getExpectedHitRules()));
        normalized.setDescription(trimToNull(normalized.getDescription()));
        normalized.setInputEventJson(copyObjectMap(normalized.getInputEventJson()));
        normalized.setMockFeatureJson(copyObjectMap(normalized.getMockFeatureJson()));
        normalized.setMockLookupJson(copyObjectMap(normalized.getMockLookupJson()));
        validateSimulationCaseConfig(normalized);
        if (!VERSION_SELECT_MODE_FIXED.equals(normalized.getVersionSelectMode())) {
            normalized.setVersionNo(null);
        }
        return normalized;
    }

    private void validateSimulationCaseConfig(SimulationCaseSaveReqVO reqVO) {
        validateSceneExists(reqVO.getSceneCode());
        if (CollUtil.isEmpty(reqVO.getInputEventJson())) {
            throw ServiceExceptionUtil.exception(SIMULATION_CASE_CONFIG_INVALID, "标准事件输入不能为空");
        }
        if (!VERSION_SELECT_MODE_LATEST.equals(reqVO.getVersionSelectMode())
                && !VERSION_SELECT_MODE_FIXED.equals(reqVO.getVersionSelectMode())) {
            throw ServiceExceptionUtil.exception(SIMULATION_CASE_CONFIG_INVALID, "版本选择模式仅支持 LATEST / FIXED");
        }
        if (VERSION_SELECT_MODE_FIXED.equals(reqVO.getVersionSelectMode())
                && (reqVO.getVersionNo() == null || reqVO.getVersionNo() <= 0)) {
            throw ServiceExceptionUtil.exception(SIMULATION_CASE_CONFIG_INVALID, "固定版本模式下必须填写大于 0 的版本号");
        }
    }

    private void validateSimulationCaseCodeUnique(String sceneCode, String caseCode, Long id) {
        SimulationCaseDO simulationCase = simulationCaseMapper.selectOne(SimulationCaseDO::getSceneCode, sceneCode,
                SimulationCaseDO::getCaseCode, caseCode);
        if (simulationCase == null) {
            return;
        }
        if (id == null || !id.equals(simulationCase.getId())) {
            throw ServiceExceptionUtil.exception(SIMULATION_CASE_CODE_DUPLICATE);
        }
    }

    private SimulationCaseDO validateSimulationCaseExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(SIMULATION_CASE_NOT_EXISTS);
        }
        SimulationCaseDO simulationCase = simulationCaseMapper.selectById(id);
        if (simulationCase == null) {
            throw ServiceExceptionUtil.exception(SIMULATION_CASE_NOT_EXISTS);
        }
        return simulationCase;
    }

    private SimulationReportDO validateSimulationReportExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(SIMULATION_REPORT_NOT_EXISTS);
        }
        SimulationReportDO report = simulationReportMapper.selectById(id);
        if (report == null) {
            throw ServiceExceptionUtil.exception(SIMULATION_REPORT_NOT_EXISTS);
        }
        return report;
    }

    private SceneReleaseDO resolveSceneRelease(SimulationCaseDO simulationCase) {
        if (VERSION_SELECT_MODE_FIXED.equalsIgnoreCase(simulationCase.getVersionSelectMode())) {
            SceneReleaseDO release = sceneReleaseMapper.selectOne(SceneReleaseDO::getSceneCode, simulationCase.getSceneCode(),
                    SceneReleaseDO::getVersionNo, simulationCase.getVersionNo());
            if (release == null || !Objects.equals(release.getValidationStatus(), "PASSED")) {
                throw ServiceExceptionUtil.exception(SIMULATION_RELEASE_NOT_AVAILABLE);
            }
            return release;
        }
        List<SceneReleaseDO> releases = sceneReleaseMapper.selectList(new LambdaQueryWrapperX<SceneReleaseDO>()
                .eq(SceneReleaseDO::getSceneCode, simulationCase.getSceneCode())
                .eq(SceneReleaseDO::getValidationStatus, "PASSED")
                .in(SceneReleaseDO::getPublishStatus, LATEST_AVAILABLE_PUBLISH_STATUSES)
                .orderByDesc(SceneReleaseDO::getVersionNo)
                .orderByDesc(SceneReleaseDO::getId));
        if (CollUtil.isEmpty(releases)) {
            throw ServiceExceptionUtil.exception(SIMULATION_RELEASE_NOT_AVAILABLE);
        }
        return releases.get(0);
    }

    private void validateSceneExists(String sceneCode) {
        SceneDO scene = sceneMapper.selectOne(SceneDO::getSceneCode, sceneCode);
        if (scene == null) {
            throw ServiceExceptionUtil.exception(SCENE_NOT_EXISTS);
        }
    }

    private RiskEvent buildRiskEvent(SimulationCaseDO simulationCase, SceneSnapshot snapshot) {
        Map<String, Object> payload = copyObjectMap(simulationCase.getInputEventJson());
        payload.putIfAbsent("sceneCode", simulationCase.getSceneCode());
        payload.putIfAbsent("traceId", "SIM-" + simulationCase.getCaseCode() + '-' + System.currentTimeMillis());
        payload.putIfAbsent("eventId", simulationCase.getCaseCode() + "-EVENT");
        payload.putIfAbsent("eventTime", Instant.now().toString());
        if (snapshot != null && snapshot.getEventSchema() != null && StrUtil.isNotBlank(snapshot.getEventSchema().getEventType())) {
            payload.putIfAbsent("eventType", snapshot.getEventSchema().getEventType());
        }
        return EngineJson.read(EngineJson.write(payload), RiskEvent.class);
    }

    private LocalSimulationRunner.SimulationOverrides buildOverrides(SimulationCaseDO simulationCase) {
        LocalSimulationRunner.SimulationOverrides overrides = LocalSimulationRunner.SimulationOverrides.empty();
        overrides.setStreamFeatures(copyObjectMap(simulationCase.getMockFeatureJson()));
        overrides.setLookupFeatures(copyObjectMap(simulationCase.getMockLookupJson()));
        return overrides;
    }

    private Map<String, Object> buildSimulationExecuteAuditPayload(SimulationCaseRespVO caseRespVO, SimulationReportRespVO reportRespVO) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("case", caseRespVO);
        if (reportRespVO != null) {
            payload.put("report", reportRespVO);
        }
        return payload;
    }

    private boolean matchExpectation(SimulationCaseDO simulationCase, LocalSimulationRunner.SimulationReport report) {
        LocalSimulationRunner.SimulationEventResult finalResult = report == null ? null : report.getFinalResult();
        String actualAction = finalResult == null || finalResult.getFinalAction() == null
                ? null : finalResult.getFinalAction().name();
        List<String> actualHitRules = finalResult == null ? Collections.emptyList()
                : defaultList(finalResult.getHitRules()).stream()
                .map(LocalSimulationRunner.MatchedRule::getRuleCode)
                .filter(StrUtil::isNotBlank)
                .toList();

        boolean actionMatched = StrUtil.isBlank(simulationCase.getExpectedAction())
                || Objects.equals(simulationCase.getExpectedAction(), actualAction);
        boolean hitRulesMatched = CollUtil.isEmpty(simulationCase.getExpectedHitRules())
                || Objects.equals(simulationCase.getExpectedHitRules(), actualHitRules);
        return actionMatched && hitRulesMatched;
    }

    private Map<Long, SimulationReportDO> buildLatestReportMap(Collection<Long> caseIds) {
        if (CollUtil.isEmpty(caseIds)) {
            return Collections.emptyMap();
        }
        List<SimulationReportDO> reports = simulationReportMapper.selectList(new LambdaQueryWrapperX<SimulationReportDO>()
                .in(SimulationReportDO::getCaseId, caseIds)
                .orderByDesc(SimulationReportDO::getCreateTime)
                .orderByDesc(SimulationReportDO::getId));
        Map<Long, SimulationReportDO> latestReportMap = new LinkedHashMap<>();
        for (SimulationReportDO report : defaultList(reports)) {
            latestReportMap.putIfAbsent(report.getCaseId(), report);
        }
        return latestReportMap;
    }

    private SimulationCaseRespVO buildSimulationCaseRespVO(SimulationCaseDO simulationCase, SimulationReportDO latestReport) {
        SimulationCaseRespVO respVO = BeanUtils.toBean(simulationCase, SimulationCaseRespVO.class);
        if (latestReport == null) {
            return respVO;
        }
        respVO.setLatestReportId(latestReport.getId());
        respVO.setLatestReportVersionNo(latestReport.getVersionNo());
        respVO.setLatestFinalAction(extractFinalAction(latestReport.getResultJson()));
        respVO.setLatestHitRules(extractHitRules(latestReport.getResultJson()));
        respVO.setLatestPassFlag(latestReport.getPassFlag());
        respVO.setLatestDurationMs(latestReport.getDurationMs());
        respVO.setLatestReportTime(latestReport.getCreateTime());
        return respVO;
    }

    private SimulationReportRespVO buildSimulationReportRespVO(SimulationReportDO report, SimulationCaseDO simulationCase) {
        SimulationReportRespVO respVO = BeanUtils.toBean(report, SimulationReportRespVO.class);
        if (simulationCase != null) {
            respVO.setCaseCode(simulationCase.getCaseCode());
            respVO.setCaseName(simulationCase.getCaseName());
        }
        simulationReportKernelViewBuilder.apply(respVO);
        return respVO;
    }

    private String extractFinalAction(Map<String, Object> resultJson) {
        Object value = extractFinalResultField(resultJson, "finalAction");
        if (value == null) {
            value = resultJson == null ? null : resultJson.get("finalAction");
        }
        return value == null ? null : String.valueOf(value);
    }

    private List<String> extractHitRules(Map<String, Object> resultJson) {
        Object value = extractFinalResultField(resultJson, "hitRules");
        if (value == null && resultJson != null) {
            value = resultJson.get("hitRules");
        }
        if (!(value instanceof List<?> items)) {
            return Collections.emptyList();
        }
        List<String> hitRules = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?> map) {
                Object ruleCode = map.get("ruleCode");
                if (ruleCode != null) {
                    hitRules.add(String.valueOf(ruleCode));
                }
                continue;
            }
            if (item != null) {
                hitRules.add(String.valueOf(item));
            }
        }
        return hitRules;
    }

    private Object extractFinalResultField(Map<String, Object> resultJson, String fieldName) {
        if (resultJson == null) {
            return null;
        }
        Object finalResult = resultJson.get("finalResult");
        if (finalResult instanceof Map<?, ?> map) {
            return map.get(fieldName);
        }
        return null;
    }

    private String normalizeVersionSelectMode(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String normalizeExpectedAction(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private List<String> normalizeExpectedHitRules(List<String> expectedHitRules) {
        if (CollUtil.isEmpty(expectedHitRules)) {
            return Collections.emptyList();
        }
        return expectedHitRules.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private String trimToNull(String text) {
        return StrUtil.emptyToNull(StrUtil.trim(text));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private Map<String, Object> copyObjectMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private <T> List<T> defaultList(Collection<T> list) {
        return list == null ? Collections.emptyList() : new ArrayList<>(list);
    }

    private LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

}
