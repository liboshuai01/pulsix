package cn.liboshuai.pulsix.module.risk.service.eventsample;

import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo.EventSamplePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo.EventSamplePreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo.EventSampleSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventsample.EventSampleDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventschema.EventSchemaDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventsample.EventSampleMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventschema.EventSchemaMapper;
import cn.liboshuai.pulsix.module.risk.service.preview.StandardEventPreviewResult;
import cn.liboshuai.pulsix.module.risk.service.preview.StandardEventPreviewService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SAMPLE_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SAMPLE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SCHEMA_NOT_EXISTS;

@Service
public class EventSampleServiceImpl implements EventSampleService {

    @Resource
    private EventSampleMapper eventSampleMapper;

    @Resource
    private EventSchemaMapper eventSchemaMapper;

    @Resource
    private StandardEventPreviewService standardEventPreviewService;

    @Override
    public Long createEventSample(EventSampleSaveReqVO createReqVO) {
        validateEventSchemaExists(createReqVO.getSceneCode(), createReqVO.getEventCode());
        validateEventSampleCodeUnique(createReqVO.getSceneCode(), createReqVO.getEventCode(), createReqVO.getSampleCode(), null);
        EventSampleDO eventSample = BeanUtils.toBean(createReqVO, EventSampleDO.class);
        eventSampleMapper.insert(eventSample);
        return eventSample.getId();
    }

    @Override
    public void updateEventSample(EventSampleSaveReqVO updateReqVO) {
        EventSampleDO eventSample = validateEventSampleExists(updateReqVO.getId());
        EventSampleDO updateObj = BeanUtils.toBean(updateReqVO, EventSampleDO.class);
        updateObj.setSceneCode(eventSample.getSceneCode());
        updateObj.setEventCode(eventSample.getEventCode());
        updateObj.setSampleCode(eventSample.getSampleCode());
        eventSampleMapper.updateById(updateObj);
    }

    @Override
    public void deleteEventSample(Long id) {
        validateEventSampleExists(id);
        eventSampleMapper.deleteById(id);
    }

    @Override
    public EventSampleDO getEventSample(Long id) {
        return eventSampleMapper.selectById(id);
    }

    @Override
    public PageResult<EventSampleDO> getEventSamplePage(EventSamplePageReqVO pageReqVO) {
        return eventSampleMapper.selectPage(pageReqVO);
    }

    @Override
    public EventSamplePreviewRespVO previewEventSample(Long id) {
        EventSampleDO eventSample = validateEventSampleExists(id);
        StandardEventPreviewResult previewResult = standardEventPreviewService.preview(
                eventSample.getSceneCode(), eventSample.getEventCode(), eventSample.getSourceCode(), eventSample.getSampleJson());

        EventSamplePreviewRespVO respVO = new EventSamplePreviewRespVO();
        respVO.setSampleId(eventSample.getId());
        respVO.setSampleCode(eventSample.getSampleCode());
        respVO.setSampleName(eventSample.getSampleName());
        respVO.setSampleType(eventSample.getSampleType());
        respVO.setSampleJson(previewResult.getRawEventJson());
        respVO.setStandardEventJson(previewResult.getStandardEventJson());
        respVO.setMissingRequiredFields(previewResult.getMissingRequiredFields());
        respVO.setDefaultedFields(previewResult.getDefaultedFields());
        respVO.setMappedFields(previewResult.getMappedFields());
        return respVO;
    }

    private EventSampleDO validateEventSampleExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(EVENT_SAMPLE_NOT_EXISTS);
        }
        EventSampleDO eventSample = eventSampleMapper.selectById(id);
        if (eventSample == null) {
            throw ServiceExceptionUtil.exception(EVENT_SAMPLE_NOT_EXISTS);
        }
        return eventSample;
    }

    private void validateEventSchemaExists(String sceneCode, String eventCode) {
        EventSchemaDO eventSchema = eventSchemaMapper.selectOne(EventSchemaDO::getSceneCode, sceneCode,
                EventSchemaDO::getEventCode, eventCode);
        if (eventSchema == null) {
            throw ServiceExceptionUtil.exception(EVENT_SCHEMA_NOT_EXISTS);
        }
    }

    private void validateEventSampleCodeUnique(String sceneCode, String eventCode, String sampleCode, Long id) {
        EventSampleDO eventSample = eventSampleMapper.selectOne(EventSampleDO::getSceneCode, sceneCode,
                EventSampleDO::getEventCode, eventCode, EventSampleDO::getSampleCode, sampleCode);
        if (eventSample == null) {
            return;
        }
        if (id == null || !id.equals(eventSample.getId())) {
            throw ServiceExceptionUtil.exception(EVENT_SAMPLE_CODE_DUPLICATE);
        }
    }

}
