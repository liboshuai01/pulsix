package cn.liboshuai.pulsix.module.risk.service.featurestream;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo.EntityTypeRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo.FeatureStreamPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo.FeatureStreamRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo.FeatureStreamSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.entitytype.EntityTypeDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureStreamConfDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventschema.EventSchemaDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.entitytype.EntityTypeMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventschema.EventSchemaMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.feature.FeatureDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.feature.FeatureStreamConfMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureAggTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureWindowTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ENTITY_TYPE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SCHEMA_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.FEATURE_STREAM_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.FEATURE_STREAM_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_NOT_EXISTS;

@Service
public class FeatureStreamServiceImpl implements FeatureStreamService {

    @Resource
    private FeatureDefMapper featureDefMapper;

    @Resource
    private FeatureStreamConfMapper featureStreamConfMapper;

    @Resource
    private EntityTypeMapper entityTypeMapper;

    @Resource
    private SceneMapper sceneMapper;

    @Resource
    private EventSchemaMapper eventSchemaMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFeatureStream(FeatureStreamSaveReqVO createReqVO) {
        validateSceneExists(createReqVO.getSceneCode());
        validateFeatureCodeUnique(createReqVO.getSceneCode(), createReqVO.getFeatureCode(), null);
        EntityTypeDO entityType = validateEntityTypeExists(createReqVO.getEntityType());
        List<String> sourceEventCodes = normalizeSourceEventCodes(createReqVO.getSourceEventCodes());
        validateEventSchemasExist(createReqVO.getSceneCode(), sourceEventCodes);

        FeatureDefDO featureDef = new FeatureDefDO();
        featureDef.setSceneCode(createReqVO.getSceneCode().trim());
        featureDef.setFeatureCode(createReqVO.getFeatureCode().trim());
        featureDef.setFeatureName(createReqVO.getFeatureName().trim());
        featureDef.setFeatureType(RiskFeatureTypeEnum.STREAM.getType());
        featureDef.setEntityType(entityType.getEntityType());
        featureDef.setEventCode(CollUtil.getFirst(sourceEventCodes));
        featureDef.setValueType(createReqVO.getValueType().trim());
        featureDef.setStatus(createReqVO.getStatus());
        featureDef.setVersion(1);
        featureDef.setDescription(trimToNull(createReqVO.getDescription()));
        featureDefMapper.insert(featureDef);

        FeatureStreamConfDO conf = buildFeatureStreamConf(createReqVO, featureDef.getSceneCode(), featureDef.getFeatureCode(), entityType, sourceEventCodes);
        featureStreamConfMapper.insert(conf);
        return featureDef.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFeatureStream(FeatureStreamSaveReqVO updateReqVO) {
        FeatureDefDO featureDef = validateFeatureStreamExists(updateReqVO.getId());
        FeatureStreamConfDO conf = validateFeatureStreamConfExists(featureDef.getSceneCode(), featureDef.getFeatureCode());
        EntityTypeDO entityType = validateEntityTypeExists(updateReqVO.getEntityType());
        List<String> sourceEventCodes = normalizeSourceEventCodes(updateReqVO.getSourceEventCodes());
        validateEventSchemasExist(featureDef.getSceneCode(), sourceEventCodes);

        FeatureDefDO updateFeatureDef = new FeatureDefDO();
        updateFeatureDef.setId(featureDef.getId());
        updateFeatureDef.setSceneCode(featureDef.getSceneCode());
        updateFeatureDef.setFeatureCode(featureDef.getFeatureCode());
        updateFeatureDef.setFeatureName(updateReqVO.getFeatureName().trim());
        updateFeatureDef.setFeatureType(RiskFeatureTypeEnum.STREAM.getType());
        updateFeatureDef.setEntityType(entityType.getEntityType());
        updateFeatureDef.setEventCode(CollUtil.getFirst(sourceEventCodes));
        updateFeatureDef.setValueType(updateReqVO.getValueType().trim());
        updateFeatureDef.setStatus(updateReqVO.getStatus());
        updateFeatureDef.setVersion(featureDef.getVersion() == null ? 1 : featureDef.getVersion() + 1);
        updateFeatureDef.setDescription(trimToNull(updateReqVO.getDescription()));
        featureDefMapper.updateById(updateFeatureDef);

        FeatureStreamConfDO updateConf = buildFeatureStreamConf(updateReqVO, featureDef.getSceneCode(), featureDef.getFeatureCode(), entityType, sourceEventCodes);
        updateConf.setId(conf.getId());
        featureStreamConfMapper.updateById(updateConf);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFeatureStream(Long id) {
        FeatureDefDO featureDef = validateFeatureStreamExists(id);
        FeatureStreamConfDO conf = featureStreamConfMapper.selectBySceneAndFeatureCode(featureDef.getSceneCode(), featureDef.getFeatureCode());
        if (conf != null) {
            featureStreamConfMapper.deleteById(conf.getId());
        }
        featureDefMapper.deleteById(id);
    }

    @Override
    public FeatureStreamRespVO getFeatureStream(Long id) {
        FeatureDefDO featureDef = validateFeatureStreamExists(id);
        FeatureStreamConfDO conf = validateFeatureStreamConfExists(featureDef.getSceneCode(), featureDef.getFeatureCode());
        EntityTypeDO entityType = entityTypeMapper.selectOne(EntityTypeDO::getEntityType, featureDef.getEntityType());
        return buildRespVO(featureDef, conf, entityType);
    }

    @Override
    public PageResult<FeatureStreamRespVO> getFeatureStreamPage(FeatureStreamPageReqVO pageReqVO) {
        PageResult<FeatureDefDO> pageResult = featureDefMapper.selectStreamFeaturePage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        Set<String> sceneCodes = pageResult.getList().stream().map(FeatureDefDO::getSceneCode).collect(Collectors.toSet());
        Set<String> featureCodes = pageResult.getList().stream().map(FeatureDefDO::getFeatureCode).collect(Collectors.toSet());
        Set<String> entityTypes = pageResult.getList().stream().map(FeatureDefDO::getEntityType)
                .filter(StrUtil::isNotBlank).collect(Collectors.toSet());

        Map<String, FeatureStreamConfDO> confMap = featureStreamConfMapper.selectListBySceneCodesAndFeatureCodes(sceneCodes, featureCodes)
                .stream().collect(Collectors.toMap(this::buildPairKey, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, EntityTypeDO> entityTypeMap = entityTypeMapper.selectListByEntityTypes(entityTypes)
                .stream().collect(Collectors.toMap(EntityTypeDO::getEntityType, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<FeatureStreamRespVO> respList = pageResult.getList().stream()
                .map(item -> buildRespVO(item, confMap.get(buildPairKey(item.getSceneCode(), item.getFeatureCode())), entityTypeMap.get(item.getEntityType())))
                .toList();
        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public List<EntityTypeRespVO> getEntityTypeList() {
        return BeanUtils.toBean(entityTypeMapper.selectAllList(), EntityTypeRespVO.class);
    }

    private SceneDO validateSceneExists(String sceneCode) {
        SceneDO scene = sceneMapper.selectOne(SceneDO::getSceneCode, sceneCode);
        if (scene == null) {
            throw ServiceExceptionUtil.exception(SCENE_NOT_EXISTS);
        }
        return scene;
    }

    private EntityTypeDO validateEntityTypeExists(String entityType) {
        EntityTypeDO entityTypeDO = entityTypeMapper.selectOne(EntityTypeDO::getEntityType, entityType);
        if (entityTypeDO == null) {
            throw ServiceExceptionUtil.exception(ENTITY_TYPE_NOT_EXISTS);
        }
        return entityTypeDO;
    }

    private FeatureDefDO validateFeatureStreamExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(FEATURE_STREAM_NOT_EXISTS);
        }
        FeatureDefDO featureDef = featureDefMapper.selectById(id);
        if (featureDef == null || !ObjectUtil.equal(featureDef.getFeatureType(), RiskFeatureTypeEnum.STREAM.getType())) {
            throw ServiceExceptionUtil.exception(FEATURE_STREAM_NOT_EXISTS);
        }
        return featureDef;
    }

    private FeatureStreamConfDO validateFeatureStreamConfExists(String sceneCode, String featureCode) {
        FeatureStreamConfDO conf = featureStreamConfMapper.selectBySceneAndFeatureCode(sceneCode, featureCode);
        if (conf == null) {
            throw ServiceExceptionUtil.exception(FEATURE_STREAM_NOT_EXISTS);
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
            throw ServiceExceptionUtil.exception(FEATURE_STREAM_CODE_DUPLICATE);
        }
    }

    private void validateEventSchemasExist(String sceneCode, List<String> sourceEventCodes) {
        List<EventSchemaDO> eventSchemaList = eventSchemaMapper.selectList(new LambdaQueryWrapperX<EventSchemaDO>()
                .eq(EventSchemaDO::getSceneCode, sceneCode)
                .inIfPresent(EventSchemaDO::getEventCode, sourceEventCodes));
        if (eventSchemaList.size() != sourceEventCodes.size()) {
            throw ServiceExceptionUtil.exception(EVENT_SCHEMA_NOT_EXISTS);
        }
    }

    private FeatureStreamConfDO buildFeatureStreamConf(FeatureStreamSaveReqVO reqVO, String sceneCode, String featureCode,
                                                       EntityTypeDO entityType, List<String> sourceEventCodes) {
        String windowType = normalizeWindowType(reqVO);
        FeatureStreamConfDO conf = new FeatureStreamConfDO();
        conf.setSceneCode(sceneCode);
        conf.setFeatureCode(featureCode);
        conf.setSourceEventCodes(String.join(",", sourceEventCodes));
        conf.setEntityKeyExpr(resolveEntityKeyExpr(reqVO.getEntityKeyExpr(), entityType));
        conf.setAggType(reqVO.getAggType().trim());
        conf.setValueExpr(trimToNull(reqVO.getValueExpr()));
        conf.setFilterExpr(trimToNull(reqVO.getFilterExpr()));
        conf.setWindowType(windowType);
        conf.setWindowSize(resolveWindowSize(reqVO, windowType));
        conf.setWindowSlide(resolveWindowSlide(reqVO, windowType));
        conf.setIncludeCurrentEvent(reqVO.getIncludeCurrentEvent());
        conf.setTtlSeconds(reqVO.getTtlSeconds());
        conf.setStateHintJson(reqVO.getStateHintJson());
        conf.setStatus(reqVO.getStatus());
        return conf;
    }

    private FeatureStreamRespVO buildRespVO(FeatureDefDO featureDef, FeatureStreamConfDO conf, EntityTypeDO entityType) {
        FeatureStreamRespVO respVO = new FeatureStreamRespVO();
        respVO.setId(featureDef.getId());
        respVO.setSceneCode(featureDef.getSceneCode());
        respVO.setFeatureCode(featureDef.getFeatureCode());
        respVO.setFeatureName(featureDef.getFeatureName());
        respVO.setFeatureType(featureDef.getFeatureType());
        respVO.setEntityType(featureDef.getEntityType());
        respVO.setEntityName(entityType == null ? null : entityType.getEntityName());
        respVO.setEntityKeyFieldName(entityType == null ? null : entityType.getKeyFieldName());
        respVO.setEventCode(featureDef.getEventCode());
        respVO.setValueType(featureDef.getValueType());
        respVO.setStatus(featureDef.getStatus() != null ? featureDef.getStatus() : conf == null ? null : conf.getStatus());
        respVO.setVersion(featureDef.getVersion());
        respVO.setDescription(featureDef.getDescription());
        respVO.setCreator(featureDef.getCreator());
        respVO.setCreateTime(featureDef.getCreateTime());
        respVO.setUpdater(featureDef.getUpdater());
        respVO.setUpdateTime(featureDef.getUpdateTime());
        if (conf != null) {
            respVO.setSourceEventCodes(splitSourceEventCodes(conf.getSourceEventCodes()));
            respVO.setEntityKeyExpr(conf.getEntityKeyExpr());
            respVO.setAggType(conf.getAggType());
            respVO.setValueExpr(conf.getValueExpr());
            respVO.setFilterExpr(conf.getFilterExpr());
            respVO.setWindowType(conf.getWindowType());
            respVO.setWindowSize(conf.getWindowSize());
            respVO.setWindowSlide(conf.getWindowSlide());
            respVO.setIncludeCurrentEvent(conf.getIncludeCurrentEvent());
            respVO.setTtlSeconds(conf.getTtlSeconds());
            respVO.setStateHintJson(conf.getStateHintJson());
        } else {
            respVO.setSourceEventCodes(Collections.emptyList());
        }
        return respVO;
    }

    private List<String> normalizeSourceEventCodes(List<String> sourceEventCodes) {
        List<String> normalized = sourceEventCodes.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(normalized)) {
            throw ServiceExceptionUtil.exception(EVENT_SCHEMA_NOT_EXISTS);
        }
        return normalized;
    }

    private List<String> splitSourceEventCodes(String sourceEventCodes) {
        if (StrUtil.isBlank(sourceEventCodes)) {
            return Collections.emptyList();
        }
        return StrUtil.splitTrim(sourceEventCodes, ',');
    }

    private String resolveEntityKeyExpr(String entityKeyExpr, EntityTypeDO entityType) {
        String resolved = trimToNull(entityKeyExpr);
        if (resolved != null) {
            return resolved;
        }
        return trimToNull(entityType == null ? null : entityType.getKeyFieldName());
    }

    private String normalizeWindowType(FeatureStreamSaveReqVO reqVO) {
        if (ObjectUtil.equal(reqVO.getAggType(), RiskFeatureAggTypeEnum.LATEST.getType())) {
            return RiskFeatureWindowTypeEnum.NONE.getType();
        }
        return reqVO.getWindowType().trim();
    }

    private String resolveWindowSize(FeatureStreamSaveReqVO reqVO, String windowType) {
        if (ObjectUtil.equal(windowType, RiskFeatureWindowTypeEnum.NONE.getType())) {
            return StrUtil.blankToDefault(trimToNull(reqVO.getWindowSize()), "1m");
        }
        return reqVO.getWindowSize().trim();
    }

    private String resolveWindowSlide(FeatureStreamSaveReqVO reqVO, String windowType) {
        if (!ObjectUtil.equal(windowType, RiskFeatureWindowTypeEnum.SLIDING.getType())) {
            return null;
        }
        return trimToNull(reqVO.getWindowSlide());
    }

    private String buildPairKey(FeatureDefDO featureDef) {
        return buildPairKey(featureDef.getSceneCode(), featureDef.getFeatureCode());
    }

    private String buildPairKey(FeatureStreamConfDO conf) {
        return buildPairKey(conf.getSceneCode(), conf.getFeatureCode());
    }

    private String buildPairKey(String sceneCode, String featureCode) {
        return sceneCode + "::" + featureCode;
    }

    private String trimToNull(String text) {
        return StrUtil.emptyToNull(StrUtil.trim(text));
    }

}
