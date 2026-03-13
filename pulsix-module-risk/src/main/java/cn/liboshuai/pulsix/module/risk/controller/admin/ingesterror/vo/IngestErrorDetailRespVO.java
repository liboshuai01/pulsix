package cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Schema(description = "管理后台 - 接入异常详情 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class IngestErrorDetailRespVO extends IngestErrorRespVO {

    @Schema(description = "原始报文 JSON")
    private Map<String, Object> rawPayloadJson;

    @Schema(description = "标准化报文 JSON")
    private Map<String, Object> standardPayloadJson;

}
