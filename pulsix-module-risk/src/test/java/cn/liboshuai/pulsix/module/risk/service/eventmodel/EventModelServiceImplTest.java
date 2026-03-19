package cn.liboshuai.pulsix.module.risk.service.eventmodel;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.exception.ServiceException;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventFieldItemVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelPreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventModelSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventFieldDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventSchemaDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accesssource.EventAccessBindingMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventmodel.EventFieldDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventmodel.EventSchemaMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventModelServiceImplTest {

    @Mock
    private EventSchemaMapper eventSchemaMapper;
    @Mock
    private EventFieldDefMapper eventFieldDefMapper;
    @Mock
    private EventAccessBindingMapper eventAccessBindingMapper;
    @Mock
    private SceneMapper sceneMapper;

    @InjectMocks
    private EventModelServiceImpl eventModelService;

    @BeforeEach
    void setUp() {
        lenient().when(sceneMapper.selectBySceneCode(anyString())).thenReturn(createScene("TRADE_RISK"));
    }

    @Test
    void createEventModel_success() {
        EventModelSaveReqVO reqVO = createBaseReqVO();
        when(eventSchemaMapper.selectByEventCode(reqVO.getEventCode())).thenReturn(null);
        doAnswer(invocation -> {
            EventSchemaDO schema = invocation.getArgument(0);
            schema.setId(100L);
            return 1;
        }).when(eventSchemaMapper).insert(any(EventSchemaDO.class));

        Long id = eventModelService.createEventModel(reqVO);

        assertThat(id).isEqualTo(100L);
        ArgumentCaptor<EventSchemaDO> schemaCaptor = ArgumentCaptor.forClass(EventSchemaDO.class);
        verify(eventSchemaMapper).insert(schemaCaptor.capture());
        assertThat(schemaCaptor.getValue().getStatus()).isEqualTo(CommonStatusEnum.DISABLE.getStatus());

        ArgumentCaptor<Collection<EventFieldDefDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(eventFieldDefMapper).insertBatch(captor.capture());
        List<EventFieldDefDO> insertedFields = new ArrayList<>(captor.getValue());
        assertThat(insertedFields).hasSize(6);
        assertThat(insertedFields).allMatch(field -> "TRADE_EVENT".equals(field.getEventCode()));
        assertThat(insertedFields).extracting(EventFieldDefDO::getSortNo)
                .containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void createEventModel_missingPublicFields_autoInjected() {
        EventModelSaveReqVO reqVO = createBaseReqVO();
        reqVO.setFields(new ArrayList<>(List.of(
                createField("amount", "交易金额", "DECIMAL", 1, null, "66.5", 1)
        )));
        when(eventSchemaMapper.selectByEventCode(reqVO.getEventCode())).thenReturn(null);
        doAnswer(invocation -> {
            EventSchemaDO schema = invocation.getArgument(0);
            schema.setId(101L);
            return 1;
        }).when(eventSchemaMapper).insert(any(EventSchemaDO.class));

        eventModelService.createEventModel(reqVO);

        ArgumentCaptor<Collection<EventFieldDefDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(eventFieldDefMapper).insertBatch(captor.capture());
        List<EventFieldDefDO> insertedFields = new ArrayList<>(captor.getValue());
        assertThat(insertedFields).extracting(EventFieldDefDO::getFieldName)
                .containsExactly("eventId", "traceId", "sceneCode", "eventCode", "eventTime", "amount");
        assertThat(insertedFields).filteredOn(field -> !"amount".equals(field.getFieldName()))
                .allMatch(field -> field.getRequiredFlag() == 1);
    }

    @Test
    void createEventModel_duplicateEventCode_rejected() {
        EventModelSaveReqVO reqVO = createBaseReqVO();
        when(eventSchemaMapper.selectByEventCode(reqVO.getEventCode()))
                .thenReturn(createEventSchema(1L, "TRADE_RISK", "TRADE_EVENT"));

        assertThatThrownBy(() -> eventModelService.createEventModel(reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_101);
    }

    @Test
    void updateEventModel_identityChanged_rejected() {
        EventModelSaveReqVO reqVO = createBaseReqVO();
        reqVO.setId(10L);
        reqVO.setSceneCode("ORDER_RISK");
        when(eventSchemaMapper.selectById(10L)).thenReturn(createEventSchema(10L, "TRADE_RISK", "TRADE_EVENT"));

        assertThatThrownBy(() -> eventModelService.updateEventModel(reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_102);
    }

    @Test
    void updateEventModel_eventCodeChanged_rejected() {
        EventModelSaveReqVO reqVO = createBaseReqVO();
        reqVO.setId(10L);
        reqVO.setEventCode("ORDER_EVENT");
        when(eventSchemaMapper.selectById(10L)).thenReturn(createEventSchema(10L, "TRADE_RISK", "TRADE_EVENT"));

        assertThatThrownBy(() -> eventModelService.updateEventModel(reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_102);
    }

    @Test
    void updateEventModel_preservesExistingStatus() {
        EventModelSaveReqVO reqVO = createBaseReqVO();
        reqVO.setId(10L);
        reqVO.setStatus(1);
        EventSchemaDO schema = createEventSchema(10L, "TRADE_RISK", "TRADE_EVENT");
        schema.setStatus(0);
        when(eventSchemaMapper.selectById(10L)).thenReturn(schema);
        when(eventSchemaMapper.selectByEventCode(reqVO.getEventCode())).thenReturn(schema);

        eventModelService.updateEventModel(reqVO);

        ArgumentCaptor<EventSchemaDO> captor = ArgumentCaptor.forClass(EventSchemaDO.class);
        verify(eventSchemaMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        assertThat(captor.getValue().getVersion()).isEqualTo(2);
        verify(eventFieldDefMapper).deleteByEventCodePhysically("TRADE_EVENT");
    }

    @Test
    void updateEventModel_sortChanged_replacesFieldListWithoutDuplicateInsertConflict() {
        EventModelSaveReqVO reqVO = createBaseReqVO();
        reqVO.setId(10L);
        EventSchemaDO schema = createEventSchema(10L, "TRADE_RISK", "TRADE_EVENT");
        when(eventSchemaMapper.selectById(10L)).thenReturn(schema);
        when(eventSchemaMapper.selectByEventCode(reqVO.getEventCode())).thenReturn(schema);

        reqVO.getFields().stream()
                .filter(field -> "sceneCode".equals(field.getFieldName()))
                .findFirst()
                .ifPresent(field -> field.setSortNo(3));
        reqVO.getFields().stream()
                .filter(field -> "eventCode".equals(field.getFieldName()))
                .findFirst()
                .ifPresent(field -> field.setSortNo(2));

        eventModelService.updateEventModel(reqVO);

        verify(eventFieldDefMapper).deleteByEventCodePhysically("TRADE_EVENT");
        ArgumentCaptor<Collection<EventFieldDefDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(eventFieldDefMapper).insertBatch(captor.capture());
        List<EventFieldDefDO> insertedFields = new ArrayList<>(captor.getValue());
        assertThat(insertedFields).extracting(EventFieldDefDO::getFieldName)
                .containsExactly("eventId", "traceId", "sceneCode", "eventCode", "eventTime", "amount");
        assertThat(insertedFields).extracting(EventFieldDefDO::getSortNo)
                .containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void updateEventModel_publicFieldConfigChanged_correctedBeforeInsert() {
        EventModelSaveReqVO reqVO = createBaseReqVO();
        reqVO.setId(10L);
        EventSchemaDO schema = createEventSchema(10L, "TRADE_RISK", "TRADE_EVENT");
        when(eventSchemaMapper.selectById(10L)).thenReturn(schema);
        when(eventSchemaMapper.selectByEventCode(reqVO.getEventCode())).thenReturn(schema);

        reqVO.getFields().stream()
                .filter(field -> "traceId".equals(field.getFieldName()))
                .findFirst()
                .ifPresent(field -> {
                    field.setFieldType("JSON");
                    field.setRequiredFlag(0);
                    field.setSortNo(99);
                });
        reqVO.getFields().stream()
                .filter(field -> "eventTime".equals(field.getFieldName()))
                .findFirst()
                .ifPresent(field -> field.setRequiredFlag(0));

        eventModelService.updateEventModel(reqVO);

        ArgumentCaptor<Collection<EventFieldDefDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(eventFieldDefMapper).insertBatch(captor.capture());
        List<EventFieldDefDO> insertedFields = new ArrayList<>(captor.getValue());
        EventFieldDefDO traceIdField = insertedFields.stream()
                .filter(field -> "traceId".equals(field.getFieldName()))
                .findFirst()
                .orElseThrow();
        EventFieldDefDO eventTimeField = insertedFields.stream()
                .filter(field -> "eventTime".equals(field.getFieldName()))
                .findFirst()
                .orElseThrow();
        assertThat(traceIdField.getFieldType()).isEqualTo("STRING");
        assertThat(traceIdField.getRequiredFlag()).isEqualTo(1);
        assertThat(traceIdField.getSortNo()).isEqualTo(2);
        assertThat(eventTimeField.getRequiredFlag()).isEqualTo(1);
        assertThat(eventTimeField.getSortNo()).isEqualTo(5);
    }

    @Test
    void createEventModel_duplicateField_rejected() {
        EventModelSaveReqVO reqVO = createBaseReqVO();
        reqVO.getFields().add(createField("eventId", "重复事件ID", "STRING", 1, null, "E_TRADE_0009", 99));
        when(eventSchemaMapper.selectByEventCode(reqVO.getEventCode())).thenReturn(null);

        assertThatThrownBy(() -> eventModelService.createEventModel(reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_104);
    }

    @Test
    void createEventModel_invalidFieldSampleValue_rejected() {
        EventModelSaveReqVO reqVO = createBaseReqVO();
        reqVO.getFields().stream()
                .filter(field -> "amount".equals(field.getFieldName()))
                .findFirst()
                .ifPresent(field -> field.setSampleValue("NOT_A_NUMBER"));
        when(eventSchemaMapper.selectByEventCode(reqVO.getEventCode())).thenReturn(null);

        assertThatThrownBy(() -> eventModelService.createEventModel(reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_105);
    }

    @Test
    void createEventModel_disabledExtField_rejected() {
        EventModelSaveReqVO reqVO = createBaseReqVO();
        reqVO.getFields().add(createField("ext", "扩展字段", "JSON", 0, null, "{\"foo\":\"bar\"}", 6));
        when(eventSchemaMapper.selectByEventCode(reqVO.getEventCode())).thenReturn(null);

        assertThatThrownBy(() -> eventModelService.createEventModel(reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_105);
    }

    @Test
    void previewStandardEvent_typeConversionCorrect() {
        EventModelSaveReqVO reqVO = createBaseReqVO();
        reqVO.getFields().stream()
                .filter(field -> "amount".equals(field.getFieldName()))
                .findFirst()
                .ifPresent(field -> field.setSampleValue("88.75"));

        EventModelPreviewRespVO preview = eventModelService.previewStandardEvent(reqVO);

        assertThat(preview.getStandardEventJson().get("sceneCode")).isEqualTo("TRADE_RISK");
        assertThat(preview.getStandardEventJson().get("eventCode")).isEqualTo("TRADE_EVENT");
        assertThat(preview.getStandardEventJson().get("amount")).isInstanceOf(BigDecimal.class);
        assertThat((BigDecimal) preview.getStandardEventJson().get("amount"))
                .isEqualByComparingTo(new BigDecimal("88.75"));
        assertThat(preview.getRequiredFields())
                .contains("eventId", "traceId", "sceneCode", "eventCode", "eventTime", "amount");
    }

    @Test
    void previewStandardEvent_missingPublicFields_stillGeneratesSystemValues() {
        EventModelSaveReqVO reqVO = createBaseReqVO();
        reqVO.setFields(new ArrayList<>(List.of(
                createField("amount", "交易金额", "DECIMAL", 1, null, "66.5", 1)
        )));

        EventModelPreviewRespVO preview = eventModelService.previewStandardEvent(reqVO);

        assertThat(preview.getStandardEventJson().get("eventId")).isEqualTo("AUTO_TRADE_EVENT_EVENT_ID");
        assertThat(preview.getStandardEventJson().get("traceId")).isEqualTo("AUTO_TRADE_EVENT_TRACE_ID");
        assertThat(preview.getStandardEventJson().get("sceneCode")).isEqualTo("TRADE_RISK");
        assertThat(preview.getStandardEventJson().get("eventCode")).isEqualTo("TRADE_EVENT");
        assertThat(preview.getStandardEventJson()).containsKey("eventTime");
    }

    @Test
    void deleteEventModel_withFeatureReference_rejected() {
        when(eventSchemaMapper.selectById(10L)).thenReturn(createEventSchema(10L, "TRADE_RISK", "TRADE_EVENT"));
        when(eventSchemaMapper.selectFeatureCountByEventCode("TRADE_EVENT")).thenReturn(1L);

        assertThatThrownBy(() -> eventModelService.deleteEventModel(10L))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_103);
        verify(eventFieldDefMapper, never()).deleteByEventCode(anyString());
        verify(eventAccessBindingMapper, never()).deleteByEventCodePhysically(anyString());
    }

    @Test
    void deleteEventModel_withAccessMappingReference_rejected() {
        when(eventSchemaMapper.selectById(10L)).thenReturn(createEventSchema(10L, "TRADE_RISK", "TRADE_EVENT"));
        when(eventSchemaMapper.selectFeatureCountByEventCode("TRADE_EVENT")).thenReturn(0L);
        when(eventAccessBindingMapper.selectCountByEventCode("TRADE_EVENT")).thenReturn(1L);

        assertThatThrownBy(() -> eventModelService.deleteEventModel(10L))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_103);
        verify(eventFieldDefMapper, never()).deleteByEventCode(anyString());
    }

    private EventModelSaveReqVO createBaseReqVO() {
        EventModelSaveReqVO reqVO = new EventModelSaveReqVO();
        reqVO.setSceneCode("TRADE_RISK");
        reqVO.setEventCode("TRADE_EVENT");
        reqVO.setEventName("交易事件");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setDescription("交易标准事件模型");

        reqVO.setFields(new ArrayList<>(List.of(
                createField("eventId", "事件ID", "STRING", 1, null, "E_TRADE_0001", 1),
                createField("traceId", "链路ID", "STRING", 1, null, "T_TRADE_0001", 2),
                createField("sceneCode", "场景编码", "STRING", 1, null, "TRADE_RISK", 3),
                createField("eventCode", "事件编码", "STRING", 1, null, "TRADE_EVENT", 4),
                createField("eventTime", "事件时间", "DATETIME", 1, null, "2026-03-08T10:00:00", 5),
                createField("amount", "交易金额", "DECIMAL", 1, null, "66.5", 6)
        )));
        return reqVO;
    }

    private EventFieldItemVO createField(String fieldName, String fieldLabel, String fieldType, Integer requiredFlag,
                                         String defaultValue, String sampleValue, Integer sortNo) {
        EventFieldItemVO field = new EventFieldItemVO();
        field.setFieldName(fieldName);
        field.setFieldLabel(fieldLabel);
        field.setFieldType(fieldType);
        field.setRequiredFlag(requiredFlag);
        field.setDefaultValue(defaultValue);
        field.setSampleValue(sampleValue);
        field.setSortNo(sortNo);
        return field;
    }

    private EventSchemaDO createEventSchema(Long id, String sceneCode, String eventCode) {
        EventSchemaDO schema = new EventSchemaDO();
        schema.setId(id);
        schema.setSceneCode(sceneCode);
        schema.setEventCode(eventCode);
        schema.setVersion(1);
        schema.setStatus(0);
        return schema;
    }

    private SceneDO createScene(String sceneCode) {
        SceneDO scene = new SceneDO();
        scene.setId(1L);
        scene.setSceneCode(sceneCode);
        scene.setSceneName("测试场景");
        return scene;
    }

}
