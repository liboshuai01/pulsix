package cn.liboshuai.pulsix.module.risk.service.simulation;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationCasePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationCaseRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationCaseSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationExecuteReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationReportRespVO;

public interface RiskSimulationService {

    Long createSimulationCase(SimulationCaseSaveReqVO createReqVO);

    void updateSimulationCase(SimulationCaseSaveReqVO updateReqVO);

    void deleteSimulationCase(Long id);

    SimulationCaseRespVO getSimulationCase(Long id);

    PageResult<SimulationCaseRespVO> getSimulationCasePage(SimulationCasePageReqVO pageReqVO);

    SimulationReportRespVO executeSimulation(SimulationExecuteReqVO reqVO);

    SimulationReportRespVO getSimulationReport(Long id);

}
