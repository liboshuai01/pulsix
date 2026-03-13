package cn.liboshuai.pulsix.module.risk.service.eventfield;

import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventfield.vo.EventFieldPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventfield.vo.EventFieldSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventfield.EventFieldDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventschema.EventSchemaDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventfield.EventFieldMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventschema.EventSchemaMapper;
import cn.liboshuai.pulsix.module.risk.service.auditlog.AuditLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_FIELD_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_FIELD_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SCHEMA_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_CREATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_DELETE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_SORT;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_UPDATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.BIZ_TYPE_EVENT_FIELD;

@Service
public class EventFieldServiceImpl implements EventFieldService {

    @Resource
    private EventFieldMapper eventFieldMapper;

    @Resource
    private EventSchemaMapper eventSchemaMapper;

    @Resource
    private AuditLogService auditLogService;

    @Override
    public Long createEventField(EventFieldSaveReqVO createReqVO) {
        validateEventSchemaExists(createReqVO.getSceneCode(), createReqVO.getEventCode());
        validateEventFieldCodeUnique(createReqVO.getSceneCode(), createReqVO.getEventCode(), createReqVO.getFieldCode(), null);
        EventFieldDO eventField = BeanUtils.toBean(createReqVO, EventFieldDO.class);
        fillFieldPathIfBlank(eventField);
        eventFieldMapper.insert(eventField);
        auditLogService.createAuditLog(eventField.getSceneCode(), BIZ_TYPE_EVENT_FIELD, buildEventFieldBizCode(eventField),
                ACTION_CREATE, null, eventFieldMapper.selectById(eventField.getId()), "新增事件字段 " + eventField.getFieldCode());
        return eventField.getId();
    }

    @Override
    public void updateEventField(EventFieldSaveReqVO updateReqVO) {
        EventFieldDO eventField = validateEventFieldExists(updateReqVO.getId());
        EventFieldDO updateObj = BeanUtils.toBean(updateReqVO, EventFieldDO.class);
        updateObj.setSceneCode(eventField.getSceneCode());
        updateObj.setEventCode(eventField.getEventCode());
        updateObj.setFieldCode(eventField.getFieldCode());
        fillFieldPathIfBlank(updateObj);
        eventFieldMapper.updateById(updateObj);
        auditLogService.createAuditLog(eventField.getSceneCode(), BIZ_TYPE_EVENT_FIELD, buildEventFieldBizCode(eventField),
                ACTION_UPDATE, eventField, eventFieldMapper.selectById(eventField.getId()), "修改事件字段 " + eventField.getFieldCode());
    }

    @Override
    public void deleteEventField(Long id) {
        EventFieldDO eventField = validateEventFieldExists(id);
        eventFieldMapper.deleteById(id);
        auditLogService.createAuditLog(eventField.getSceneCode(), BIZ_TYPE_EVENT_FIELD, buildEventFieldBizCode(eventField),
                ACTION_DELETE, eventField, null, "删除事件字段 " + eventField.getFieldCode());
    }

    @Override
    public EventFieldDO getEventField(Long id) {
        return eventFieldMapper.selectById(id);
    }

    @Override
    public PageResult<EventFieldDO> getEventFieldPage(EventFieldPageReqVO pageReqVO) {
        return eventFieldMapper.selectPage(pageReqVO);
    }

    @Override
    public void updateEventFieldSort(Long id, Integer sortNo) {
        EventFieldDO eventField = validateEventFieldExists(id);
        EventFieldDO updateObj = new EventFieldDO();
        updateObj.setId(id);
        updateObj.setSortNo(sortNo);
        eventFieldMapper.updateById(updateObj);
        auditLogService.createAuditLog(eventField.getSceneCode(), BIZ_TYPE_EVENT_FIELD, buildEventFieldBizCode(eventField),
                ACTION_SORT, eventField, eventFieldMapper.selectById(id), "调整事件字段排序 " + eventField.getFieldCode());
    }

    private EventFieldDO validateEventFieldExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(EVENT_FIELD_NOT_EXISTS);
        }
        EventFieldDO eventField = eventFieldMapper.selectById(id);
        if (eventField == null) {
            throw ServiceExceptionUtil.exception(EVENT_FIELD_NOT_EXISTS);
        }
        return eventField;
    }

    private void validateEventSchemaExists(String sceneCode, String eventCode) {
        EventSchemaDO eventSchema = eventSchemaMapper.selectOne(EventSchemaDO::getSceneCode, sceneCode,
                EventSchemaDO::getEventCode, eventCode);
        if (eventSchema == null) {
            throw ServiceExceptionUtil.exception(EVENT_SCHEMA_NOT_EXISTS);
        }
    }

    private void validateEventFieldCodeUnique(String sceneCode, String eventCode, String fieldCode, Long id) {
        EventFieldDO eventField = eventFieldMapper.selectOne(EventFieldDO::getSceneCode, sceneCode,
                EventFieldDO::getEventCode, eventCode, EventFieldDO::getFieldCode, fieldCode);
        if (eventField == null) {
            return;
        }
        if (id == null || !id.equals(eventField.getId())) {
            throw ServiceExceptionUtil.exception(EVENT_FIELD_CODE_DUPLICATE);
        }
    }

    private void fillFieldPathIfBlank(EventFieldDO eventField) {
        if (StrUtil.isBlank(eventField.getFieldPath())) {
            eventField.setFieldPath("$." + eventField.getFieldCode());
        }
    }

    private String buildEventFieldBizCode(EventFieldDO eventField) {
        return eventField.getSceneCode() + ':' + eventField.getEventCode() + ':' + eventField.getFieldCode();
    }

}
