package cn.liboshuai.pulsix.module.risk.service.ingestmapping;

import cn.hutool.core.util.ObjectUtil;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo.IngestMappingPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo.IngestMappingPreviewReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo.IngestMappingPreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestmapping.vo.IngestMappingSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventfield.EventFieldDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventschema.EventSchemaDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingestmapping.IngestMappingDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingestsource.IngestSourceDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventfield.EventFieldMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventschema.EventSchemaMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.ingestmapping.IngestMappingMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.ingestsource.IngestSourceMapper;
import cn.liboshuai.pulsix.module.risk.service.preview.StandardEventPreviewResult;
import cn.liboshuai.pulsix.module.risk.service.preview.StandardEventPreviewService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_FIELD_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.EVENT_SCHEMA_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.INGEST_MAPPING_KEY_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.INGEST_MAPPING_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.INGEST_SOURCE_NOT_EXISTS;

@Service
public class IngestMappingServiceImpl implements IngestMappingService {

    @Resource
    private IngestMappingMapper ingestMappingMapper;

    @Resource
    private IngestSourceMapper ingestSourceMapper;

    @Resource
    private EventSchemaMapper eventSchemaMapper;

    @Resource
    private EventFieldMapper eventFieldMapper;

    @Resource
    private StandardEventPreviewService standardEventPreviewService;

    @Override
    public Long createIngestMapping(IngestMappingSaveReqVO createReqVO) {
        validateSourceExists(createReqVO.getSourceCode());
        validateEventSchemaExists(createReqVO.getSceneCode(), createReqVO.getEventCode());
        EventFieldDO targetField = validateTargetFieldExists(createReqVO.getSceneCode(), createReqVO.getEventCode(),
                createReqVO.getTargetFieldCode());
        validateIngestMappingKeyUnique(createReqVO.getSourceCode(), createReqVO.getSceneCode(), createReqVO.getEventCode(),
                createReqVO.getTargetFieldCode(), null);
        IngestMappingDO ingestMapping = BeanUtils.toBean(createReqVO, IngestMappingDO.class);
        ingestMapping.setTargetFieldName(targetField.getFieldName());
        ingestMappingMapper.insert(ingestMapping);
        return ingestMapping.getId();
    }

    @Override
    public void updateIngestMapping(IngestMappingSaveReqVO updateReqVO) {
        IngestMappingDO ingestMapping = validateIngestMappingExists(updateReqVO.getId());
        EventFieldDO targetField = validateTargetFieldExists(ingestMapping.getSceneCode(), ingestMapping.getEventCode(),
                ingestMapping.getTargetFieldCode());
        IngestMappingDO updateObj = BeanUtils.toBean(updateReqVO, IngestMappingDO.class);
        updateObj.setSourceCode(ingestMapping.getSourceCode());
        updateObj.setSceneCode(ingestMapping.getSceneCode());
        updateObj.setEventCode(ingestMapping.getEventCode());
        updateObj.setTargetFieldCode(ingestMapping.getTargetFieldCode());
        updateObj.setTargetFieldName(targetField.getFieldName());
        ingestMappingMapper.updateById(updateObj);
    }

    @Override
    public void deleteIngestMapping(Long id) {
        validateIngestMappingExists(id);
        ingestMappingMapper.deleteById(id);
    }

    @Override
    public IngestMappingDO getIngestMapping(Long id) {
        return ingestMappingMapper.selectById(id);
    }

    @Override
    public PageResult<IngestMappingDO> getIngestMappingPage(IngestMappingPageReqVO pageReqVO) {
        return ingestMappingMapper.selectPage(pageReqVO);
    }

    @Override
    public IngestMappingPreviewRespVO previewIngestMapping(IngestMappingPreviewReqVO reqVO) {
        validateSourceExists(reqVO.getSourceCode());
        validateEventSchemaExists(reqVO.getSceneCode(), reqVO.getEventCode());
        StandardEventPreviewResult previewResult = standardEventPreviewService.preview(
                reqVO.getSceneCode(), reqVO.getEventCode(), reqVO.getSourceCode(), reqVO.getRawEventJson());
        IngestMappingPreviewRespVO respVO = new IngestMappingPreviewRespVO();
        respVO.setSourceCode(reqVO.getSourceCode());
        respVO.setSceneCode(reqVO.getSceneCode());
        respVO.setEventCode(reqVO.getEventCode());
        respVO.setRawEventJson(previewResult.getRawEventJson());
        respVO.setStandardEventJson(previewResult.getStandardEventJson());
        respVO.setMissingRequiredFields(previewResult.getMissingRequiredFields());
        respVO.setDefaultedFields(previewResult.getDefaultedFields());
        respVO.setMappedFields(previewResult.getMappedFields());
        return respVO;
    }

    private IngestMappingDO validateIngestMappingExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(INGEST_MAPPING_NOT_EXISTS);
        }
        IngestMappingDO ingestMapping = ingestMappingMapper.selectById(id);
        if (ingestMapping == null) {
            throw ServiceExceptionUtil.exception(INGEST_MAPPING_NOT_EXISTS);
        }
        return ingestMapping;
    }

    private IngestSourceDO validateSourceExists(String sourceCode) {
        IngestSourceDO ingestSource = ingestSourceMapper.selectOne(IngestSourceDO::getSourceCode, sourceCode);
        if (ingestSource == null) {
            throw ServiceExceptionUtil.exception(INGEST_SOURCE_NOT_EXISTS);
        }
        return ingestSource;
    }

    private void validateEventSchemaExists(String sceneCode, String eventCode) {
        EventSchemaDO eventSchema = eventSchemaMapper.selectOne(EventSchemaDO::getSceneCode, sceneCode,
                EventSchemaDO::getEventCode, eventCode);
        if (eventSchema == null) {
            throw ServiceExceptionUtil.exception(EVENT_SCHEMA_NOT_EXISTS);
        }
    }

    private EventFieldDO validateTargetFieldExists(String sceneCode, String eventCode, String targetFieldCode) {
        EventFieldDO eventField = eventFieldMapper.selectOne(EventFieldDO::getSceneCode, sceneCode,
                EventFieldDO::getEventCode, eventCode, EventFieldDO::getFieldCode, targetFieldCode);
        if (eventField == null) {
            throw ServiceExceptionUtil.exception(EVENT_FIELD_NOT_EXISTS);
        }
        return eventField;
    }

    private void validateIngestMappingKeyUnique(String sourceCode, String sceneCode, String eventCode, String targetFieldCode,
                                                Long id) {
        IngestMappingDO ingestMapping = ingestMappingMapper.selectOne(new LambdaQueryWrapperX<IngestMappingDO>()
                .eq(IngestMappingDO::getSourceCode, sourceCode)
                .eq(IngestMappingDO::getSceneCode, sceneCode)
                .eq(IngestMappingDO::getEventCode, eventCode)
                .eq(IngestMappingDO::getTargetFieldCode, targetFieldCode));
        if (ingestMapping == null) {
            return;
        }
        if (id == null || !ObjectUtil.equal(ingestMapping.getId(), id)) {
            throw ServiceExceptionUtil.exception(INGEST_MAPPING_KEY_DUPLICATE);
        }
    }

}
