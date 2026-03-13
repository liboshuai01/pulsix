package cn.liboshuai.pulsix.module.risk.service.eventschema;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventschema.vo.EventSchemaPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventschema.vo.EventSchemaSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventfield.EventFieldDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventsample.EventSampleDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventschema.EventSchemaDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureStreamConfDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingestmapping.IngestMappingDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventfield.EventFieldMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventsample.EventSampleMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventschema.EventSchemaMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.feature.FeatureStreamConfMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.ingestmapping.IngestMappingMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import cn.liboshuai.pulsix.module.risk.service.auditlog.AuditLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SCHEMA_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SCHEMA_DELETE_INVALID;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SCHEMA_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_CREATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_DELETE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_UPDATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.BIZ_TYPE_EVENT_SCHEMA;

@Service
public class EventSchemaServiceImpl implements EventSchemaService {

    @Resource
    private EventSchemaMapper eventSchemaMapper;

    @Resource
    private SceneMapper sceneMapper;

    @Resource
    private EventFieldMapper eventFieldMapper;

    @Resource
    private EventSampleMapper eventSampleMapper;

    @Resource
    private IngestMappingMapper ingestMappingMapper;

    @Resource
    private FeatureStreamConfMapper featureStreamConfMapper;

    @Resource
    private AuditLogService auditLogService;

    @Override
    public Long createEventSchema(EventSchemaSaveReqVO createReqVO) {
        validateSceneExists(createReqVO.getSceneCode());
        validateEventSchemaCodeUnique(createReqVO.getSceneCode(), createReqVO.getEventCode(), null);
        EventSchemaDO eventSchema = BeanUtils.toBean(createReqVO, EventSchemaDO.class);
        eventSchema.setVersion(1);
        eventSchemaMapper.insert(eventSchema);
        auditLogService.createAuditLog(eventSchema.getSceneCode(), BIZ_TYPE_EVENT_SCHEMA,
                buildEventSchemaBizCode(eventSchema.getSceneCode(), eventSchema.getEventCode()), ACTION_CREATE,
                null, eventSchemaMapper.selectById(eventSchema.getId()), "新增事件定义 " + eventSchema.getEventCode());
        return eventSchema.getId();
    }

    @Override
    public void updateEventSchema(EventSchemaSaveReqVO updateReqVO) {
        EventSchemaDO eventSchema = validateEventSchemaExists(updateReqVO.getId());
        EventSchemaDO updateObj = BeanUtils.toBean(updateReqVO, EventSchemaDO.class);
        updateObj.setSceneCode(eventSchema.getSceneCode());
        updateObj.setEventCode(eventSchema.getEventCode());
        updateObj.setVersion(eventSchema.getVersion() == null ? 1 : eventSchema.getVersion() + 1);
        eventSchemaMapper.updateById(updateObj);
        auditLogService.createAuditLog(eventSchema.getSceneCode(), BIZ_TYPE_EVENT_SCHEMA,
                buildEventSchemaBizCode(eventSchema.getSceneCode(), eventSchema.getEventCode()), ACTION_UPDATE,
                eventSchema, eventSchemaMapper.selectById(eventSchema.getId()), "修改事件定义 " + eventSchema.getEventCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEventSchema(Long id) {
        EventSchemaDO eventSchema = validateEventSchemaExists(id);
        validateEventSchemaCanDelete(eventSchema);
        eventSchemaMapper.deleteById(id);
        auditLogService.createAuditLog(eventSchema.getSceneCode(), BIZ_TYPE_EVENT_SCHEMA,
                buildEventSchemaBizCode(eventSchema.getSceneCode(), eventSchema.getEventCode()), ACTION_DELETE,
                eventSchema, null, "删除事件定义 " + eventSchema.getEventCode());
    }

    @Override
    public EventSchemaDO getEventSchema(Long id) {
        return eventSchemaMapper.selectById(id);
    }

    @Override
    public PageResult<EventSchemaDO> getEventSchemaPage(EventSchemaPageReqVO pageReqVO) {
        return eventSchemaMapper.selectPage(pageReqVO);
    }

    private EventSchemaDO validateEventSchemaExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(EVENT_SCHEMA_NOT_EXISTS);
        }
        EventSchemaDO eventSchema = eventSchemaMapper.selectById(id);
        if (eventSchema == null) {
            throw ServiceExceptionUtil.exception(EVENT_SCHEMA_NOT_EXISTS);
        }
        return eventSchema;
    }

    private void validateSceneExists(String sceneCode) {
        SceneDO scene = sceneMapper.selectOne(SceneDO::getSceneCode, sceneCode);
        if (scene == null) {
            throw ServiceExceptionUtil.exception(SCENE_NOT_EXISTS);
        }
    }

    private void validateEventSchemaCodeUnique(String sceneCode, String eventCode, Long id) {
        EventSchemaDO eventSchema = eventSchemaMapper.selectOne(EventSchemaDO::getSceneCode, sceneCode,
                EventSchemaDO::getEventCode, eventCode);
        if (eventSchema == null) {
            return;
        }
        if (id == null || !id.equals(eventSchema.getId())) {
            throw ServiceExceptionUtil.exception(EVENT_SCHEMA_CODE_DUPLICATE);
        }
    }

    private void validateEventSchemaCanDelete(EventSchemaDO eventSchema) {
        List<String> references = new ArrayList<>();
        Long eventFieldCount = eventFieldMapper.selectCount(new LambdaQueryWrapperX<EventFieldDO>()
                .eq(EventFieldDO::getSceneCode, eventSchema.getSceneCode())
                .eq(EventFieldDO::getEventCode, eventSchema.getEventCode()));
        if (eventFieldCount != null && eventFieldCount > 0) {
            references.add("事件字段 " + eventFieldCount + " 条");
        }
        Long eventSampleCount = eventSampleMapper.selectCount(new LambdaQueryWrapperX<EventSampleDO>()
                .eq(EventSampleDO::getSceneCode, eventSchema.getSceneCode())
                .eq(EventSampleDO::getEventCode, eventSchema.getEventCode()));
        if (eventSampleCount != null && eventSampleCount > 0) {
            references.add("事件样例 " + eventSampleCount + " 条");
        }
        Long ingestMappingCount = ingestMappingMapper.selectCount(new LambdaQueryWrapperX<IngestMappingDO>()
                .eq(IngestMappingDO::getSceneCode, eventSchema.getSceneCode())
                .eq(IngestMappingDO::getEventCode, eventSchema.getEventCode()));
        if (ingestMappingCount != null && ingestMappingCount > 0) {
            references.add("字段映射 " + ingestMappingCount + " 条");
        }
        List<String> featureCodes = featureStreamConfMapper.selectList(new LambdaQueryWrapperX<FeatureStreamConfDO>()
                        .eq(FeatureStreamConfDO::getSceneCode, eventSchema.getSceneCode()))
                .stream()
                .filter(conf -> StrUtil.splitTrim(conf.getSourceEventCodes(), ',').contains(eventSchema.getEventCode()))
                .map(FeatureStreamConfDO::getFeatureCode)
                .distinct()
                .toList();
        if (!featureCodes.isEmpty()) {
            references.add("流式特征 " + String.join(",", featureCodes));
        }
        if (!references.isEmpty()) {
            throw ServiceExceptionUtil.exception(EVENT_SCHEMA_DELETE_INVALID, String.join("；", references));
        }
    }

    private String buildEventSchemaBizCode(String sceneCode, String eventCode) {
        return sceneCode + ':' + eventCode;
    }

}
