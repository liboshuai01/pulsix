package cn.liboshuai.pulsix.module.risk.service.replay;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.RiskEvent;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.simulation.LocalReplayRunner;
import cn.liboshuai.pulsix.engine.simulation.LocalSimulationRunner;
import cn.liboshuai.pulsix.engine.snapshot.SceneSnapshotEnvelopes;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobCreateReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobDetailRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobExecuteReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.decisionlog.DecisionLogDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.release.SceneReleaseDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.replay.ReplayJobDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.decisionlog.DecisionLogMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.release.SceneReleaseMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.replay.ReplayJobMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.REPLAY_JOB_CONFIG_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.REPLAY_JOB_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_RELEASE_NOT_EXISTS;

@Service
public class ReplayJobServiceImpl implements ReplayJobService {

    private static final String JOB_STATUS_INIT = "INIT";
    private static final String JOB_STATUS_RUNNING = "RUNNING";
    private static final String JOB_STATUS_SUCCESS = "SUCCESS";
    private static final String JOB_STATUS_FAILED = "FAILED";

    private static final String INPUT_SOURCE_DECISION_LOG_EXPORT = "DECISION_LOG_EXPORT";
    private static final String DEFAULT_DECISION_LOG_INPUT_REF = "LATEST_SCENE_DECISION_LOGS";
    private static final int DEFAULT_DECISION_LOG_LIMIT = 20;
    private static final int MAX_SAMPLE_DIFF_COUNT = 10;

    private final LocalReplayRunner replayRunner = new LocalReplayRunner();

    @Resource
    private ReplayJobMapper replayJobMapper;

    @Resource
    private SceneReleaseMapper sceneReleaseMapper;

    @Resource
    private DecisionLogMapper decisionLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReplayJob(ReplayJobCreateReqVO createReqVO) {
        ReplayJobCreateReqVO normalizedReqVO = normalizeCreateReqVO(createReqVO);
        validateReplayConfig(normalizedReqVO);

        ReplayJobDO replayJob = BeanUtils.toBean(normalizedReqVO, ReplayJobDO.class);
        replayJob.setJobCode(generateJobCode(normalizedReqVO));
        replayJob.setJobStatus(JOB_STATUS_INIT);
        replayJob.setEventTotalCount(0);
        replayJob.setDiffEventCount(0);
        replayJobMapper.insert(replayJob);
        return replayJob.getId();
    }

    @Override
    public PageResult<ReplayJobRespVO> getReplayJobPage(ReplayJobPageReqVO pageReqVO) {
        PageResult<ReplayJobDO> pageResult = replayJobMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        List<ReplayJobRespVO> list = pageResult.getList().stream()
                .map(item -> BeanUtils.toBean(item, ReplayJobRespVO.class))
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public ReplayJobDetailRespVO getReplayJob(Long id) {
        return BeanUtils.toBean(validateReplayJobExists(id), ReplayJobDetailRespVO.class);
    }

    @Override
    public ReplayJobDetailRespVO executeReplayJob(ReplayJobExecuteReqVO reqVO) {
        ReplayJobDO replayJob = validateReplayJobExists(reqVO.getId());
        SceneReleaseDO baselineRelease = validateSceneReleaseExists(replayJob.getSceneCode(), replayJob.getBaselineVersionNo());
        SceneReleaseDO targetRelease = validateSceneReleaseExists(replayJob.getSceneCode(), replayJob.getTargetVersionNo());
        List<RiskEvent> events = loadReplayEvents(replayJob);

        LocalDateTime startedAt = LocalDateTime.now();
        updateJobStatus(replayJob.getId(), JOB_STATUS_RUNNING, startedAt, null, 0, 0, Collections.emptyMap(), Collections.emptyList());

        try {
            LocalReplayRunner.ReplayDiffReport diffReport = replayRunner.diff(
                    SceneSnapshotEnvelopes.fromSnapshot(buildReleaseSnapshot(baselineRelease)),
                    SceneSnapshotEnvelopes.fromSnapshot(buildReleaseSnapshot(targetRelease)),
                    events);
            updateJobStatus(replayJob.getId(), JOB_STATUS_SUCCESS, startedAt, LocalDateTime.now(),
                    ObjectUtil.defaultIfNull(diffReport.getEventCount(), 0),
                    ObjectUtil.defaultIfNull(diffReport.getChangedEventCount(), 0),
                    buildSummaryJson(diffReport),
                    buildSampleDiffJson(diffReport));
            return getReplayJob(replayJob.getId());
        } catch (Exception exception) {
            updateJobStatus(replayJob.getId(), JOB_STATUS_FAILED, startedAt, LocalDateTime.now(), 0, 0, Collections.emptyMap(), Collections.emptyList());
            throw exception;
        }
    }

    private void updateJobStatus(Long id, String jobStatus, LocalDateTime startedAt, LocalDateTime finishedAt,
                                 Integer eventTotalCount, Integer diffEventCount,
                                 Map<String, Object> summaryJson,
                                 List<Map<String, Object>> sampleDiffJson) {
        ReplayJobDO updateObj = new ReplayJobDO();
        updateObj.setId(id);
        updateObj.setJobStatus(jobStatus);
        updateObj.setStartedAt(startedAt);
        updateObj.setFinishedAt(finishedAt);
        if (eventTotalCount != null) {
            updateObj.setEventTotalCount(eventTotalCount);
        }
        if (diffEventCount != null) {
            updateObj.setDiffEventCount(diffEventCount);
        }
        if (summaryJson != null) {
            updateObj.setSummaryJson(summaryJson);
        }
        if (sampleDiffJson != null) {
            updateObj.setSampleDiffJson(sampleDiffJson);
        }
        replayJobMapper.updateById(updateObj);
    }

    private ReplayJobCreateReqVO normalizeCreateReqVO(ReplayJobCreateReqVO reqVO) {
        ReplayJobCreateReqVO normalized = BeanUtils.toBean(reqVO, ReplayJobCreateReqVO.class);
        normalized.setSceneCode(trimToNull(normalized.getSceneCode()));
        normalized.setInputSourceType(normalizeUpperCode(normalized.getInputSourceType()));
        normalized.setInputRef(resolveInputRef(normalized.getInputSourceType(), normalized.getInputRef()));
        normalized.setRemark(trimToNull(normalized.getRemark()));
        return normalized;
    }

    private void validateReplayConfig(ReplayJobCreateReqVO reqVO) {
        if (reqVO.getBaselineVersionNo() == null || reqVO.getBaselineVersionNo() <= 0) {
            throw ServiceExceptionUtil.exception(REPLAY_JOB_CONFIG_INVALID, "基线版本号必须大于 0");
        }
        if (reqVO.getTargetVersionNo() == null || reqVO.getTargetVersionNo() <= 0) {
            throw ServiceExceptionUtil.exception(REPLAY_JOB_CONFIG_INVALID, "目标版本号必须大于 0");
        }
        if (Objects.equals(reqVO.getBaselineVersionNo(), reqVO.getTargetVersionNo())) {
            throw ServiceExceptionUtil.exception(REPLAY_JOB_CONFIG_INVALID, "基线版本号与目标版本号不能相同");
        }
        if (!Objects.equals(reqVO.getInputSourceType(), INPUT_SOURCE_DECISION_LOG_EXPORT)) {
            throw ServiceExceptionUtil.exception(REPLAY_JOB_CONFIG_INVALID, "当前仅支持 DECISION_LOG_EXPORT 输入源");
        }
        validateSceneReleaseExists(reqVO.getSceneCode(), reqVO.getBaselineVersionNo());
        validateSceneReleaseExists(reqVO.getSceneCode(), reqVO.getTargetVersionNo());
    }

    private ReplayJobDO validateReplayJobExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(REPLAY_JOB_NOT_EXISTS);
        }
        ReplayJobDO replayJob = replayJobMapper.selectById(id);
        if (replayJob == null) {
            throw ServiceExceptionUtil.exception(REPLAY_JOB_NOT_EXISTS);
        }
        return replayJob;
    }

    private SceneReleaseDO validateSceneReleaseExists(String sceneCode, Integer versionNo) {
        SceneReleaseDO release = sceneReleaseMapper.selectOne(SceneReleaseDO::getSceneCode, sceneCode,
                SceneReleaseDO::getVersionNo, versionNo);
        if (release == null) {
            throw ServiceExceptionUtil.exception(SCENE_RELEASE_NOT_EXISTS);
        }
        if (!Objects.equals(release.getValidationStatus(), "PASSED")) {
            throw ServiceExceptionUtil.exception(REPLAY_JOB_CONFIG_INVALID, "回放版本必须通过预检");
        }
        return release;
    }

    private List<RiskEvent> loadReplayEvents(ReplayJobDO replayJob) {
        if (!Objects.equals(replayJob.getInputSourceType(), INPUT_SOURCE_DECISION_LOG_EXPORT)) {
            throw ServiceExceptionUtil.exception(REPLAY_JOB_CONFIG_INVALID, "当前仅支持 DECISION_LOG_EXPORT 输入源");
        }
        List<DecisionLogDO> decisionLogs = DEFAULT_DECISION_LOG_INPUT_REF.equalsIgnoreCase(replayJob.getInputRef())
                ? loadLatestDecisionLogs(replayJob.getSceneCode())
                : loadDecisionLogsByIds(replayJob.getInputRef());
        if (CollUtil.isEmpty(decisionLogs)) {
            throw ServiceExceptionUtil.exception(REPLAY_JOB_CONFIG_INVALID, "未找到可用于回放的决策日志输入");
        }
        return decisionLogs.stream()
                .map(this::buildRiskEvent)
                .sorted(Comparator.comparing(RiskEvent::getEventTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(item -> StrUtil.blankToDefault(item.getEventId(), ""))
                        .thenComparing(item -> StrUtil.blankToDefault(item.getTraceId(), "")))
                .toList();
    }

    private List<DecisionLogDO> loadLatestDecisionLogs(String sceneCode) {
        return decisionLogMapper.selectList(new LambdaQueryWrapperX<DecisionLogDO>()
                .eq(DecisionLogDO::getSceneCode, sceneCode)
                .orderByDesc(DecisionLogDO::getEventTime)
                .orderByDesc(DecisionLogDO::getId)
                .last("limit " + DEFAULT_DECISION_LOG_LIMIT));
    }

    private List<DecisionLogDO> loadDecisionLogsByIds(String inputRef) {
        List<Long> ids = parseDecisionLogIds(inputRef);
        if (CollUtil.isEmpty(ids)) {
            throw ServiceExceptionUtil.exception(REPLAY_JOB_CONFIG_INVALID, "决策日志编号列表不能为空");
        }
        List<DecisionLogDO> decisionLogs = decisionLogMapper.selectBatchIds(ids);
        if (decisionLogs.size() != ids.size()) {
            Set<Long> foundIds = decisionLogs.stream().map(DecisionLogDO::getId).collect(Collectors.toSet());
            List<Long> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();
            throw ServiceExceptionUtil.exception(REPLAY_JOB_CONFIG_INVALID, "决策日志不存在：" + missingIds);
        }
        Map<Long, DecisionLogDO> decisionLogMap = decisionLogs.stream()
                .collect(Collectors.toMap(DecisionLogDO::getId, item -> item));
        List<DecisionLogDO> orderedLogs = new ArrayList<>(ids.size());
        for (Long id : ids) {
            DecisionLogDO decisionLog = decisionLogMap.get(id);
            if (decisionLog != null) {
                orderedLogs.add(decisionLog);
            }
        }
        return orderedLogs;
    }

    private List<Long> parseDecisionLogIds(String inputRef) {
        String text = trimToNull(inputRef);
        if (text == null) {
            return Collections.emptyList();
        }
        String[] segments = text.split("[,，\\s]+");
        List<Long> ids = new ArrayList<>();
        for (String segment : segments) {
            String value = trimToNull(segment);
            if (value == null) {
                continue;
            }
            try {
                ids.add(Long.valueOf(value));
            } catch (NumberFormatException exception) {
                throw ServiceExceptionUtil.exception(REPLAY_JOB_CONFIG_INVALID, "决策日志编号格式不正确：" + value);
            }
        }
        return ids.stream()
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private RiskEvent buildRiskEvent(DecisionLogDO decisionLog) {
        Map<String, Object> payload = decisionLog.getInputJson() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(decisionLog.getInputJson());
        payload.putIfAbsent("sceneCode", decisionLog.getSceneCode());
        payload.putIfAbsent("traceId", decisionLog.getTraceId());
        payload.putIfAbsent("eventId", decisionLog.getEventId());
        payload.putIfAbsent("eventTime", decisionLog.getEventTime());
        return EngineJson.read(EngineJson.write(payload), RiskEvent.class);
    }

    private SceneSnapshot buildReleaseSnapshot(SceneReleaseDO release) {
        return EngineJson.read(EngineJson.write(release.getSnapshotJson()), SceneSnapshot.class);
    }

    private Map<String, Object> buildSummaryJson(LocalReplayRunner.ReplayDiffReport diffReport) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("sceneCode", diffReport.getSceneCode());
        summary.put("eventCount", diffReport.getEventCount());
        summary.put("changedEventCount", diffReport.getChangedEventCount());
        summary.put("changeRate", calculateChangeRate(diffReport.getChangedEventCount(), diffReport.getEventCount()));
        summary.put("baseline", buildSnapshotSummary(diffReport.getBaseline(), diffReport.getBaselineSummary()));
        summary.put("candidate", buildSnapshotSummary(diffReport.getCandidate(), diffReport.getCandidateSummary()));
        summary.put("topChangeTypes", buildChangeTypeCounter(diffReport.getDifferences()));
        summary.put("sampleSize", Math.min(MAX_SAMPLE_DIFF_COUNT, CollUtil.size(diffReport.getDifferences())));
        return summary;
    }

    private Map<String, Object> buildSnapshotSummary(LocalReplayRunner.SnapshotRef snapshotRef,
                                                     LocalReplayRunner.ReplaySummary replaySummary) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (snapshotRef != null) {
            summary.put("snapshotId", snapshotRef.getSnapshotId());
            summary.put("version", snapshotRef.getVersion());
            summary.put("checksum", snapshotRef.getChecksum());
        }
        if (replaySummary != null) {
            summary.put("finalActionCounts", replaySummary.getFinalActionCounts() == null
                    ? Collections.emptyMap() : new LinkedHashMap<>(replaySummary.getFinalActionCounts()));
            summary.put("matchedEventCount", replaySummary.getMatchedEventCount());
        }
        return summary;
    }

    private Map<String, Integer> buildChangeTypeCounter(Collection<LocalReplayRunner.ReplayEventDiff> differences) {
        if (CollUtil.isEmpty(differences)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> counter = new LinkedHashMap<>();
        for (LocalReplayRunner.ReplayEventDiff difference : differences) {
            for (String changeType : defaultList(difference.getChangeTypes())) {
                if (StrUtil.isBlank(changeType)) {
                    continue;
                }
                counter.merge(changeType.trim(), 1, Integer::sum);
            }
        }
        return counter.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private List<Map<String, Object>> buildSampleDiffJson(LocalReplayRunner.ReplayDiffReport diffReport) {
        if (diffReport == null || CollUtil.isEmpty(diffReport.getDifferences())) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> sampleDiffs = new ArrayList<>();
        for (LocalReplayRunner.ReplayEventDiff difference : diffReport.getDifferences().stream().limit(MAX_SAMPLE_DIFF_COUNT).toList()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("eventIndex", difference.getEventIndex());
            item.put("eventId", difference.getEventId());
            item.put("traceId", difference.getTraceId());
            item.put("changeTypes", defaultList(difference.getChangeTypes()));
            item.put("baselineAction", extractFinalAction(difference.getBaselineResult()));
            item.put("candidateAction", extractFinalAction(difference.getCandidateResult()));
            item.put("baselineHitRules", extractHitRuleCodes(difference.getBaselineResult()));
            item.put("candidateHitRules", extractHitRuleCodes(difference.getCandidateResult()));
            sampleDiffs.add(item);
        }
        return sampleDiffs;
    }

    private String extractFinalAction(LocalSimulationRunner.SimulationEventResult result) {
        return result == null || result.getFinalAction() == null ? null : result.getFinalAction().name();
    }

    private List<String> extractHitRuleCodes(LocalSimulationRunner.SimulationEventResult result) {
        if (result == null || CollUtil.isEmpty(result.getHitRules())) {
            return Collections.emptyList();
        }
        return result.getHitRules().stream()
                .map(LocalSimulationRunner.MatchedRule::getRuleCode)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private Double calculateChangeRate(Integer changedEventCount, Integer eventCount) {
        int total = ObjectUtil.defaultIfNull(eventCount, 0);
        if (total <= 0) {
            return 0D;
        }
        return ObjectUtil.defaultIfNull(changedEventCount, 0) * 1.0D / total;
    }

    private String resolveInputRef(String inputSourceType, String inputRef) {
        String normalized = trimToNull(inputRef);
        if (Objects.equals(normalizeUpperCode(inputSourceType), INPUT_SOURCE_DECISION_LOG_EXPORT) && normalized == null) {
            return DEFAULT_DECISION_LOG_INPUT_REF;
        }
        return normalized;
    }

    private String generateJobCode(ReplayJobCreateReqVO reqVO) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return (reqVO.getSceneCode() + "_REPLAY_" + reqVO.getBaselineVersionNo() + '_' + reqVO.getTargetVersionNo() + '_' + timestamp)
                .replace('-', '_');
    }

    private String normalizeUpperCode(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String trimToNull(String text) {
        return StrUtil.emptyToNull(StrUtil.trim(text));
    }

    private <T> List<T> defaultList(List<T> source) {
        return source == null ? Collections.emptyList() : source;
    }

}
