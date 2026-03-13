package cn.liboshuai.pulsix.module.risk.service.eventschema;

import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventschema.vo.EventSchemaPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventschema.vo.EventSchemaSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventschema.EventSchemaDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventschema.EventSchemaMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import cn.liboshuai.pulsix.module.risk.service.auditlog.AuditLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SCHEMA_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SCHEMA_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_CREATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_UPDATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.BIZ_TYPE_EVENT_SCHEMA;

@Service
public class EventSchemaServiceImpl implements EventSchemaService {

    @Resource
    private EventSchemaMapper eventSchemaMapper;

    @Resource
    private SceneMapper sceneMapper;

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

    private String buildEventSchemaBizCode(String sceneCode, String eventCode) {
        return sceneCode + ':' + eventCode;
    }

}
