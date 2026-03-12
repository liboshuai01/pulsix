package cn.liboshuai.pulsix.module.risk.controller.admin.eventsample.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 事件样例 Response VO")
@Data
public class EventSampleRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "所属场景编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_RISK")
    private String sceneCode;

    @Schema(description = "所属事件编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_EVENT")
    private String eventCode;

    @Schema(description = "样例编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRADE_STD_SUCCESS")
    private String sampleCode;

    @Schema(description = "样例名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "交易成功标准样例")
    private String sampleName;

    @Schema(description = "样例类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "STANDARD")
    private String sampleType;

    @Schema(description = "来源接入源编码", example = "trade_http_demo")
    private String sourceCode;

    @Schema(description = "样例报文 JSON")
    private Map<String, Object> sampleJson;

    @Schema(description = "样例说明")
    private String description;

    @Schema(description = "排序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer sortNo;

    @Schema(description = "状态，见 CommonStatusEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
