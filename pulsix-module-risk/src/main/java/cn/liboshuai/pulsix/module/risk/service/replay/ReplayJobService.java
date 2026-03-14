package cn.liboshuai.pulsix.module.risk.service.replay;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobCreateReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobDetailRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobExecuteReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.replay.vo.ReplayJobRespVO;

public interface ReplayJobService {

    Long createReplayJob(ReplayJobCreateReqVO createReqVO);

    PageResult<ReplayJobRespVO> getReplayJobPage(ReplayJobPageReqVO pageReqVO);

    ReplayJobDetailRespVO getReplayJob(Long id);

    ReplayJobDetailRespVO executeReplayJob(ReplayJobExecuteReqVO reqVO);

    ReplayJobDetailRespVO captureReplayGoldenCase(ReplayJobExecuteReqVO reqVO);

    ReplayJobDetailRespVO verifyReplayGoldenCase(ReplayJobExecuteReqVO reqVO);

}
