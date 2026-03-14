package cn.liboshuai.pulsix.module.risk.service.simulation;

import cn.liboshuai.pulsix.engine.demo.DemoFixtures;
import cn.liboshuai.pulsix.engine.json.EngineJson;
import cn.liboshuai.pulsix.engine.simulation.LocalSimulationRunner;
import cn.liboshuai.pulsix.framework.common.util.json.JsonUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.simulation.vo.SimulationReportRespVO;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationReportKernelViewBuilderTest {

    private final LocalSimulationRunner simulationRunner = new LocalSimulationRunner();

    private final SimulationReportKernelViewBuilder builder = new SimulationReportKernelViewBuilder();

    @Test
    void shouldProjectKernelFieldsFromSimulationReportJson() {
        LocalSimulationRunner.SimulationReport kernelReport = simulationRunner.simulate(
                DemoFixtures.demoEnvelope(),
                List.of(DemoFixtures.blacklistedEvent()));
        SimulationReportRespVO respVO = new SimulationReportRespVO();
        respVO.setResultJson(JsonUtils.parseObject(EngineJson.write(kernelReport), new TypeReference<Map<String, Object>>() {
        }));

        builder.apply(respVO);

        assertThat(respVO.getSnapshotId()).isEqualTo("TRADE_RISK_v12");
        assertThat(respVO.getUsedVersion()).isEqualTo(12);
        assertThat(respVO.getEventCount()).isEqualTo(1);
        assertThat(respVO.getOverridesApplied()).isFalse();
        assertThat(respVO.getFinalAction()).isEqualTo("REJECT");
        assertThat(respVO.getHitReasons()).isNotEmpty();
        assertThat(respVO.getHitRules()).isNotEmpty();
        assertThat(respVO.getFeatureSnapshot()).containsKeys("device_in_blacklist", "user_risk_level");
        assertThat(respVO.getTrace()).isNotEmpty();
        assertThat(respVO.getFinalResult()).containsEntry("finalAction", "REJECT");
        assertThat(respVO.getResults()).hasSize(1);
    }

}
