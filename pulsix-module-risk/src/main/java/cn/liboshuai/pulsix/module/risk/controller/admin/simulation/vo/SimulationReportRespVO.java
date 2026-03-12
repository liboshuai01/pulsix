package cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 仿真报告 Response VO")
@Data
public class SimulationReportRespVO {

    private Long id;

    private Long caseId;

    private String caseCode;

    private String caseName;

    private String sceneCode;

    private Integer versionNo;

    private String traceId;

    private Map<String, Object> resultJson;

    private Integer passFlag;

    private Long durationMs;

    private String creator;

    private LocalDateTime createTime;

    private String updater;

    private LocalDateTime updateTime;

}
