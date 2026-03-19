package cn.liboshuai.pulsix.module.risk.service.scene;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.exception.ServiceException;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.SceneSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import cn.liboshuai.pulsix.module.risk.enums.scene.SceneRuntimeModeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SceneServiceImplTest {

    @Mock
    private SceneMapper sceneMapper;

    @InjectMocks
    private SceneServiceImpl sceneService;

    @Test
    void createScene_defaultsToDisable() {
        SceneSaveReqVO reqVO = new SceneSaveReqVO();
        reqVO.setSceneCode("TEST_RISK");
        reqVO.setSceneName("测试场景");
        reqVO.setRuntimeMode(SceneRuntimeModeEnum.ASYNC_DECISION.name());
        reqVO.setDefaultPolicyCode("TEST_POLICY");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setDescription("测试描述");

        sceneService.createScene(reqVO);

        ArgumentCaptor<SceneDO> captor = ArgumentCaptor.forClass(SceneDO.class);
        verify(sceneMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CommonStatusEnum.DISABLE.getStatus());
    }

    @Test
    void updateScene_preservesExistingStatus() {
        SceneDO scene = new SceneDO();
        scene.setId(10L);
        scene.setSceneCode("ORDER_RISK");
        scene.setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(sceneMapper.selectById(10L)).thenReturn(scene);
        when(sceneMapper.selectBySceneCode("ORDER_RISK")).thenReturn(scene);

        SceneSaveReqVO reqVO = new SceneSaveReqVO();
        reqVO.setId(10L);
        reqVO.setSceneCode("ORDER_RISK");
        reqVO.setSceneName("订单后置风控");
        reqVO.setRuntimeMode(SceneRuntimeModeEnum.ASYNC_DECISION.name());
        reqVO.setDefaultPolicyCode("ORDER_POLICY");
        reqVO.setStatus(CommonStatusEnum.DISABLE.getStatus());
        reqVO.setDescription("更新描述");

        sceneService.updateScene(reqVO);

        ArgumentCaptor<SceneDO> captor = ArgumentCaptor.forClass(SceneDO.class);
        verify(sceneMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CommonStatusEnum.ENABLE.getStatus());
    }

    @Test
    void deleteScene_enabled_rejected() {
        when(sceneMapper.selectById(10L)).thenReturn(createScene(10L, CommonStatusEnum.ENABLE.getStatus()));

        assertThatThrownBy(() -> sceneService.deleteScene(10L))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_004);
        verify(sceneMapper, never()).deleteById(any());
    }

    @Test
    void deleteScene_disabledWithDependency_rejected() {
        when(sceneMapper.selectById(10L)).thenReturn(createScene(10L, CommonStatusEnum.DISABLE.getStatus()));
        when(sceneMapper.selectEventSchemaCountBySceneCode("ORDER_RISK")).thenReturn(1L);

        assertThatThrownBy(() -> sceneService.deleteScene(10L))
                .isInstanceOf(ServiceException.class)
                .extracting("code")
                .isEqualTo(1_003_000_003);
        verify(sceneMapper, never()).deleteById(any());
    }

    @Test
    void deleteScene_disabledWithoutDependency_success() {
        when(sceneMapper.selectById(10L)).thenReturn(createScene(10L, CommonStatusEnum.DISABLE.getStatus()));

        sceneService.deleteScene(10L);

        verify(sceneMapper).deleteById(10L);
    }

    private SceneDO createScene(Long id, Integer status) {
        SceneDO scene = new SceneDO();
        scene.setId(id);
        scene.setSceneCode("ORDER_RISK");
        scene.setSceneName("订单后置风控");
        scene.setStatus(status);
        return scene;
    }

}
