package cn.liboshuai.pulsix.module.risk.service.release;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.model.ActionType;
import cn.liboshuai.pulsix.engine.model.AggType;
import cn.liboshuai.pulsix.engine.model.DecisionMode;
import cn.liboshuai.pulsix.engine.model.DerivedFeatureSpec;
import cn.liboshuai.pulsix.engine.model.EngineType;
import cn.liboshuai.pulsix.engine.model.EventSchemaSpec;
import cn.liboshuai.pulsix.engine.model.FeatureType;
import cn.liboshuai.pulsix.engine.model.LookupFeatureSpec;
import cn.liboshuai.pulsix.engine.model.LookupType;
import cn.liboshuai.pulsix.engine.model.PolicyRuleRefSpec;
import cn.liboshuai.pulsix.engine.model.PolicySpec;
import cn.liboshuai.pulsix.engine.model.RuleSpec;
import cn.liboshuai.pulsix.engine.model.ScoreBandSpec;
import cn.liboshuai.pulsix.engine.model.RuntimeHints;
import cn.liboshuai.pulsix.engine.model.SceneSnapshot;
import cn.liboshuai.pulsix.engine.model.SceneSpec;
import cn.liboshuai.pulsix.engine.model.StreamFeatureSpec;
import cn.liboshuai.pulsix.engine.model.WindowType;
import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.util.monitor.TracerUtils;
import cn.liboshuai.pulsix.framework.security.core.util.SecurityFrameworkUtils;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.json.JsonUtils;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedValidateReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedValidateRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleaseCompileReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleasePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleasePublishReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleaseRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.release.vo.SceneReleaseRollbackReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RuleValidateReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RuleValidateRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventfield.EventFieldDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventschema.EventSchemaDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureDerivedConfDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureLookupConfDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureStreamConfDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.list.ListSetDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.policy.PolicyDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.policy.PolicyRuleRefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.policy.PolicyScoreBandDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.release.SceneReleaseDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.rule.RuleDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventfield.EventFieldMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventschema.EventSchemaMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.feature.FeatureDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.feature.FeatureDerivedConfMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.feature.FeatureLookupConfMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.feature.FeatureStreamConfMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.list.ListSetMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.policy.PolicyDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.policy.PolicyRuleRefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.policy.PolicyScoreBandMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.release.SceneReleaseMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.rule.RuleDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureTypeEnum;
import cn.liboshuai.pulsix.module.risk.service.auditlog.AuditLogService;
import cn.liboshuai.pulsix.module.risk.service.featurederived.FeatureDerivedService;
import cn.liboshuai.pulsix.module.risk.service.rule.RuleService;
import cn.liboshuai.pulsix.module.risk.util.RiskListRedisUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_RELEASE_ALREADY_ACTIVE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_RELEASE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_RELEASE_ROLLBACK_SOURCE_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_RELEASE_STATUS_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_RELEASE_VALIDATION_FAILED;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_COMPILE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_PUBLISH;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_ROLLBACK;

@Service
public class SceneReleaseServiceImpl implements SceneReleaseService {

    private static final String PUBLISH_STATUS_DRAFT = "DRAFT";
    private static final String PUBLISH_STATUS_PUBLISHED = "PUBLISHED";
    private static final String PUBLISH_STATUS_ACTIVE = "ACTIVE";
    private static final String PUBLISH_STATUS_ROLLED_BACK = "ROLLED_BACK";
    private static final String PUBLISH_STATUS_FAILED = "FAILED";
    private static final String VALIDATION_STATUS_PASSED = "PASSED";
    private static final String VALIDATION_STATUS_FAILED = "FAILED";
    private static final String SNAPSHOT_STATUS_DRAFT = "DRAFT";
    private static final String SNAPSHOT_STATUS_VALIDATED = "VALIDATED";
    private static final String CHECK_PASS = "PASS";
    private static final String CHECK_FAIL = "FAIL";
    private static final String CHECK_WARN = "WARN";

    @Resource
    private SceneReleaseMapper sceneReleaseMapper;

    @Resource
    private SceneMapper sceneMapper;

    @Resource
    private AuditLogService auditLogService;

    @Resource
    private EventSchemaMapper eventSchemaMapper;

    @Resource
    private EventFieldMapper eventFieldMapper;

    @Resource
    private FeatureDefMapper featureDefMapper;

    @Resource
    private FeatureStreamConfMapper featureStreamConfMapper;

    @Resource
    private FeatureLookupConfMapper featureLookupConfMapper;

    @Resource
    private FeatureDerivedConfMapper featureDerivedConfMapper;

    @Resource
    private RuleDefMapper ruleDefMapper;

    @Resource
    private PolicyDefMapper policyDefMapper;

    @Resource
    private PolicyRuleRefMapper policyRuleRefMapper;

    @Resource
    private PolicyScoreBandMapper policyScoreBandMapper;

    @Resource
    private ListSetMapper listSetMapper;

    @Resource
    private RuleService ruleService;

    @Resource
    private FeatureDerivedService featureDerivedService;

    private final SceneReleaseRuntimePreviewService runtimePreviewService = new SceneReleaseRuntimePreviewService();

    @Override
    public PageResult<SceneReleaseRespVO> getSceneReleasePage(SceneReleasePageReqVO pageReqVO) {
        PageResult<SceneReleaseDO> pageResult = sceneReleaseMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        return new PageResult<>(pageResult.getList().stream().map(this::buildRespVO).toList(), pageResult.getTotal());
    }

    @Override
    public SceneReleaseRespVO getSceneRelease(Long id) {
        return buildRespVO(validateSceneReleaseExists(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SceneReleaseRespVO compileSceneRelease(SceneReleaseCompileReqVO reqVO) {
        String sceneCode = StrUtil.trim(reqVO.getSceneCode());
        SceneDO scene = validateSceneExists(sceneCode);
        int nextVersionNo = ObjectUtil.defaultIfNull(sceneReleaseMapper.selectMaxVersionNo(sceneCode), 0) + 1;
        long startNanos = System.nanoTime();

        CompileContext context = loadCompileContext(scene);
        SceneSnapshot snapshot = buildSnapshot(scene, nextVersionNo, context);
        SceneReleaseRuntimePreviewResult runtimePreview = runtimePreviewService.preview(snapshot);
        Map<String, Object> validationReport = buildValidationReport(scene, snapshot, context, runtimePreview);
        boolean valid = VALIDATION_STATUS_PASSED.equals(validationReport.get("validationStatus"));
        snapshot.setStatus(valid ? SNAPSHOT_STATUS_VALIDATED : SNAPSHOT_STATUS_DRAFT);
        snapshot.setChecksum(null);
        String checksum = md5Hex(EngineJson.write(snapshot));
        snapshot.setChecksum(checksum);
        Map<String, Object> snapshotJson = JsonUtils.parseObject(EngineJson.write(snapshot), new TypeReference<Map<String, Object>>() {
        });
        Map<String, Object> dependencyDigest = buildDependencyDigest(scene, context, runtimePreview);
        long compileDurationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        SceneReleaseDO release = new SceneReleaseDO();
        release.setSceneCode(sceneCode);
        release.setVersionNo(nextVersionNo);
        release.setSnapshotJson(snapshotJson);
        release.setChecksum(checksum);
        release.setPublishStatus(PUBLISH_STATUS_DRAFT);
        release.setValidationStatus(valid ? VALIDATION_STATUS_PASSED : VALIDATION_STATUS_FAILED);
        release.setValidationReportJson(validationReport);
        release.setDependencyDigestJson(dependencyDigest);
        release.setCompileDurationMs(compileDurationMs);
        release.setCompiledFeatureCount(runtimePreview.isValid() ? runtimePreview.getFeatureCodes().size() : totalCompiledFeatures(snapshot));
        release.setCompiledRuleCount(runtimePreview.isValid() ? runtimePreview.getOrderedRuleCount() : CollUtil.size(snapshot.getRules()));
        release.setCompiledPolicyCount(snapshot.getPolicy() == null ? 0 : 1);
        release.setRemark(trimToNull(reqVO.getRemark()));
        sceneReleaseMapper.insert(release);
        writeReleaseAuditLog(release.getSceneCode(), buildReleaseBizCode(release), ACTION_COMPILE,
                null, buildReleaseAuditPayload(release), "编译场景发布版本 " + buildReleaseBizCode(release));
        return buildRespVO(release);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SceneReleaseRespVO publishSceneRelease(SceneReleasePublishReqVO reqVO) {
        SceneReleaseDO release = validateSceneReleaseExists(reqVO.getId());
        validateReleaseReadyForPublish(release);

        LocalDateTime publishedAt = LocalDateTime.now();
        LocalDateTime effectiveFrom = ObjectUtil.defaultIfNull(reqVO.getEffectiveFrom(), publishedAt);
        boolean effectiveNow = !effectiveFrom.isAfter(publishedAt);
        Map<String, Object> beforePayload = buildReleaseAuditPayload(release);

        if (effectiveNow) {
            updateCurrentActiveStatuses(release.getSceneCode(), release.getId(), PUBLISH_STATUS_PUBLISHED);
        }

        applyPublishMetadata(release, effectiveNow ? PUBLISH_STATUS_ACTIVE : PUBLISH_STATUS_PUBLISHED,
                publishedAt, effectiveFrom, resolveUpdatedRemark(reqVO.getRemark(), release.getRemark()));
        sceneReleaseMapper.updateById(release);
        writeReleaseAuditLog(release.getSceneCode(), buildReleaseBizCode(release), ACTION_PUBLISH,
                beforePayload, buildReleaseAuditPayload(release), buildPublishAuditRemark(release, effectiveNow));
        return buildRespVO(release);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SceneReleaseRespVO rollbackSceneRelease(SceneReleaseRollbackReqVO reqVO) {
        SceneReleaseDO sourceRelease = validateSceneReleaseExists(reqVO.getId());
        validateReleaseAvailableForRollback(sourceRelease);

        LocalDateTime publishedAt = LocalDateTime.now();
        LocalDateTime effectiveFrom = ObjectUtil.defaultIfNull(reqVO.getEffectiveFrom(), publishedAt);
        boolean effectiveNow = !effectiveFrom.isAfter(publishedAt);
        SceneReleaseDO currentActiveRelease = getCurrentActiveRelease(sourceRelease.getSceneCode());
        if (currentActiveRelease != null && Objects.equals(currentActiveRelease.getId(), sourceRelease.getId())) {
            throw ServiceExceptionUtil.exception(SCENE_RELEASE_ALREADY_ACTIVE);
        }
        if (effectiveNow) {
            updateCurrentActiveStatuses(sourceRelease.getSceneCode(), null, PUBLISH_STATUS_ROLLED_BACK);
        }

        int nextVersionNo = ObjectUtil.defaultIfNull(sceneReleaseMapper.selectMaxVersionNo(sourceRelease.getSceneCode()), 0) + 1;
        SceneReleaseDO rollbackRelease = buildRollbackRelease(sourceRelease, nextVersionNo, publishedAt, effectiveFrom,
                effectiveNow ? PUBLISH_STATUS_ACTIVE : PUBLISH_STATUS_PUBLISHED, reqVO.getRemark());
        sceneReleaseMapper.insert(rollbackRelease);
        writeReleaseAuditLog(sourceRelease.getSceneCode(), buildReleaseBizCode(rollbackRelease), ACTION_ROLLBACK,
                buildReleaseAuditPayload(sourceRelease), buildReleaseAuditPayload(rollbackRelease),
                buildRollbackAuditRemark(sourceRelease, rollbackRelease, currentActiveRelease, effectiveNow));
        return buildRespVO(rollbackRelease);
    }

    private void validateReleaseReadyForPublish(SceneReleaseDO release) {
        if (!VALIDATION_STATUS_PASSED.equals(release.getValidationStatus())) {
            throw ServiceExceptionUtil.exception(SCENE_RELEASE_VALIDATION_FAILED);
        }
        if (!PUBLISH_STATUS_DRAFT.equals(release.getPublishStatus())) {
            throw ServiceExceptionUtil.exception(SCENE_RELEASE_STATUS_INVALID, "正式发布");
        }
    }

    private void validateReleaseAvailableForRollback(SceneReleaseDO release) {
        if (!VALIDATION_STATUS_PASSED.equals(release.getValidationStatus())) {
            throw ServiceExceptionUtil.exception(SCENE_RELEASE_VALIDATION_FAILED);
        }
        if (PUBLISH_STATUS_DRAFT.equals(release.getPublishStatus()) || PUBLISH_STATUS_FAILED.equals(release.getPublishStatus())) {
            throw ServiceExceptionUtil.exception(SCENE_RELEASE_ROLLBACK_SOURCE_INVALID);
        }
    }

    private SceneReleaseDO buildRollbackRelease(SceneReleaseDO sourceRelease, int nextVersionNo, LocalDateTime publishedAt,
                                                LocalDateTime effectiveFrom, String publishStatus, String remark) {
        SceneReleaseDO release = new SceneReleaseDO();
        release.setSceneCode(sourceRelease.getSceneCode());
        release.setVersionNo(nextVersionNo);
        release.setValidationStatus(sourceRelease.getValidationStatus());
        release.setValidationReportJson(sourceRelease.getValidationReportJson());
        release.setDependencyDigestJson(sourceRelease.getDependencyDigestJson());
        release.setCompileDurationMs(sourceRelease.getCompileDurationMs());
        release.setCompiledFeatureCount(sourceRelease.getCompiledFeatureCount());
        release.setCompiledRuleCount(sourceRelease.getCompiledRuleCount());
        release.setCompiledPolicyCount(sourceRelease.getCompiledPolicyCount());
        release.setRollbackFromVersion(sourceRelease.getVersionNo());
        applyPublishMetadata(release, publishStatus, publishedAt, effectiveFrom,
                resolveUpdatedRemark(remark, "回滚到 v" + sourceRelease.getVersionNo()), sourceRelease.getSnapshotJson());
        return release;
    }

    private void applyPublishMetadata(SceneReleaseDO release, String publishStatus, LocalDateTime publishedAt,
                                      LocalDateTime effectiveFrom, String remark) {
        applyPublishMetadata(release, publishStatus, publishedAt, effectiveFrom, remark, release.getSnapshotJson());
    }

    private void applyPublishMetadata(SceneReleaseDO release, String publishStatus, LocalDateTime publishedAt,
                                      LocalDateTime effectiveFrom, String remark, Map<String, Object> sourceSnapshotJson) {
        release.setPublishStatus(publishStatus);
        release.setPublishedBy(resolveOperatorName());
        release.setPublishedAt(publishedAt);
        release.setEffectiveFrom(effectiveFrom);
        release.setRemark(remark);

        SceneSnapshot snapshot = buildReleaseSnapshot(sourceSnapshotJson);
        snapshot.setSnapshotId(release.getSceneCode() + "_v" + release.getVersionNo());
        snapshot.setSceneCode(release.getSceneCode());
        snapshot.setVersion(release.getVersionNo());
        snapshot.setStatus(publishStatus);
        snapshot.setPublishedAt(toInstant(publishedAt));
        snapshot.setEffectiveFrom(toInstant(effectiveFrom));
        snapshot.setChecksum(null);
        String checksum = md5Hex(EngineJson.write(snapshot));
        snapshot.setChecksum(checksum);
        release.setChecksum(checksum);
        release.setSnapshotJson(JsonUtils.parseObject(EngineJson.write(snapshot), new TypeReference<Map<String, Object>>() {
        }));
    }

    private SceneSnapshot buildReleaseSnapshot(Map<String, Object> snapshotJson) {
        if (snapshotJson == null || snapshotJson.isEmpty()) {
            throw new IllegalStateException("scene release snapshot_json must not be empty");
        }
        return EngineJson.read(EngineJson.write(snapshotJson), SceneSnapshot.class);
    }

    private SceneReleaseDO getCurrentActiveRelease(String sceneCode) {
        List<SceneReleaseDO> releases = sceneReleaseMapper.selectList(new LambdaQueryWrapperX<SceneReleaseDO>()
                .eq(SceneReleaseDO::getSceneCode, sceneCode)
                .eq(SceneReleaseDO::getPublishStatus, PUBLISH_STATUS_ACTIVE)
                .orderByDesc(SceneReleaseDO::getEffectiveFrom)
                .orderByDesc(SceneReleaseDO::getPublishedAt)
                .orderByDesc(SceneReleaseDO::getVersionNo)
                .orderByDesc(SceneReleaseDO::getId));
        return CollUtil.isEmpty(releases) ? null : releases.get(0);
    }

    private void updateCurrentActiveStatuses(String sceneCode, Long excludeId, String targetStatus) {
        List<SceneReleaseDO> activeReleases = sceneReleaseMapper.selectList(new LambdaQueryWrapperX<SceneReleaseDO>()
                .eq(SceneReleaseDO::getSceneCode, sceneCode)
                .eq(SceneReleaseDO::getPublishStatus, PUBLISH_STATUS_ACTIVE));
        for (SceneReleaseDO activeRelease : defaultList(activeReleases)) {
            if (excludeId != null && Objects.equals(excludeId, activeRelease.getId())) {
                continue;
            }
            SceneReleaseDO updateObj = new SceneReleaseDO();
            updateObj.setId(activeRelease.getId());
            updateObj.setPublishStatus(targetStatus);
            sceneReleaseMapper.updateById(updateObj);
        }
    }

    private Map<String, Object> buildReleaseAuditPayload(SceneReleaseDO release) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", release.getId());
        payload.put("sceneCode", release.getSceneCode());
        payload.put("versionNo", release.getVersionNo());
        payload.put("publishStatus", release.getPublishStatus());
        payload.put("validationStatus", release.getValidationStatus());
        payload.put("checksum", release.getChecksum());
        payload.put("publishedBy", release.getPublishedBy());
        payload.put("publishedAt", release.getPublishedAt());
        payload.put("effectiveFrom", release.getEffectiveFrom());
        payload.put("rollbackFromVersion", release.getRollbackFromVersion());
        payload.put("compiledFeatureCount", release.getCompiledFeatureCount());
        payload.put("compiledRuleCount", release.getCompiledRuleCount());
        payload.put("compiledPolicyCount", release.getCompiledPolicyCount());
        payload.put("remark", release.getRemark());
        return payload;
    }

    private void writeReleaseAuditLog(String sceneCode, String bizCode, String actionType, Map<String, Object> beforeJson,
                                      Map<String, Object> afterJson, String remark) {
        auditLogService.createAuditLog(sceneCode, cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.BIZ_TYPE_RELEASE,
                bizCode, actionType, beforeJson, afterJson, remark);
    }

    private String buildReleaseBizCode(SceneReleaseDO release) {
        return release.getSceneCode() + "_v" + release.getVersionNo();
    }

    private String buildPublishAuditRemark(SceneReleaseDO release, boolean effectiveNow) {
        return effectiveNow
                ? "正式发布版本 v" + release.getVersionNo() + " 并立即生效"
                : "正式发布版本 v" + release.getVersionNo() + "，计划于 " + release.getEffectiveFrom() + " 生效";
    }

    private String buildRollbackAuditRemark(SceneReleaseDO sourceRelease, SceneReleaseDO rollbackRelease,
                                            SceneReleaseDO currentActiveRelease, boolean effectiveNow) {
        StringBuilder remark = new StringBuilder();
        remark.append("基于历史版本 v").append(sourceRelease.getVersionNo())
                .append(" 生成回滚版本 v").append(rollbackRelease.getVersionNo());
        if (currentActiveRelease != null) {
            remark.append("，当前生效版本为 v").append(currentActiveRelease.getVersionNo());
        }
        if (effectiveNow) {
            remark.append("，已立即生效");
        } else {
            remark.append("，计划于 ").append(rollbackRelease.getEffectiveFrom()).append(" 生效");
        }
        return remark.toString();
    }

    private String resolveUpdatedRemark(String preferredRemark, String fallbackRemark) {
        String remark = trimToNull(preferredRemark);
        return remark != null ? remark : trimToNull(fallbackRemark);
    }

    private String resolveOperatorName() {
        String nickname = trimToNull(SecurityFrameworkUtils.getLoginUserNickname());
        if (nickname != null) {
            return nickname;
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return userId != null ? String.valueOf(userId) : "system";
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private CompileContext loadCompileContext(SceneDO scene) {
        CompileContext context = new CompileContext();
        context.scene = scene;
        context.eventSchemas = eventSchemaMapper.selectList(new LambdaQueryWrapperX<EventSchemaDO>()
                .eq(EventSchemaDO::getSceneCode, scene.getSceneCode())
                .orderByDesc(EventSchemaDO::getVersion)
                .orderByDesc(EventSchemaDO::getId));
        context.eventSchemaMap = context.eventSchemas.stream()
                .collect(Collectors.toMap(EventSchemaDO::getEventCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        context.defaultEventSchema = StrUtil.isBlank(scene.getDefaultEventCode()) ? null : context.eventSchemaMap.get(scene.getDefaultEventCode());
        context.eventFields = eventFieldMapper.selectList(new LambdaQueryWrapperX<EventFieldDO>()
                .eq(EventFieldDO::getSceneCode, scene.getSceneCode())
                .eqIfPresent(EventFieldDO::getEventCode, scene.getDefaultEventCode())
                .orderByAsc(EventFieldDO::getSortNo)
                .orderByAsc(EventFieldDO::getId));

        List<FeatureDefDO> featureDefs = featureDefMapper.selectList(new LambdaQueryWrapperX<FeatureDefDO>()
                .eq(FeatureDefDO::getSceneCode, scene.getSceneCode())
                .orderByAsc(FeatureDefDO::getFeatureType)
                .orderByAsc(FeatureDefDO::getFeatureCode));
        context.streamFeatureDefs = filterEnabledFeatures(featureDefs, RiskFeatureTypeEnum.STREAM.getType());
        context.lookupFeatureDefs = filterEnabledFeatures(featureDefs, RiskFeatureTypeEnum.LOOKUP.getType());
        context.derivedFeatureDefs = filterEnabledFeatures(featureDefs, RiskFeatureTypeEnum.DERIVED.getType());
        context.streamConfMap = toMap(featureStreamConfMapper.selectListBySceneCodesAndFeatureCodes(
                Set.of(scene.getSceneCode()), codesOf(context.streamFeatureDefs)), FeatureStreamConfDO::getFeatureCode);
        context.lookupConfMap = toMap(featureLookupConfMapper.selectListBySceneCodesAndFeatureCodes(
                Set.of(scene.getSceneCode()), codesOf(context.lookupFeatureDefs)), FeatureLookupConfDO::getFeatureCode);
        context.derivedConfMap = toMap(featureDerivedConfMapper.selectListBySceneCodesAndFeatureCodes(
                Set.of(scene.getSceneCode()), codesOf(context.derivedFeatureDefs)), FeatureDerivedConfDO::getFeatureCode);

        context.allRules = ruleDefMapper.selectList(new LambdaQueryWrapperX<RuleDefDO>()
                .eq(RuleDefDO::getSceneCode, scene.getSceneCode())
                .orderByDesc(RuleDefDO::getPriority)
                .orderByAsc(RuleDefDO::getId));
        context.allRuleMap = context.allRules.stream()
                .collect(Collectors.toMap(RuleDefDO::getRuleCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        context.enabledRules = context.allRules.stream().filter(item -> CommonStatusEnum.isEnable(item.getStatus())).toList();
        context.enabledRuleMap = context.enabledRules.stream()
                .collect(Collectors.toMap(RuleDefDO::getRuleCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        context.policy = StrUtil.isBlank(scene.getDefaultPolicyCode()) ? null
                : policyDefMapper.selectBySceneAndPolicyCode(scene.getSceneCode(), scene.getDefaultPolicyCode());
        context.policyRuleRefs = context.policy == null ? Collections.emptyList()
                : policyRuleRefMapper.selectListBySceneAndPolicyCode(scene.getSceneCode(), context.policy.getPolicyCode());
        context.policyScoreBands = context.policy == null ? Collections.emptyList()
                : policyScoreBandMapper.selectListBySceneAndPolicyCode(scene.getSceneCode(), context.policy.getPolicyCode());
        context.listSets = listSetMapper.selectList(new LambdaQueryWrapperX<ListSetDO>()
                .eq(ListSetDO::getSceneCode, scene.getSceneCode())
                .orderByAsc(ListSetDO::getListCode)
                .orderByAsc(ListSetDO::getId));
        return context;
    }

    private SceneSnapshot buildSnapshot(SceneDO scene, Integer versionNo, CompileContext context) {
        SceneSnapshot snapshot = new SceneSnapshot();
        snapshot.setSnapshotId(scene.getSceneCode() + "_v" + versionNo);
        snapshot.setSceneCode(scene.getSceneCode());
        snapshot.setSceneName(scene.getSceneName());
        snapshot.setVersion(versionNo);
        snapshot.setStatus(SNAPSHOT_STATUS_DRAFT);
        snapshot.setRuntimeMode(resolveRuntimeMode(scene.getAccessMode()));
        snapshot.setScene(buildSceneSpec(scene, context.defaultEventSchema));
        snapshot.setEventSchema(buildEventSchemaSpec(context.defaultEventSchema, context.eventFields));
        snapshot.setVariables(buildVariables(context.eventFields));
        snapshot.setStreamFeatures(buildStreamFeatures(context));
        snapshot.setLookupFeatures(buildLookupFeatures(context));
        snapshot.setDerivedFeatures(buildDerivedFeatures(context));
        snapshot.setRules(buildRules(context));
        snapshot.setPolicy(buildPolicySpec(context));
        snapshot.setRuntimeHints(buildRuntimeHints(snapshot));
        return snapshot;
    }

    private Map<String, Object> buildValidationReport(SceneDO scene,
                                                      SceneSnapshot snapshot,
                                                      CompileContext context,
                                                      SceneReleaseRuntimePreviewResult runtimePreview) {
        List<Map<String, Object>> checks = new ArrayList<>();
        List<Map<String, Object>> warnings = new ArrayList<>();

        if (StrUtil.isBlank(scene.getDefaultEventCode())) {
            checks.add(check("SCENE", scene.getSceneCode(), CHECK_FAIL, "场景未配置默认事件编码"));
        } else {
            checks.add(check("SCENE", scene.getSceneCode(), CHECK_PASS, "已配置默认事件编码：" + scene.getDefaultEventCode()));
        }
        if (StrUtil.isBlank(scene.getDefaultPolicyCode())) {
            checks.add(check("SCENE", scene.getSceneCode(), CHECK_FAIL, "场景未配置默认策略编码"));
        } else {
            checks.add(check("SCENE", scene.getSceneCode(), CHECK_PASS, "已配置默认策略编码：" + scene.getDefaultPolicyCode()));
        }
        if (context.defaultEventSchema == null) {
            checks.add(check("EVENT_SCHEMA", scene.getDefaultEventCode(), CHECK_FAIL, "默认事件 Schema 不存在"));
        } else {
            checks.add(check("EVENT_SCHEMA", context.defaultEventSchema.getEventCode(), CHECK_PASS,
                    "已定位默认事件 Schema，事件类型=" + context.defaultEventSchema.getEventType()));
        }
        if (CollUtil.isEmpty(context.eventFields)) {
            checks.add(check("EVENT_FIELD", scene.getDefaultEventCode(), CHECK_FAIL, "默认事件未配置字段定义"));
        } else {
            checks.add(check("EVENT_FIELD", scene.getDefaultEventCode(), CHECK_PASS,
                    "已加载字段定义数量=" + context.eventFields.size()));
        }

        validateStreamFeatures(context, checks);
        validateLookupFeatures(context, checks, warnings);
        validateDerivedFeatures(context, checks, warnings);
        validateRules(context, checks);
        validatePolicy(scene, context, checks);

        checks.add(check("SNAPSHOT", snapshot.getSnapshotId(),
                runtimePreview.isValid() ? CHECK_PASS : CHECK_FAIL,
                runtimePreview.summaryMessage()));

        long passCount = checks.stream().filter(item -> CHECK_PASS.equals(item.get("result"))).count();
        long failCount = checks.stream().filter(item -> CHECK_FAIL.equals(item.get("result"))).count();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("passCount", passCount);
        summary.put("failCount", failCount);
        summary.put("warningCount", warnings.size());
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("sceneCode", scene.getSceneCode());
        report.put("valid", failCount == 0);
        report.put("validationStatus", failCount == 0 ? VALIDATION_STATUS_PASSED : VALIDATION_STATUS_FAILED);
        report.put("checks", checks);
        report.put("warnings", warnings);
        report.put("runtimeCompilePreview", runtimePreview.toValidationPreviewMap());
        report.put("summary", summary);
        return report;
    }

    private void validateStreamFeatures(CompileContext context, List<Map<String, Object>> checks) {
        for (FeatureDefDO featureDef : context.streamFeatureDefs) {
            FeatureStreamConfDO conf = context.streamConfMap.get(featureDef.getFeatureCode());
            if (conf == null) {
                checks.add(check("STREAM_FEATURE", featureDef.getFeatureCode(), CHECK_FAIL, "缺少流式特征配置"));
                continue;
            }
            if (CommonStatusEnum.isDisable(conf.getStatus())) {
                checks.add(check("STREAM_FEATURE", featureDef.getFeatureCode(), CHECK_FAIL, "流式特征配置已停用"));
                continue;
            }
            if (StrUtil.hasBlank(conf.getSourceEventCodes(), conf.getEntityKeyExpr(), conf.getAggType(), conf.getWindowType())
                    || conf.getTtlSeconds() == null || conf.getTtlSeconds() <= 0) {
                checks.add(check("STREAM_FEATURE", featureDef.getFeatureCode(), CHECK_FAIL, "窗口、聚合或 TTL 配置不完整"));
                continue;
            }
            checks.add(check("STREAM_FEATURE", featureDef.getFeatureCode(), CHECK_PASS, "流式特征配置完整"));
        }
    }

    private void validateLookupFeatures(CompileContext context,
                                        List<Map<String, Object>> checks,
                                        List<Map<String, Object>> warnings) {
        Map<String, ListSetDO> listPrefixMap = buildListPrefixMap(context.listSets);
        for (FeatureDefDO featureDef : context.lookupFeatureDefs) {
            FeatureLookupConfDO conf = context.lookupConfMap.get(featureDef.getFeatureCode());
            if (conf == null) {
                checks.add(check("LOOKUP_FEATURE", featureDef.getFeatureCode(), CHECK_FAIL, "缺少查询特征配置"));
                continue;
            }
            if (CommonStatusEnum.isDisable(conf.getStatus())) {
                checks.add(check("LOOKUP_FEATURE", featureDef.getFeatureCode(), CHECK_FAIL, "查询特征配置已停用"));
                continue;
            }
            if (StrUtil.hasBlank(conf.getLookupType(), conf.getKeyExpr(), conf.getSourceRef())) {
                checks.add(check("LOOKUP_FEATURE", featureDef.getFeatureCode(), CHECK_FAIL, "lookupType、keyExpr 或 sourceRef 未配置"));
                continue;
            }
            checks.add(check("LOOKUP_FEATURE", featureDef.getFeatureCode(), CHECK_PASS, "查询特征配置完整"));
            if (StrUtil.startWith(conf.getSourceRef(), "pulsix:list:") && !listPrefixMap.containsKey(conf.getSourceRef())) {
                warnings.add(check("LOOKUP_FEATURE", featureDef.getFeatureCode(), CHECK_WARN,
                        "未找到与 sourceRef 对应的名单集合：" + conf.getSourceRef()));
            }
        }
    }

    private void validateDerivedFeatures(CompileContext context,
                                         List<Map<String, Object>> checks,
                                         List<Map<String, Object>> warnings) {
        Set<String> availableCodes = context.availableDependencyCodes();
        for (FeatureDefDO featureDef : context.derivedFeatureDefs) {
            FeatureDerivedConfDO conf = context.derivedConfMap.get(featureDef.getFeatureCode());
            if (conf == null) {
                checks.add(check("DERIVED_FEATURE", featureDef.getFeatureCode(), CHECK_FAIL, "缺少派生特征配置"));
                continue;
            }
            if (CommonStatusEnum.isDisable(conf.getStatus())) {
                checks.add(check("DERIVED_FEATURE", featureDef.getFeatureCode(), CHECK_FAIL, "派生特征配置已停用"));
                continue;
            }
            FeatureDerivedValidateReqVO validateReqVO = new FeatureDerivedValidateReqVO();
            validateReqVO.setSceneCode(featureDef.getSceneCode());
            validateReqVO.setFeatureCode(featureDef.getFeatureCode());
            validateReqVO.setEngineType(conf.getEngineType());
            validateReqVO.setExprContent(conf.getExprContent());
            validateReqVO.setDependsOnJson(defaultList(conf.getDependsOnJson()));
            validateReqVO.setSandboxFlag(ObjectUtil.defaultIfNull(conf.getSandboxFlag(), 1));
            FeatureDerivedValidateRespVO respVO = featureDerivedService.validateExpression(validateReqVO);
            List<String> extractedCodes = extractReferencedCodes(conf.getExprContent(), availableCodes);
            List<String> missingDeclared = extractedCodes.stream()
                    .filter(code -> !defaultList(conf.getDependsOnJson()).contains(code))
                    .toList();
            if (Boolean.TRUE.equals(respVO.getValid()) && CollUtil.isEmpty(missingDeclared)) {
                checks.add(check("DERIVED_FEATURE", featureDef.getFeatureCode(), CHECK_PASS, respVO.getMessage()));
            } else {
                String message = Boolean.TRUE.equals(respVO.getValid())
                        ? "表达式引用了未声明依赖：" + String.join("、", missingDeclared)
                        : respVO.getMessage();
                checks.add(check("DERIVED_FEATURE", featureDef.getFeatureCode(), CHECK_FAIL, message));
            }
            List<String> unusedDeclared = defaultList(conf.getDependsOnJson()).stream()
                    .filter(code -> !extractedCodes.contains(code))
                    .toList();
            if (CollUtil.isNotEmpty(unusedDeclared)) {
                warnings.add(check("DERIVED_FEATURE", featureDef.getFeatureCode(), CHECK_WARN,
                        "声明了但未在表达式中使用的依赖：" + String.join("、", unusedDeclared)));
            }
        }
    }

    private void validateRules(CompileContext context, List<Map<String, Object>> checks) {
        for (RuleDefDO rule : context.enabledRules) {
            RuleValidateReqVO reqVO = new RuleValidateReqVO();
            reqVO.setSceneCode(rule.getSceneCode());
            reqVO.setEngineType(rule.getEngineType());
            reqVO.setExprContent(rule.getExprContent());
            reqVO.setHitReasonTemplate(rule.getHitReasonTemplate());
            RuleValidateRespVO respVO = ruleService.validateRule(reqVO);
            checks.add(check("RULE", rule.getRuleCode(), Boolean.TRUE.equals(respVO.getValid()) ? CHECK_PASS : CHECK_FAIL,
                    respVO.getMessage()));
        }
    }

    private void validatePolicy(SceneDO scene, CompileContext context, List<Map<String, Object>> checks) {
        if (context.policy == null) {
            checks.add(check("POLICY", scene.getDefaultPolicyCode(), CHECK_FAIL, "默认策略不存在"));
            return;
        }
        if (CommonStatusEnum.isDisable(context.policy.getStatus())) {
            checks.add(check("POLICY", context.policy.getPolicyCode(), CHECK_FAIL, "默认策略已停用"));
        } else {
            checks.add(check("POLICY", context.policy.getPolicyCode(), CHECK_PASS, "默认策略已加载"));
        }
        if (CollUtil.isEmpty(context.policyRuleRefs)) {
            checks.add(check("POLICY", context.policy.getPolicyCode(), CHECK_FAIL, "策略未绑定规则"));
            return;
        }
        if (DecisionMode.SCORE_CARD == parseEnum(context.policy.getDecisionMode(), DecisionMode.class)) {
            if (CollUtil.isEmpty(context.policyScoreBands)) {
                checks.add(check("POLICY_SCORE_BAND", context.policy.getPolicyCode(), CHECK_FAIL, "SCORE_CARD 策略未配置评分段"));
            } else {
                checks.add(check("POLICY_SCORE_BAND", context.policy.getPolicyCode(), CHECK_PASS, "已加载评分段数量=" + context.policyScoreBands.size()));
            }
        }
        for (PolicyRuleRefDO ref : context.policyRuleRefs) {
            RuleDefDO referencedRule = context.allRuleMap.get(ref.getRuleCode());
            if (referencedRule == null) {
                checks.add(check("POLICY_RULE_REF", ref.getRuleCode(), CHECK_FAIL, "策略引用了不存在的规则"));
                continue;
            }
            if (!context.enabledRuleMap.containsKey(ref.getRuleCode())) {
                checks.add(check("POLICY_RULE_REF", ref.getRuleCode(), CHECK_FAIL, "策略引用了已停用的规则"));
                continue;
            }
            checks.add(check("POLICY_RULE_REF", ref.getRuleCode(), CHECK_PASS, "策略规则引用有效"));
        }
    }

    private Map<String, Object> buildDependencyDigest(SceneDO scene,
                                                      CompileContext context,
                                                      SceneReleaseRuntimePreviewResult runtimePreview) {
        Map<String, ListSetDO> listPrefixMap = buildListPrefixMap(context.listSets);
        Map<String, Object> digest = new LinkedHashMap<>();
        digest.put("sceneCode", scene.getSceneCode());
        digest.put("sceneName", scene.getSceneName());
        digest.put("defaultEventCode", scene.getDefaultEventCode());
        digest.put("defaultPolicyCode", scene.getDefaultPolicyCode());
        digest.put("eventFields", context.eventFields.stream().map(field -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fieldCode", field.getFieldCode());
            item.put("fieldName", field.getFieldName());
            item.put("fieldType", field.getFieldType());
            item.put("required", ObjectUtil.equal(field.getRequiredFlag(), 1));
            item.put("fieldPath", field.getFieldPath());
            return item;
        }).toList());
        digest.put("streamFeatures", context.streamFeatureDefs.stream().map(featureDef -> {
            FeatureStreamConfDO conf = context.streamConfMap.get(featureDef.getFeatureCode());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("featureCode", featureDef.getFeatureCode());
            item.put("featureName", featureDef.getFeatureName());
            item.put("entityType", featureDef.getEntityType());
            item.put("sourceEventCodes", parseCodes(conf == null ? null : conf.getSourceEventCodes()));
            item.put("sourceEventTypes", resolveSourceEventTypes(conf == null ? null : conf.getSourceEventCodes(), context.eventSchemaMap));
            item.put("aggType", conf == null ? null : conf.getAggType());
            item.put("windowType", conf == null ? null : conf.getWindowType());
            item.put("windowSize", conf == null ? null : conf.getWindowSize());
            item.put("windowSlide", conf == null ? null : conf.getWindowSlide());
            return item;
        }).toList());
        digest.put("lookupFeatures", context.lookupFeatureDefs.stream().map(featureDef -> {
            FeatureLookupConfDO conf = context.lookupConfMap.get(featureDef.getFeatureCode());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("featureCode", featureDef.getFeatureCode());
            item.put("featureName", featureDef.getFeatureName());
            item.put("lookupType", conf == null ? null : conf.getLookupType());
            item.put("sourceRef", conf == null ? null : conf.getSourceRef());
            ListSetDO linkedList = conf == null ? null : listPrefixMap.get(conf.getSourceRef());
            item.put("linkedListCode", linkedList == null ? null : linkedList.getListCode());
            item.put("linkedListName", linkedList == null ? null : linkedList.getListName());
            return item;
        }).toList());
        digest.put("derivedFeatures", context.derivedFeatureDefs.stream().map(featureDef -> {
            FeatureDerivedConfDO conf = context.derivedConfMap.get(featureDef.getFeatureCode());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("featureCode", featureDef.getFeatureCode());
            item.put("featureName", featureDef.getFeatureName());
            item.put("dependsOn", conf == null ? Collections.emptyList() : defaultList(conf.getDependsOnJson()));
            item.put("engineType", conf == null ? null : conf.getEngineType());
            return item;
        }).toList());
        digest.put("rules", context.enabledRules.stream().map(rule -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ruleCode", rule.getRuleCode());
            item.put("ruleName", rule.getRuleName());
            item.put("priority", rule.getPriority());
            item.put("hitAction", rule.getHitAction());
            item.put("dependsOn", extractReferencedCodes(rule.getExprContent(), context.availableDependencyCodes()));
            return item;
        }).toList());
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("policyCode", context.policy == null ? null : context.policy.getPolicyCode());
        policy.put("policyName", context.policy == null ? null : context.policy.getPolicyName());
        policy.put("decisionMode", context.policy == null ? null : context.policy.getDecisionMode());
        policy.put("defaultAction", context.policy == null ? null : context.policy.getDefaultAction());
        policy.put("scoreCalcMode", context.policy == null ? null : context.policy.getScoreCalcMode());
        policy.put("scoreBands", context.policyScoreBands.stream()
                .sorted(Comparator.comparing(PolicyScoreBandDO::getBandNo, Comparator.nullsLast(Integer::compareTo)))
                .map(scoreBand -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("bandNo", scoreBand.getBandNo());
                    item.put("minScore", scoreBand.getMinScore());
                    item.put("maxScore", scoreBand.getMaxScore());
                    item.put("hitAction", scoreBand.getHitAction());
                    item.put("hitReasonTemplate", scoreBand.getHitReasonTemplate());
                    return item;
                }).toList());
        policy.put("ruleOrder", context.policyRuleRefs.stream()
                .sorted(Comparator.comparing(PolicyRuleRefDO::getOrderNo, Comparator.nullsLast(Integer::compareTo)))
                .map(PolicyRuleRefDO::getRuleCode)
                .toList());
        digest.put("policy", policy);
        digest.put("listSets", context.listSets.stream().map(item -> {
            Map<String, Object> listItem = new LinkedHashMap<>();
            listItem.put("listCode", item.getListCode());
            listItem.put("listName", item.getListName());
            listItem.put("matchType", item.getMatchType());
            listItem.put("listType", item.getListType());
            listItem.put("storageType", item.getStorageType());
            listItem.put("redisKeyPrefix", RiskListRedisUtils.buildRedisKeyPrefix(item.getListCode(), item.getListType(), item.getMatchType()));
            return listItem;
        }).toList());
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("eventFieldCount", context.eventFields.size());
        counts.put("streamFeatureCount", context.streamFeatureDefs.size());
        counts.put("lookupFeatureCount", context.lookupFeatureDefs.size());
        counts.put("derivedFeatureCount", context.derivedFeatureDefs.size());
        counts.put("ruleCount", context.enabledRules.size());
        counts.put("policyRuleRefCount", context.policyRuleRefs.size());
        counts.put("policyScoreBandCount", context.policyScoreBands.size());
        digest.put("counts", counts);
        digest.put("runtimePreview", runtimePreview.toDependencyPreviewMap());
        return digest;
    }

    private SceneSpec buildSceneSpec(SceneDO scene, EventSchemaDO defaultEventSchema) {
        SceneSpec sceneSpec = new SceneSpec();
        sceneSpec.setDefaultPolicyCode(scene.getDefaultPolicyCode());
        sceneSpec.setAllowedEventTypes(defaultEventSchema == null || StrUtil.isBlank(defaultEventSchema.getEventType())
                ? Collections.emptyList() : Collections.singletonList(defaultEventSchema.getEventType().trim().toLowerCase(Locale.ROOT)));
        sceneSpec.setDecisionTimeoutMs(500);
        sceneSpec.setLogLevel("FULL");
        return sceneSpec;
    }

    private EventSchemaSpec buildEventSchemaSpec(EventSchemaDO eventSchema, List<EventFieldDO> eventFields) {
        if (eventSchema == null) {
            return null;
        }
        EventSchemaSpec spec = new EventSchemaSpec();
        spec.setEventCode(eventSchema.getEventCode());
        spec.setEventType(StrUtil.blankToDefault(eventSchema.getEventType(), "").trim().toLowerCase(Locale.ROOT));
        spec.setRequiredFields(eventFields.stream()
                .filter(item -> ObjectUtil.equal(item.getRequiredFlag(), 1))
                .map(EventFieldDO::getFieldCode)
                .toList());
        spec.setOptionalFields(eventFields.stream()
                .filter(item -> !ObjectUtil.equal(item.getRequiredFlag(), 1))
                .map(EventFieldDO::getFieldCode)
                .toList());
        return spec;
    }

    private Map<String, List<String>> buildVariables(List<EventFieldDO> eventFields) {
        Map<String, List<String>> variables = new LinkedHashMap<>();
        variables.put("baseFields", eventFields.stream().map(EventFieldDO::getFieldCode).toList());
        return variables;
    }

    private List<StreamFeatureSpec> buildStreamFeatures(CompileContext context) {
        List<StreamFeatureSpec> result = new ArrayList<>();
        for (FeatureDefDO featureDef : context.streamFeatureDefs) {
            FeatureStreamConfDO conf = context.streamConfMap.get(featureDef.getFeatureCode());
            StreamFeatureSpec spec = new StreamFeatureSpec();
            spec.setCode(featureDef.getFeatureCode());
            spec.setName(featureDef.getFeatureName());
            spec.setType(FeatureType.STREAM);
            spec.setValueType(featureDef.getValueType());
            spec.setDescription(featureDef.getDescription());
            if (conf != null) {
                spec.setSourceEventTypes(resolveSourceEventTypes(conf.getSourceEventCodes(), context.eventSchemaMap));
                spec.setEntityType(featureDef.getEntityType());
                spec.setEntityKeyExpr(conf.getEntityKeyExpr());
                spec.setAggType(parseEnum(conf.getAggType(), AggType.class));
                spec.setValueExpr(conf.getValueExpr());
                spec.setFilterExpr(conf.getFilterExpr());
                spec.setWindowType(parseEnum(conf.getWindowType(), WindowType.class));
                spec.setWindowSize(conf.getWindowSize());
                spec.setWindowSlide(conf.getWindowSlide());
                spec.setIncludeCurrentEvent(ObjectUtil.equal(conf.getIncludeCurrentEvent(), 1));
                spec.setTtl(secondsToDuration(conf.getTtlSeconds()));
            }
            result.add(spec);
        }
        return result;
    }

    private List<LookupFeatureSpec> buildLookupFeatures(CompileContext context) {
        List<LookupFeatureSpec> result = new ArrayList<>();
        for (FeatureDefDO featureDef : context.lookupFeatureDefs) {
            FeatureLookupConfDO conf = context.lookupConfMap.get(featureDef.getFeatureCode());
            LookupFeatureSpec spec = new LookupFeatureSpec();
            spec.setCode(featureDef.getFeatureCode());
            spec.setName(featureDef.getFeatureName());
            spec.setType(FeatureType.LOOKUP);
            spec.setValueType(featureDef.getValueType());
            spec.setDescription(featureDef.getDescription());
            if (conf != null) {
                spec.setLookupType(parseEnum(conf.getLookupType(), LookupType.class));
                spec.setKeyExpr(conf.getKeyExpr());
                spec.setSourceRef(conf.getSourceRef());
                spec.setDefaultValue(conf.getDefaultValue());
                spec.setTimeoutMs(conf.getTimeoutMs());
                spec.setCacheTtlSeconds(conf.getCacheTtlSeconds() == null ? null : conf.getCacheTtlSeconds().intValue());
            }
            result.add(spec);
        }
        return result;
    }

    private List<DerivedFeatureSpec> buildDerivedFeatures(CompileContext context) {
        List<DerivedFeatureSpec> result = new ArrayList<>();
        for (FeatureDefDO featureDef : context.derivedFeatureDefs) {
            FeatureDerivedConfDO conf = context.derivedConfMap.get(featureDef.getFeatureCode());
            DerivedFeatureSpec spec = new DerivedFeatureSpec();
            spec.setCode(featureDef.getFeatureCode());
            spec.setName(featureDef.getFeatureName());
            spec.setType(FeatureType.DERIVED);
            spec.setValueType(featureDef.getValueType());
            spec.setDescription(featureDef.getDescription());
            if (conf != null) {
                spec.setEngineType(parseEnum(conf.getEngineType(), EngineType.class));
                spec.setExpr(conf.getExprContent());
                spec.setDependsOn(defaultList(conf.getDependsOnJson()));
            }
            result.add(spec);
        }
        return result;
    }

    private List<RuleSpec> buildRules(CompileContext context) {
        Set<String> availableCodes = context.availableDependencyCodes();
        return context.enabledRules.stream().map(rule -> {
            RuleSpec spec = new RuleSpec();
            spec.setCode(rule.getRuleCode());
            spec.setName(rule.getRuleName());
            spec.setEngineType(parseEnum(rule.getEngineType(), EngineType.class));
            spec.setPriority(rule.getPriority());
            spec.setWhenExpr(rule.getExprContent());
            spec.setDependsOn(extractReferencedCodes(rule.getExprContent(), availableCodes));
            spec.setHitAction(parseEnum(rule.getHitAction(), ActionType.class));
            spec.setRiskScore(rule.getRiskScore());
            spec.setHitReasonTemplate(rule.getHitReasonTemplate());
            spec.setEnabled(true);
            return spec;
        }).toList();
    }

    private PolicySpec buildPolicySpec(CompileContext context) {
        if (context.policy == null) {
            return null;
        }
        PolicySpec spec = new PolicySpec();
        spec.setPolicyCode(context.policy.getPolicyCode());
        spec.setPolicyName(context.policy.getPolicyName());
        spec.setDecisionMode(parseEnum(context.policy.getDecisionMode(), DecisionMode.class));
        spec.setDefaultAction(parseEnum(context.policy.getDefaultAction(), ActionType.class));
        List<PolicyRuleRefDO> orderedRefs = context.policyRuleRefs.stream()
                .sorted(Comparator.comparing(PolicyRuleRefDO::getOrderNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(PolicyRuleRefDO::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        spec.setRuleOrder(orderedRefs.stream().map(PolicyRuleRefDO::getRuleCode).toList());
        spec.setRuleRefs(orderedRefs.stream().map(ref -> {
            PolicyRuleRefSpec ruleRefSpec = new PolicyRuleRefSpec();
            ruleRefSpec.setRuleCode(ref.getRuleCode());
            ruleRefSpec.setOrderNo(ref.getOrderNo());
            ruleRefSpec.setEnabled(!ObjectUtil.equal(ref.getEnabledFlag(), 0));
            ruleRefSpec.setScoreWeight(ref.getScoreWeight());
            ruleRefSpec.setStopOnHit(!ObjectUtil.equal(ref.getStopOnHit(), 0));
            ruleRefSpec.setBranchExpr(ref.getBranchExpr());
            return ruleRefSpec;
        }).toList());
        spec.setScoreBands(context.policyScoreBands.stream()
                .sorted(Comparator.comparing(PolicyScoreBandDO::getBandNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(PolicyScoreBandDO::getId, Comparator.nullsLast(Long::compareTo)))
                .map(item -> {
                    ScoreBandSpec scoreBandSpec = new ScoreBandSpec();
                    scoreBandSpec.setCode(item.getBandNo() == null ? null : "BAND_" + item.getBandNo());
                    scoreBandSpec.setMinScore(item.getMinScore());
                    scoreBandSpec.setMaxScore(item.getMaxScore());
                    scoreBandSpec.setAction(parseEnum(item.getHitAction(), ActionType.class));
                    scoreBandSpec.setReasonTemplate(item.getHitReasonTemplate());
                    return scoreBandSpec;
                }).toList());
        return spec;
    }

    private RuntimeHints buildRuntimeHints(SceneSnapshot snapshot) {
        RuntimeHints hints = new RuntimeHints();
        hints.setRequiredStreamFeatures(defaultList(snapshot.getStreamFeatures()).stream().map(StreamFeatureSpec::getCode).toList());
        hints.setRequiredLookupFeatures(defaultList(snapshot.getLookupFeatures()).stream().map(LookupFeatureSpec::getCode).toList());
        hints.setRequiredDerivedFeatures(defaultList(snapshot.getDerivedFeatures()).stream().map(DerivedFeatureSpec::getCode).toList());
        hints.setMaxRuleExecutionCount(100);
        hints.setAllowGroovy(true);
        hints.setNeedFullDecisionLog(true);
        return hints;
    }

    private SceneReleaseRespVO buildRespVO(SceneReleaseDO release) {
        SceneReleaseRespVO respVO = new SceneReleaseRespVO();
        respVO.setId(release.getId());
        respVO.setSceneCode(release.getSceneCode());
        respVO.setVersionNo(release.getVersionNo());
        respVO.setSnapshotJson(release.getSnapshotJson());
        respVO.setChecksum(release.getChecksum());
        respVO.setPublishStatus(release.getPublishStatus());
        respVO.setValidationStatus(release.getValidationStatus());
        respVO.setValidationReportJson(release.getValidationReportJson());
        respVO.setDependencyDigestJson(release.getDependencyDigestJson());
        respVO.setCompileDurationMs(release.getCompileDurationMs());
        respVO.setCompiledFeatureCount(release.getCompiledFeatureCount());
        respVO.setCompiledRuleCount(release.getCompiledRuleCount());
        respVO.setCompiledPolicyCount(release.getCompiledPolicyCount());
        respVO.setPublishedBy(release.getPublishedBy());
        respVO.setPublishedAt(release.getPublishedAt());
        respVO.setEffectiveFrom(release.getEffectiveFrom());
        respVO.setRollbackFromVersion(release.getRollbackFromVersion());
        respVO.setRemark(release.getRemark());
        respVO.setCreator(release.getCreator());
        respVO.setCreateTime(release.getCreateTime());
        respVO.setUpdater(release.getUpdater());
        respVO.setUpdateTime(release.getUpdateTime());
        return respVO;
    }

    private SceneDO validateSceneExists(String sceneCode) {
        SceneDO scene = sceneMapper.selectOne(SceneDO::getSceneCode, sceneCode);
        if (scene == null) {
            throw ServiceExceptionUtil.exception(SCENE_NOT_EXISTS);
        }
        return scene;
    }

    private SceneReleaseDO validateSceneReleaseExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(SCENE_RELEASE_NOT_EXISTS);
        }
        SceneReleaseDO release = sceneReleaseMapper.selectById(id);
        if (release == null) {
            throw ServiceExceptionUtil.exception(SCENE_RELEASE_NOT_EXISTS);
        }
        return release;
    }

    private List<FeatureDefDO> filterEnabledFeatures(List<FeatureDefDO> featureDefs, String featureType) {
        return defaultList(featureDefs).stream()
                .filter(item -> Objects.equals(featureType, item.getFeatureType()))
                .filter(item -> CommonStatusEnum.isEnable(item.getStatus()))
                .toList();
    }

    private <T, K> Map<K, T> toMap(List<T> list, java.util.function.Function<T, K> keyMapper) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }
        return list.stream().collect(Collectors.toMap(keyMapper, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Set<String> codesOf(Collection<FeatureDefDO> featureDefs) {
        return defaultList(featureDefs).stream().map(FeatureDefDO::getFeatureCode).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> parseCodes(String rawCodes) {
        if (StrUtil.isBlank(rawCodes)) {
            return Collections.emptyList();
        }
        return List.of(rawCodes.split(",")).stream()
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private List<String> resolveSourceEventTypes(String rawSourceEventCodes, Map<String, EventSchemaDO> eventSchemaMap) {
        return parseCodes(rawSourceEventCodes).stream()
                .map(code -> {
                    EventSchemaDO schema = eventSchemaMap.get(code);
                    return schema == null || StrUtil.isBlank(schema.getEventType()) ? code : schema.getEventType().trim().toLowerCase(Locale.ROOT);
                })
                .distinct()
                .toList();
    }

    private <E extends Enum<E>> E parseEnum(String rawValue, Class<E> enumClass) {
        if (StrUtil.isBlank(rawValue)) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String secondsToDuration(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return null;
        }
        return seconds + "s";
    }

    private String resolveRuntimeMode(String accessMode) {
        return "HTTP".equalsIgnoreCase(StrUtil.blankToDefault(accessMode, "")) ? "SYNC_DECISION" : "ASYNC_DECISION";
    }

    private List<String> extractReferencedCodes(String expression, Set<String> candidateCodes) {
        if (StrUtil.isBlank(expression) || CollUtil.isEmpty(candidateCodes)) {
            return Collections.emptyList();
        }
        List<String> orderedCodes = candidateCodes.stream()
                .filter(StrUtil::isNotBlank)
                .sorted(Comparator.comparingInt(String::length).reversed().thenComparing(String::compareTo))
                .toList();
        Set<String> references = new LinkedHashSet<>();
        for (String code : orderedCodes) {
            Pattern pattern = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(code) + "(?![A-Za-z0-9_])");
            if (pattern.matcher(expression).find()) {
                references.add(code);
            }
        }
        return new ArrayList<>(references);
    }

    private Map<String, ListSetDO> buildListPrefixMap(List<ListSetDO> listSets) {
        return defaultList(listSets).stream().collect(Collectors.toMap(
                item -> RiskListRedisUtils.buildRedisKeyPrefix(item.getListCode(), item.getListType(), item.getMatchType()),
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new));
    }

    private Map<String, Object> check(String type, String code, String result, String message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("code", code);
        item.put("result", result);
        item.put("message", message);
        return item;
    }

    private int totalCompiledFeatures(SceneSnapshot snapshot) {
        return CollUtil.size(snapshot.getStreamFeatures())
                + CollUtil.size(snapshot.getLookupFeatures())
                + CollUtil.size(snapshot.getDerivedFeatures());
    }

    private String trimToNull(String text) {
        return StrUtil.emptyToNull(StrUtil.trim(text));
    }

    private <T> List<T> defaultList(Collection<T> list) {
        return list == null ? Collections.emptyList() : new ArrayList<>(list);
    }

    private String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(StrUtil.blankToDefault(value, "").getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 not supported", exception);
        }
    }

    private static class CompileContext {

        private SceneDO scene;

        private List<EventSchemaDO> eventSchemas = Collections.emptyList();

        private Map<String, EventSchemaDO> eventSchemaMap = Collections.emptyMap();

        private EventSchemaDO defaultEventSchema;

        private List<EventFieldDO> eventFields = Collections.emptyList();

        private List<FeatureDefDO> streamFeatureDefs = Collections.emptyList();

        private List<FeatureDefDO> lookupFeatureDefs = Collections.emptyList();

        private List<FeatureDefDO> derivedFeatureDefs = Collections.emptyList();

        private Map<String, FeatureStreamConfDO> streamConfMap = Collections.emptyMap();

        private Map<String, FeatureLookupConfDO> lookupConfMap = Collections.emptyMap();

        private Map<String, FeatureDerivedConfDO> derivedConfMap = Collections.emptyMap();

        private List<RuleDefDO> allRules = Collections.emptyList();

        private Map<String, RuleDefDO> allRuleMap = Collections.emptyMap();

        private List<RuleDefDO> enabledRules = Collections.emptyList();

        private Map<String, RuleDefDO> enabledRuleMap = Collections.emptyMap();

        private PolicyDefDO policy;

        private List<PolicyRuleRefDO> policyRuleRefs = Collections.emptyList();

        private List<PolicyScoreBandDO> policyScoreBands = Collections.emptyList();

        private List<ListSetDO> listSets = Collections.emptyList();

        private Set<String> availableDependencyCodes() {
            LinkedHashSet<String> codes = new LinkedHashSet<>();
            defaultList(eventFields).stream().map(EventFieldDO::getFieldCode).filter(StrUtil::isNotBlank).forEach(codes::add);
            defaultList(streamFeatureDefs).stream().map(FeatureDefDO::getFeatureCode).filter(StrUtil::isNotBlank).forEach(codes::add);
            defaultList(lookupFeatureDefs).stream().map(FeatureDefDO::getFeatureCode).filter(StrUtil::isNotBlank).forEach(codes::add);
            defaultList(derivedFeatureDefs).stream().map(FeatureDefDO::getFeatureCode).filter(StrUtil::isNotBlank).forEach(codes::add);
            return codes;
        }

        private <T> List<T> defaultList(Collection<T> list) {
            return list == null ? Collections.emptyList() : new ArrayList<>(list);
        }
    }

}
