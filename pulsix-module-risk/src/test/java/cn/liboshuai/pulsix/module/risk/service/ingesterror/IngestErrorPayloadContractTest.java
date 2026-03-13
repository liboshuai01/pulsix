package cn.liboshuai.pulsix.module.risk.service.ingesterror;

import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo.IngestErrorDetailRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingesterror.IngestErrorLogDO;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IngestErrorPayloadContractTest {

    @Test
    void shouldRetainStringAndObjectPayloadsWhenMappingDetailVo() {
        IngestErrorLogDO logDO = new IngestErrorLogDO();
        logDO.setRawPayloadJson("{\"event_id\":\"raw_trade_bad_8103\"}");
        logDO.setStandardPayloadJson(Map.of("eventId", "raw_trade_bad_8103"));

        IngestErrorDetailRespVO respVO = BeanUtils.toBean(logDO, IngestErrorDetailRespVO.class);

        assertThat(respVO.getRawPayloadJson()).isEqualTo("{\"event_id\":\"raw_trade_bad_8103\"}");
        assertThat(respVO.getStandardPayloadJson()).isEqualTo(Map.of("eventId", "raw_trade_bad_8103"));
    }

}
