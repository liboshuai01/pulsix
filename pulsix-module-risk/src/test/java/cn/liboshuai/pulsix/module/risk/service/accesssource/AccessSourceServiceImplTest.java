package cn.liboshuai.pulsix.module.risk.service.accesssource;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.exception.ServiceException;
import cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo.AccessSourceSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.AccessSourceDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accesssource.AccessSourceMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.accesssource.EventAccessBindingMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class AccessSourceServiceImplTest {

    @Mock
    private AccessSourceMapper accessSourceMapper;
    @Mock
    private EventAccessBindingMapper eventAccessBindingMapper;
    @Mock
    private SceneMapper sceneMapper;

    @InjectMocks
    private AccessSourceServiceImpl accessSourceService;

    @BeforeEach
    void setUp() {
        lenient().when(sceneMapper.selectBySceneCode(anyString())).thenReturn(createScene("ORDER_RISK"));
    }

    @Test
    void createAccessSource_duplicateCode_rejected() {
        AccessSourceSaveReqVO reqVO = createBaseReqVO();
        when(accessSourceMapper.selectBySourceCode(reqVO.getSourceCode())).thenReturn(createAccessSource(1L));

        assertThatThrownBy(() -> accessSourceService.createAccessSource(reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_201);
    }

    @Test
    void updateAccessSource_codeChanged_rejected() {
        AccessSourceSaveReqVO reqVO = createBaseReqVO();
        reqVO.setId(10L);
        reqVO.setSourceCode("ORDER_CENTER_HTTP_V2");
        when(accessSourceMapper.selectById(10L)).thenReturn(createAccessSource(10L));

        assertThatThrownBy(() -> accessSourceService.updateAccessSource(reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_202);
    }

    @Test
    void deleteAccessSource_withBinding_rejected() {
        when(accessSourceMapper.selectById(10L)).thenReturn(createAccessSource(10L));
        when(eventAccessBindingMapper.selectCountBySourceCode("ORDER_CENTER_SDK")).thenReturn(1L);

        assertThatThrownBy(() -> accessSourceService.deleteAccessSource(10L))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_203);
        verify(accessSourceMapper, never()).deleteById(any());
    }

    @Test
    void deleteAccessSource_enabled_rejected() {
        AccessSourceDO accessSource = createAccessSource(10L);
        accessSource.setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(accessSourceMapper.selectById(10L)).thenReturn(accessSource);

        assertThatThrownBy(() -> accessSourceService.deleteAccessSource(10L))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_207);
        verify(accessSourceMapper, never()).deleteById(any());
    }

    @Test
    void deleteAccessSource_disabledWithoutBinding_success() {
        when(accessSourceMapper.selectById(10L)).thenReturn(createAccessSource(10L));
        when(eventAccessBindingMapper.selectCountBySourceCode("ORDER_CENTER_SDK")).thenReturn(0L);

        accessSourceService.deleteAccessSource(10L);

        verify(accessSourceMapper).deleteById(10L);
    }

    @Test
    void updateAccessSource_removeBoundScene_rejected() {
        AccessSourceSaveReqVO reqVO = createBaseReqVO();
        reqVO.setId(10L);
        reqVO.setAllowedSceneCodes(List.of("PROMOTION_RISK"));
        when(accessSourceMapper.selectById(10L)).thenReturn(createAccessSource(10L));
        when(accessSourceMapper.selectBySourceCode(reqVO.getSourceCode())).thenReturn(createAccessSource(10L));
        when(sceneMapper.selectBySceneCode("PROMOTION_RISK")).thenReturn(createScene("PROMOTION_RISK"));
        when(eventAccessBindingMapper.selectBoundSceneCodesBySourceCode("ORDER_CENTER_SDK"))
                .thenReturn(List.of("ORDER_RISK"));

        assertThatThrownBy(() -> accessSourceService.updateAccessSource(reqVO))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_206);
    }

    @Test
    void createAccessSource_success() {
        AccessSourceSaveReqVO reqVO = createBaseReqVO();
        when(accessSourceMapper.selectBySourceCode(reqVO.getSourceCode())).thenReturn(null);
        doAnswer(invocation -> {
            AccessSourceDO accessSource = invocation.getArgument(0);
            accessSource.setId(14001L);
            return 1;
        }).when(accessSourceMapper).insert(any(AccessSourceDO.class));

        accessSourceService.createAccessSource(reqVO);

        ArgumentCaptor<AccessSourceDO> captor = ArgumentCaptor.forClass(AccessSourceDO.class);
        verify(accessSourceMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CommonStatusEnum.DISABLE.getStatus());
    }

    private AccessSourceSaveReqVO createBaseReqVO() {
        AccessSourceSaveReqVO reqVO = new AccessSourceSaveReqVO();
        reqVO.setSourceCode("ORDER_CENTER_SDK");
        reqVO.setSourceName("订单中心 SDK 接入");
        reqVO.setSourceType("SDK");
        reqVO.setTopicName("pulsix.event.standard");
        reqVO.setRateLimitQps(500);
        reqVO.setAllowedSceneCodes(List.of("ORDER_RISK"));
        reqVO.setIpWhitelist(List.of("172.20.8.0/24"));
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setDescription("服务订单支付事件的后端 SDK 接入源");
        return reqVO;
    }

    private AccessSourceDO createAccessSource(Long id) {
        AccessSourceDO accessSource = new AccessSourceDO();
        accessSource.setId(id);
        accessSource.setSourceCode("ORDER_CENTER_SDK");
        accessSource.setAllowedSceneCodes(List.of("ORDER_RISK"));
        accessSource.setStatus(CommonStatusEnum.DISABLE.getStatus());
        return accessSource;
    }

    private SceneDO createScene(String sceneCode) {
        SceneDO scene = new SceneDO();
        scene.setId(1L);
        scene.setSceneCode(sceneCode);
        scene.setSceneName("测试场景");
        return scene;
    }

}
