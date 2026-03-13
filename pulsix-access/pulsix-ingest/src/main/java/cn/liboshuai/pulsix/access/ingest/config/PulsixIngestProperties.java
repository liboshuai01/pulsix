package cn.liboshuai.pulsix.access.ingest.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "pulsix.access.ingest")
@Validated
@Data
public class PulsixIngestProperties {

    @NotBlank(message = "时区不能为空")
    private String zoneId = "Asia/Shanghai";

    @Valid
    @NotNull
    private Http http = new Http();

    @Valid
    @NotNull
    private Netty netty = new Netty();

    @Valid
    @NotNull
    private Kafka kafka = new Kafka();

    @Valid
    @NotNull
    private ConfigCache configCache = new ConfigCache();

    @Data
    public static class Http {

        @NotNull(message = "HTTP 开关不能为空")
        private Boolean enabled = true;

        @NotBlank(message = "HTTP 接口路径不能为空")
        private String path = "/api/access/ingest/events";

        @NotBlank(message = "Beacon 接口路径不能为空")
        private String beaconPath = "/api/access/ingest/beacon";

        @NotNull(message = "最大报文大小不能为空")
        @Min(value = 1024, message = "最大报文大小不能小于 1024")
        private Integer maxPayloadBytes = 256 * 1024;

    }

    @Data
    public static class Netty {

        @NotNull(message = "Netty 开关不能为空")
        private Boolean enabled = true;

        @NotNull(message = "Netty 监听端口不能为空")
        @Min(value = 1, message = "Netty 监听端口必须大于 0")
        private Integer port = 19100;

        @NotNull(message = "Boss 线程数不能为空")
        @Min(value = 1, message = "Boss 线程数不能小于 1")
        private Integer bossThreads = 1;

        @NotNull(message = "Worker 线程数不能为空")
        @Min(value = 0, message = "Worker 线程数不能小于 0")
        private Integer workerThreads = 0;

        @NotNull(message = "最大帧长度不能为空")
        @Min(value = 1024, message = "最大帧长度不能小于 1024")
        private Integer maxFrameLength = 256 * 1024;

        @NotNull(message = "空闲超时不能为空")
        @Min(value = 1, message = "空闲超时不能小于 1 秒")
        private Integer idleTimeoutSeconds = 60;

    }

    @Data
    public static class Kafka {

        @NotNull(message = "Kafka 开关不能为空")
        private Boolean enabled = true;

        @NotBlank(message = "标准事件 Topic 不能为空")
        private String standardTopicName = "pulsix.event.standard";

        @NotBlank(message = "DLQ Topic 不能为空")
        private String dlqTopicName = "pulsix.event.dlq";

        @NotBlank(message = "接入错误 Topic 不能为空")
        private String errorTopicName = "pulsix.ingest.error";

        @NotNull(message = "Kafka 发送超时不能为空")
        @Min(value = 1, message = "Kafka 发送超时必须大于 0")
        private Integer sendTimeoutMillis = 5000;

    }

    @Data
    public static class ConfigCache {

        @NotNull(message = "配置刷新间隔不能为空")
        @Min(value = 1, message = "配置刷新间隔必须大于 0")
        private Integer refreshIntervalSeconds = 30;

        @NotNull(message = "接入源缓存上限不能为空")
        @Min(value = 1, message = "接入源缓存上限必须大于 0")
        private Integer maxSourceEntries = 1000;

    }

}
