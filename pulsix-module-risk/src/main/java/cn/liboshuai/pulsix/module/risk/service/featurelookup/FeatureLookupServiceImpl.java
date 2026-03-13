package cn.liboshuai.pulsix.module.risk.service.featurelookup;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurelookup.vo.FeatureLookupPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurelookup.vo.FeatureLookupRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurelookup.vo.FeatureLookupSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureLookupConfDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.feature.FeatureDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.feature.FeatureLookupConfMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureTypeEnum;
import cn.liboshuai.pulsix.module.risk.service.auditlog.AuditLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.FEATURE_LOOKUP_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.FEATURE_LOOKUP_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_CREATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_DELETE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_UPDATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.BIZ_TYPE_FEATURE;

@Service
public class FeatureLookupServiceImpl implements FeatureLookupService {

    @Resource
    private FeatureDefMapper featureDefMapper;

    @Resource
    private FeatureLookupConfMapper featureLookupConfMapper;

    @Resource
    private SceneMapper sceneMapper;

    @Resource
    private AuditLogService auditLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFeatureLookup(FeatureLookupSaveReqVO createReqVO) {
        validateSceneExists(createReqVO.getSceneCode());
        validateFeatureCodeUnique(createReqVO.getSceneCode(), createReqVO.getFeatureCode(), null);

        FeatureDefDO featureDef = new FeatureDefDO();
        featureDef.setSceneCode(createReqVO.getSceneCode().trim());
        featureDef.setFeatureCode(createReqVO.getFeatureCode().trim());
        featureDef.setFeatureName(createReqVO.getFeatureName().trim());
        featureDef.setFeatureType(RiskFeatureTypeEnum.LOOKUP.getType());
        featureDef.setEntityType(null);
        featureDef.setEventCode(null);
        featureDef.setValueType(createReqVO.getValueType().trim());
        featureDef.setStatus(createReqVO.getStatus());
        featureDef.setVersion(1);
        featureDef.setDescription(trimToNull(createReqVO.getDescription()));
        featureDefMapper.insert(featureDef);

        FeatureLookupConfDO conf = buildFeatureLookupConf(createReqVO, featureDef.getSceneCode(), featureDef.getFeatureCode());
        featureLookupConfMapper.insert(conf);
        auditLogService.createAuditLog(featureDef.getSceneCode(), BIZ_TYPE_FEATURE, featureDef.getFeatureCode(), ACTION_CREATE,
                null, getFeatureLookup(featureDef.getId()), "新增查表特征 " + featureDef.getFeatureCode());
        return featureDef.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFeatureLookup(FeatureLookupSaveReqVO updateReqVO) {
        FeatureDefDO featureDef = validateFeatureLookupExists(updateReqVO.getId());
        FeatureLookupRespVO beforePayload = getFeatureLookup(featureDef.getId());
        FeatureLookupConfDO conf = validateFeatureLookupConfExists(featureDef.getSceneCode(), featureDef.getFeatureCode());

        FeatureDefDO updateFeatureDef = new FeatureDefDO();
        updateFeatureDef.setId(featureDef.getId());
        updateFeatureDef.setSceneCode(featureDef.getSceneCode());
        updateFeatureDef.setFeatureCode(featureDef.getFeatureCode());
        updateFeatureDef.setFeatureName(updateReqVO.getFeatureName().trim());
        updateFeatureDef.setFeatureType(RiskFeatureTypeEnum.LOOKUP.getType());
        updateFeatureDef.setValueType(updateReqVO.getValueType().trim());
        updateFeatureDef.setStatus(updateReqVO.getStatus());
        updateFeatureDef.setVersion(featureDef.getVersion() == null ? 1 : featureDef.getVersion() + 1);
        updateFeatureDef.setDescription(trimToNull(updateReqVO.getDescription()));
        featureDefMapper.updateById(updateFeatureDef);

        FeatureLookupConfDO updateConf = buildFeatureLookupConf(updateReqVO, featureDef.getSceneCode(), featureDef.getFeatureCode());
        updateConf.setId(conf.getId());
        featureLookupConfMapper.updateById(updateConf);
        auditLogService.createAuditLog(featureDef.getSceneCode(), BIZ_TYPE_FEATURE, featureDef.getFeatureCode(), ACTION_UPDATE,
                beforePayload, getFeatureLookup(featureDef.getId()), "修改查表特征 " + featureDef.getFeatureCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFeatureLookup(Long id) {
        FeatureLookupRespVO beforePayload = getFeatureLookup(id);
        FeatureDefDO featureDef = validateFeatureLookupExists(id);
        FeatureLookupConfDO conf = featureLookupConfMapper.selectBySceneAndFeatureCode(featureDef.getSceneCode(), featureDef.getFeatureCode());
        if (conf != null) {
            featureLookupConfMapper.deleteById(conf.getId());
        }
        featureDefMapper.deleteById(id);
        auditLogService.createAuditLog(featureDef.getSceneCode(), BIZ_TYPE_FEATURE, featureDef.getFeatureCode(), ACTION_DELETE,
                beforePayload, null, "删除查表特征 " + featureDef.getFeatureCode());
    }

    @Override
    public FeatureLookupRespVO getFeatureLookup(Long id) {
        FeatureDefDO featureDef = validateFeatureLookupExists(id);
        FeatureLookupConfDO conf = validateFeatureLookupConfExists(featureDef.getSceneCode(), featureDef.getFeatureCode());
        return buildRespVO(featureDef, conf);
    }

    @Override
    public PageResult<FeatureLookupRespVO> getFeatureLookupPage(FeatureLookupPageReqVO pageReqVO) {
        PageResult<FeatureDefDO> pageResult = featureDefMapper.selectLookupFeaturePage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        Set<String> sceneCodes = pageResult.getList().stream().map(FeatureDefDO::getSceneCode).collect(Collectors.toSet());
        Set<String> featureCodes = pageResult.getList().stream().map(FeatureDefDO::getFeatureCode).collect(Collectors.toSet());
        Map<String, FeatureLookupConfDO> confMap = featureLookupConfMapper.selectListBySceneCodesAndFeatureCodes(sceneCodes, featureCodes)
                .stream().collect(Collectors.toMap(this::buildPairKey, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<FeatureLookupRespVO> respList = pageResult.getList().stream()
                .map(item -> buildRespVO(item, confMap.get(buildPairKey(item.getSceneCode(), item.getFeatureCode()))))
                .toList();
        return new PageResult<>(respList, pageResult.getTotal());
    }

    private SceneDO validateSceneExists(String sceneCode) {
        SceneDO scene = sceneMapper.selectOne(SceneDO::getSceneCode, sceneCode);
        if (scene == null) {
            throw ServiceExceptionUtil.exception(SCENE_NOT_EXISTS);
        }
        return scene;
    }

    private FeatureDefDO validateFeatureLookupExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(FEATURE_LOOKUP_NOT_EXISTS);
        }
        FeatureDefDO featureDef = featureDefMapper.selectById(id);
        if (featureDef == null || !ObjectUtil.equal(featureDef.getFeatureType(), RiskFeatureTypeEnum.LOOKUP.getType())) {
            throw ServiceExceptionUtil.exception(FEATURE_LOOKUP_NOT_EXISTS);
        }
        return featureDef;
    }

    private FeatureLookupConfDO validateFeatureLookupConfExists(String sceneCode, String featureCode) {
        FeatureLookupConfDO conf = featureLookupConfMapper.selectBySceneAndFeatureCode(sceneCode, featureCode);
        if (conf == null) {
            throw ServiceExceptionUtil.exception(FEATURE_LOOKUP_NOT_EXISTS);
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
            throw ServiceExceptionUtil.exception(FEATURE_LOOKUP_CODE_DUPLICATE);
        }
    }

    private FeatureLookupConfDO buildFeatureLookupConf(FeatureLookupSaveReqVO reqVO, String sceneCode, String featureCode) {
        FeatureLookupConfDO conf = new FeatureLookupConfDO();
        conf.setSceneCode(sceneCode);
        conf.setFeatureCode(featureCode);
        conf.setLookupType(reqVO.getLookupType().trim());
        conf.setKeyExpr(reqVO.getKeyExpr().trim());
        conf.setSourceRef(reqVO.getSourceRef().trim());
        conf.setDefaultValue(trimToNull(reqVO.getDefaultValue()));
        conf.setCacheTtlSeconds(reqVO.getCacheTtlSeconds());
        conf.setTimeoutMs(reqVO.getTimeoutMs());
        conf.setExtraJson(reqVO.getExtraJson());
        conf.setStatus(reqVO.getStatus());
        return conf;
    }

    private FeatureLookupRespVO buildRespVO(FeatureDefDO featureDef, FeatureLookupConfDO conf) {
        FeatureLookupRespVO respVO = new FeatureLookupRespVO();
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
            respVO.setLookupType(conf.getLookupType());
            respVO.setKeyExpr(conf.getKeyExpr());
            respVO.setSourceRef(conf.getSourceRef());
            respVO.setDefaultValue(conf.getDefaultValue());
            respVO.setCacheTtlSeconds(conf.getCacheTtlSeconds());
            respVO.setTimeoutMs(conf.getTimeoutMs());
            respVO.setExtraJson(conf.getExtraJson());
        }
        return respVO;
    }

    private String buildPairKey(FeatureLookupConfDO conf) {
        return buildPairKey(conf.getSceneCode(), conf.getFeatureCode());
    }

    private String buildPairKey(String sceneCode, String featureCode) {
        return sceneCode + "::" + featureCode;
    }

    private String trimToNull(String text) {
        return StrUtil.emptyToNull(StrUtil.trim(text));
    }

}
