package cn.liboshuai.pulsix.access.sdk.model;

import cn.liboshuai.pulsix.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PulsixSdkSendRequestTest {

    @Test
    void shouldValidateMinimalRequestSuccessfully() {
        PulsixSdkSendRequest request = PulsixSdkSendRequest.builder()
                .sceneCode("TRADE_RISK")
                .eventCode("TRADE_EVENT")
                .payload("{\"event_id\":\"E1001\"}")
                .build()
                .validate();

        assertThat(request.getMetadata()).isNotNull();
        assertThat(request.getSceneCode()).isEqualTo("TRADE_RISK");
        assertThat(request.getEventCode()).isEqualTo("TRADE_EVENT");
    }

    @Test
    void shouldRejectBlankSceneCode() {
        PulsixSdkSendRequest request = PulsixSdkSendRequest.builder()
                .sceneCode(" ")
                .eventCode("TRADE_EVENT")
                .payload("{}")
                .build();

        assertThatThrownBy(request::validate)
                .isInstanceOf(ServiceException.class)
                .hasMessage("SDK 请求不合法：sceneCode 不能为空");
    }

}
