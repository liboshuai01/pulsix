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
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_FIELD_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_FIELD_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SCHEMA_NOT_EXISTS;

@Service
public class EventFieldServiceImpl implements EventFieldService {

    @Resource
    private EventFieldMapper eventFieldMapper;

    @Resource
    private EventSchemaMapper eventSchemaMapper;

    @Override
    public Long createEventField(EventFieldSaveReqVO createReqVO) {
        validateEventSchemaExists(createReqVO.getSceneCode(), createReqVO.getEventCode());
        validateEventFieldCodeUnique(createReqVO.getSceneCode(), createReqVO.getEventCode(), createReqVO.getFieldCode(), null);
        EventFieldDO eventField = BeanUtils.toBean(createReqVO, EventFieldDO.class);
        fillFieldPathIfBlank(eventField);
        eventFieldMapper.insert(eventField);
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
    }

    @Override
    public void deleteEventField(Long id) {
        validateEventFieldExists(id);
        eventFieldMapper.deleteById(id);
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
        validateEventFieldExists(id);
        EventFieldDO updateObj = new EventFieldDO();
        updateObj.setId(id);
        updateObj.setSortNo(sortNo);
        eventFieldMapper.updateById(updateObj);
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

}
