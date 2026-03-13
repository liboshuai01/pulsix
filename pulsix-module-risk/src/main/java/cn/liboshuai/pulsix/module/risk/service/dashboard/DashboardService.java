package cn.liboshuai.pulsix.module.risk.service.dashboard;

import cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo.DashboardQueryReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo.DashboardSummaryRespVO;

public interface DashboardService {

    DashboardSummaryRespVO getDashboardSummary(DashboardQueryReqVO reqVO);

}
