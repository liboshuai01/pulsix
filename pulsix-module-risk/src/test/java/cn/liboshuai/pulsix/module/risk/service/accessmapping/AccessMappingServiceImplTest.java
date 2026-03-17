package cn.liboshuai.pulsix.module.risk.service.accessmapping;

import cn.liboshuai.pulsix.framework.common.exception.ServiceException;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingPreviewRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingRuleItemVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessRawFieldItemVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accessmapping.EventAccessMappingRuleDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.AccessSourceDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.EventAccessBindingDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventFieldDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventSchemaDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accessmapping.EventAccessMappingRuleMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accessmapping.EventAccessRawFieldDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accesssource.AccessSourceMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accesssource.EventAccessBindingMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventmodel.EventFieldDefMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.eventmodel.EventSchemaMapper;
import cn.liboshuai.pulsix.module.risk.service.accessmapping.bo.AccessMappingRuntimeBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessMappingServiceImplTest {

    @Mock
    private EventAccessBindingMapper eventAccessBindingMapper;
    @Mock
    private EventAccessRawFieldDefMapper eventAccessRawFieldDefMapper;
    @Mock
    private EventAccessMappingRuleMapper eventAccessMappingRuleMapper;
    @Mock
    private EventSchemaMapper eventSchemaMapper;
    @Mock
    private EventFieldDefMapper eventFieldDefMapper;
    @Mock
    private AccessSourceMapper accessSourceMapper;

    @InjectMocks
    private AccessMappingServiceImpl accessMappingService;

    @BeforeEach
    void setUp() {
        EventSchemaDO eventSchema = createEventSchema();
        AccessSourceDO accessSource = createAccessSource();
        lenient().when(eventSchemaMapper.selectByEventCode(anyString())).thenReturn(eventSchema);
        lenient().when(accessSourceMapper.selectBySourceCode(anyString())).thenReturn(accessSource);
        lenient().when(eventFieldDefMapper.selectListByEventCode(anyString())).thenReturn(createStandardFields());
        lenient().when(eventAccessBindingMapper.selectByEventCodeAndSourceCode(anyString(), anyString())).thenReturn(null);
    }

    @Test
    void previewStandardEvent_aviatorScriptSuccess() {
        AccessMappingPreviewRespVO preview = accessMappingService.previewStandardEvent(createBaseReqVO());

        assertThat(preview.getStandardEventJson().get("sceneCode")).isEqualTo("PROMOTION_RISK");
        assertThat(preview.getStandardEventJson().get("eventCode")).isEqualTo("PROMOTION_EVENT");
        assertThat(preview.getStandardEventJson().get("grantAmount")).isInstanceOf(BigDecimal.class);
        assertThat((BigDecimal) preview.getStandardEventJson().get("grantAmount"))
                .isEqualByComparingTo(new BigDecimal("68.80"));
        assertThat(preview.getStandardEventJson().get("channel")).isEqualTo("promotion-center");
        assertThat(preview.getFieldSourceMap().get("grantAmount")).isEqualTo("MAPPING");
        assertThat(preview.getFieldSourceMap().get("channel")).isEqualTo("MAPPING");
        assertThat(preview.getMessages()).isEmpty();
    }

    @Test
    void createAccessMapping_aviatorScriptSuccess() {
        AccessMappingSaveReqVO reqVO = createBaseReqVO();
        doAnswer(invocation -> {
            EventAccessBindingDO binding = invocation.getArgument(0);
            binding.setId(14101L);
            return 1;
        }).when(eventAccessBindingMapper).insert(any(EventAccessBindingDO.class));

        Long id = accessMappingService.createAccessMapping(reqVO);

        assertThat(id).isEqualTo(14101L);
        ArgumentCaptor<Collection<EventAccessMappingRuleDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(eventAccessMappingRuleMapper).insertBatch(captor.capture());
        assertThat(captor.getValue())
                .extracting(EventAccessMappingRuleDO::getScriptEngine)
                .contains("AVIATOR");
    }

    @Test
    void createAccessMapping_invalidAviatorExpression_rejected() {
        AccessMappingSaveReqVO reqVO = createBaseReqVO();
        reqVO.getMappingRules().stream()
                .filter(rule -> "grantAmount".equals(rule.getTargetFieldName()))
                .findFirst()
                .ifPresent(rule -> rule.setScriptContent("rawPayload['order']['amount'"));

        assertThatThrownBy(() -> accessMappingService.createAccessMapping(reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_310);
    }

    @Test
    void createAccessMapping_expressionEngineRejected() {
        AccessMappingSaveReqVO reqVO = createBaseReqVO();
        reqVO.getMappingRules().stream()
                .filter(rule -> "grantAmount".equals(rule.getTargetFieldName()))
                .findFirst()
                .ifPresent(rule -> rule.setScriptEngine("EXPRESSION"));

        assertThatThrownBy(() -> accessMappingService.createAccessMapping(reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_311);
    }

    @Test
    void getRuntimeAccessMapping_expressionEngineRejected() {
        EventAccessBindingDO binding = new EventAccessBindingDO();
        binding.setId(14101L);
        binding.setEventCode("PROMOTION_EVENT");
        binding.setSourceCode("PROMOTION_CENTER_HTTP");
        when(eventAccessBindingMapper.selectAccessMappingListBySourceCodeAndEventCode("PROMOTION_CENTER_HTTP", "PROMOTION_EVENT"))
                .thenReturn(List.of(binding));
        when(eventAccessRawFieldDefMapper.selectListByBindingId(anyLong())).thenReturn(List.of());
        when(eventAccessMappingRuleMapper.selectListByBindingId(14101L))
                .thenReturn(List.of(createRuleDO("grantAmount", "EXPRESSION", "rawPayload['order']['amount']")));

        assertThatThrownBy(() -> accessMappingService.getRuntimeAccessMapping("PROMOTION_CENTER_HTTP", "PROMOTION_EVENT"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("scriptEngine=EXPRESSION");
    }

    @Test
    void getRuntimeAccessMapping_aviatorScriptSuccess() {
        EventAccessBindingDO binding = new EventAccessBindingDO();
        binding.setId(14101L);
        binding.setEventCode("PROMOTION_EVENT");
        binding.setSourceCode("PROMOTION_CENTER_HTTP");
        when(eventAccessBindingMapper.selectAccessMappingListBySourceCodeAndEventCode("PROMOTION_CENTER_HTTP", "PROMOTION_EVENT"))
                .thenReturn(List.of(binding));
        when(eventAccessRawFieldDefMapper.selectListByBindingId(anyLong())).thenReturn(List.of());
        when(eventAccessMappingRuleMapper.selectListByBindingId(14101L))
                .thenReturn(List.of(createRuleDO("grantAmount", "AVIATOR", "rawPayload['order']['amount']")));

        AccessMappingRuntimeBO runtimeBO = accessMappingService.getRuntimeAccessMapping("PROMOTION_CENTER_HTTP", "PROMOTION_EVENT");

        assertThat(runtimeBO).isNotNull();
        assertThat(runtimeBO.getMappingRules()).hasSize(1);
        assertThat(runtimeBO.getMappingRules().get(0).getScriptEngine()).isEqualTo("AVIATOR");
    }

    private AccessMappingSaveReqVO createBaseReqVO() {
        AccessMappingSaveReqVO reqVO = new AccessMappingSaveReqVO();
        reqVO.setEventCode("PROMOTION_EVENT");
        reqVO.setSourceCode("PROMOTION_CENTER_HTTP");
        reqVO.setDescription("营销中心受理事件接入映射");

        Map<String, Object> rawPayload = new LinkedHashMap<>();
        rawPayload.put("order", Map.of("amount", "68.80"));
        reqVO.setRawSampleJson(rawPayload);
        reqVO.setSampleHeadersJson(Map.of("x-source", "promotion-center"));

        AccessRawFieldItemVO rawField = new AccessRawFieldItemVO();
        rawField.setFieldName("amount");
        rawField.setFieldPath("order.amount");
        rawField.setFieldType("DECIMAL");
        rawField.setRequiredFlag(1);
        rawField.setSortNo(1);
        reqVO.setRawFields(List.of(rawField));

        AccessMappingRuleItemVO amountRule = new AccessMappingRuleItemVO();
        amountRule.setTargetFieldName("grantAmount");
        amountRule.setMappingType("SCRIPT");
        amountRule.setScriptEngine("AVIATOR");
        amountRule.setScriptContent("rawPayload['order']['amount']");

        AccessMappingRuleItemVO channelRule = new AccessMappingRuleItemVO();
        channelRule.setTargetFieldName("channel");
        channelRule.setMappingType("SCRIPT");
        channelRule.setScriptEngine("AVIATOR");
        channelRule.setScriptContent("headers['x-source']");

        reqVO.setMappingRules(List.of(amountRule, channelRule));
        return reqVO;
    }

    private EventSchemaDO createEventSchema() {
        EventSchemaDO schema = new EventSchemaDO();
        schema.setId(11L);
        schema.setSceneCode("PROMOTION_RISK");
        schema.setEventCode("PROMOTION_EVENT");
        return schema;
    }

    private AccessSourceDO createAccessSource() {
        AccessSourceDO accessSource = new AccessSourceDO();
        accessSource.setId(21L);
        accessSource.setSourceCode("PROMOTION_CENTER_HTTP");
        accessSource.setSourceName("营销中心 HTTP 接入");
        accessSource.setAllowedSceneCodes(List.of("PROMOTION_RISK"));
        return accessSource;
    }

    private List<EventFieldDefDO> createStandardFields() {
        return List.of(
                createField("eventId", "STRING", 1, 1),
                createField("sceneCode", "STRING", 1, 2),
                createField("eventCode", "STRING", 1, 3),
                createField("grantAmount", "DECIMAL", 1, 4),
                createField("channel", "STRING", 0, 5)
        );
    }

    private EventFieldDefDO createField(String fieldName, String fieldType, Integer requiredFlag, Integer sortNo) {
        EventFieldDefDO field = new EventFieldDefDO();
        field.setEventCode("PROMOTION_EVENT");
        field.setFieldName(fieldName);
        field.setFieldLabel(fieldName);
        field.setFieldType(fieldType);
        field.setRequiredFlag(requiredFlag);
        field.setSortNo(sortNo);
        return field;
    }

    private EventAccessMappingRuleDO createRuleDO(String targetFieldName, String scriptEngine, String scriptContent) {
        EventAccessMappingRuleDO rule = new EventAccessMappingRuleDO();
        rule.setTargetFieldName(targetFieldName);
        rule.setMappingType("SCRIPT");
        rule.setScriptEngine(scriptEngine);
        rule.setScriptContent(scriptContent);
        return rule;
    }

}
