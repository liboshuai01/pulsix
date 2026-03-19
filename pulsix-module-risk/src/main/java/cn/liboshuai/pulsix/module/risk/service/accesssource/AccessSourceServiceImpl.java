package cn.liboshuai.pulsix.module.risk.service.accesssource;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo.AccessSourcePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo.AccessSourceSaveReqVO;
import cn.liboshuai.pulsix.module.risk.convert.accesssource.AccessSourceConvert;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.AccessSourceDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.EventAccessBindingDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accesssource.AccessSourceMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accesssource.EventAccessBindingMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_SOURCE_ALLOWED_SCENE_CONFLICT;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_SOURCE_ALLOWED_SCENE_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_SOURCE_ALLOWED_SCENE_REQUIRED;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_SOURCE_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_SOURCE_CODE_IMMUTABLE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_SOURCE_DELETE_DENIED;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_SOURCE_DELETE_ENABLED_DENIED;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.ACCESS_SOURCE_NOT_EXISTS;

@Service
@Validated
public class AccessSourceServiceImpl implements AccessSourceService {

    private static final String DELETE_BLOCKED_REASON_ENABLED = "当前为启用状态，请先停用后再删除";
    private static final String DELETE_BLOCKED_REASON_ACCESS_MAPPING = "当前存在关联接入映射，无法删除";

    @Resource
    private AccessSourceMapper accessSourceMapper;
    @Resource
    private EventAccessBindingMapper eventAccessBindingMapper;
    @Resource
    private SceneMapper sceneMapper;

    @Override
    public Long createAccessSource(AccessSourceSaveReqVO createReqVO) {
        validateSourceCodeUnique(null, createReqVO.getSourceCode());
        List<String> allowedSceneCodes = normalizeStringList(createReqVO.getAllowedSceneCodes());
        validateAllowedScenes(allowedSceneCodes);

        AccessSourceDO accessSource = AccessSourceConvert.INSTANCE.convert(createReqVO);
        accessSource.setAllowedSceneCodes(allowedSceneCodes);
        accessSource.setIpWhitelist(normalizeStringList(createReqVO.getIpWhitelist()));
        accessSource.setStatus(CommonStatusEnum.DISABLE.getStatus());
        accessSourceMapper.insert(accessSource);
        return accessSource.getId();
    }

    @Override
    public void updateAccessSource(AccessSourceSaveReqVO updateReqVO) {
        AccessSourceDO accessSource = validateAccessSourceExists(updateReqVO.getId());
        validateSourceCodeImmutable(accessSource, updateReqVO.getSourceCode());
        validateSourceCodeUnique(updateReqVO.getId(), updateReqVO.getSourceCode());
        List<String> allowedSceneCodes = normalizeStringList(updateReqVO.getAllowedSceneCodes());
        validateAllowedScenes(allowedSceneCodes);
        validateBoundSceneCompatibility(accessSource.getSourceCode(), allowedSceneCodes);

        AccessSourceDO updateObj = AccessSourceConvert.INSTANCE.convert(updateReqVO);
        updateObj.setAllowedSceneCodes(allowedSceneCodes);
        updateObj.setIpWhitelist(normalizeStringList(updateReqVO.getIpWhitelist()));
        updateObj.setStatus(accessSource.getStatus());
        accessSourceMapper.updateById(updateObj);
    }

    @Override
    public void updateAccessSourceStatus(Long id, Integer status) {
        validateAccessSourceExists(id);

        AccessSourceDO updateObj = new AccessSourceDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        accessSourceMapper.updateById(updateObj);
    }

    @Override
    public void deleteAccessSource(Long id) {
        AccessSourceDO accessSource = validateAccessSourceExists(id);
        if (ObjectUtil.equal(accessSource.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
            throw exception(ACCESS_SOURCE_DELETE_ENABLED_DENIED);
        }
        if (eventAccessBindingMapper.selectCountBySourceCode(accessSource.getSourceCode()) > 0) {
            throw exception(ACCESS_SOURCE_DELETE_DENIED, accessSource.getSourceCode());
        }
        accessSourceMapper.deleteById(id);
    }

    @Override
    public AccessSourceDO getAccessSource(Long id) {
        return accessSourceMapper.selectById(id);
    }

    @Override
    public PageResult<AccessSourceDO> getAccessSourcePage(AccessSourcePageReqVO pageReqVO) {
        return accessSourceMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AccessSourceDO> getSimpleAccessSourceList(String sceneCode) {
        return accessSourceMapper.selectEnabledListBySceneCode(sceneCode);
    }

    @Override
    public String getDeleteBlockedReason(AccessSourceDO accessSource) {
        if (accessSource == null) {
            return null;
        }
        if (ObjectUtil.equal(accessSource.getStatus(), CommonStatusEnum.ENABLE.getStatus())) {
            return DELETE_BLOCKED_REASON_ENABLED;
        }
        if (eventAccessBindingMapper.selectCountBySourceCode(accessSource.getSourceCode()) > 0) {
            return DELETE_BLOCKED_REASON_ACCESS_MAPPING;
        }
        return null;
    }

    @Override
    public Map<String, List<AccessSourceDO>> getBindingSourceMap(Collection<String> eventCodes) {
        if (eventCodes == null || eventCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        List<EventAccessBindingDO> bindings = eventAccessBindingMapper.selectListByEventCodes(eventCodes);
        if (bindings.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> sourceCodes = new LinkedHashSet<>();
        for (EventAccessBindingDO binding : bindings) {
            sourceCodes.add(binding.getSourceCode());
        }
        Map<String, AccessSourceDO> sourceMap = new LinkedHashMap<>();
        for (AccessSourceDO source : accessSourceMapper.selectListBySourceCodes(sourceCodes)) {
            sourceMap.put(source.getSourceCode(), source);
        }

        Map<String, List<AccessSourceDO>> result = new LinkedHashMap<>();
        for (EventAccessBindingDO binding : bindings) {
            AccessSourceDO source = sourceMap.get(binding.getSourceCode());
            if (source == null) {
                continue;
            }
            result.computeIfAbsent(binding.getEventCode(), key -> new ArrayList<>()).add(source);
        }
        return result;
    }

    private AccessSourceDO validateAccessSourceExists(Long id) {
        if (id == null) {
            throw exception(ACCESS_SOURCE_NOT_EXISTS);
        }
        AccessSourceDO accessSource = accessSourceMapper.selectById(id);
        if (accessSource == null) {
            throw exception(ACCESS_SOURCE_NOT_EXISTS);
        }
        return accessSource;
    }

    private void validateSourceCodeUnique(Long id, String sourceCode) {
        AccessSourceDO accessSource = accessSourceMapper.selectBySourceCode(sourceCode);
        if (accessSource == null) {
            return;
        }
        if (id == null || !ObjectUtil.equal(accessSource.getId(), id)) {
            throw exception(ACCESS_SOURCE_CODE_DUPLICATE, sourceCode);
        }
    }

    private void validateSourceCodeImmutable(AccessSourceDO accessSource, String sourceCode) {
        if (!ObjectUtil.equal(accessSource.getSourceCode(), sourceCode)) {
            throw exception(ACCESS_SOURCE_CODE_IMMUTABLE);
        }
    }

    private void validateAllowedScenes(List<String> allowedSceneCodes) {
        if (allowedSceneCodes == null || allowedSceneCodes.isEmpty()) {
            throw exception(ACCESS_SOURCE_ALLOWED_SCENE_REQUIRED);
        }
        for (String sceneCode : new LinkedHashSet<>(allowedSceneCodes)) {
            SceneDO scene = sceneMapper.selectBySceneCode(sceneCode);
            if (scene == null) {
                throw exception(ACCESS_SOURCE_ALLOWED_SCENE_INVALID, sceneCode);
            }
        }
    }

    private void validateBoundSceneCompatibility(String sourceCode, List<String> allowedSceneCodes) {
        Set<String> allowedSceneCodeSet = new LinkedHashSet<>(allowedSceneCodes);
        for (String sceneCode : eventAccessBindingMapper.selectBoundSceneCodesBySourceCode(sourceCode)) {
            if (!allowedSceneCodeSet.contains(sceneCode)) {
                throw exception(ACCESS_SOURCE_ALLOWED_SCENE_CONFLICT, sourceCode, sceneCode);
            }
        }
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> normalized = new ArrayList<>();
        Set<String> deduplicated = new LinkedHashSet<>();
        for (String value : values) {
            String trimmed = StrUtil.trim(value);
            if (StrUtil.isBlank(trimmed) || !deduplicated.add(trimmed)) {
                continue;
            }
            normalized.add(trimmed);
        }
        return normalized;
    }

}
