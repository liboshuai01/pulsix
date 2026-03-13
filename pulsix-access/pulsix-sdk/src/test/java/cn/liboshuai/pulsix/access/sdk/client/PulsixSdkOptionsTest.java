package cn.liboshuai.pulsix.access.sdk.client;

import cn.liboshuai.pulsix.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PulsixSdkOptionsTest {

    @Test
    void shouldApplyDefaultsAndValidateSuccessfully() {
        PulsixSdkOptions options = PulsixSdkOptions.builder()
                .sourceCode("trade_sdk_demo")
                .build()
                .validate();

        assertThat(options.getHost()).isEqualTo("127.0.0.1");
        assertThat(options.getPort()).isEqualTo(19100);
        assertThat(options.getConnectTimeoutMillis()).isEqualTo(3000);
        assertThat(options.getRequestTimeoutMillis()).isEqualTo(5000);
        assertThat(options.getMaxBatchSize()).isEqualTo(100);
        assertThat(options.getMaxBufferSize()).isEqualTo(1000);
        assertThat(options.getBatchFlushIntervalMillis()).isEqualTo(100);
        assertThat(options.getMaxRetryCount()).isEqualTo(2);
        assertThat(options.getAutoReconnect()).isTrue();
    }

    @Test
    void shouldRejectBlankSourceCode() {
        PulsixSdkOptions options = PulsixSdkOptions.builder()
                .sourceCode(" ")
                .build();

        assertThatThrownBy(options::validate)
                .isInstanceOf(ServiceException.class)
                .hasMessage("SDK 配置不合法：sourceCode 不能为空");
    }

}
