package cn.liboshuai.pulsix.module.risk.service.dashboard;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo.DashboardQueryReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo.DashboardSummaryRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.dashboard.vo.DashboardTrendPointRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.dashboard.RiskMetricSnapshotDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.dashboard.RiskMetricSnapshotMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final String DEFAULT_GRANULARITY = "1m";
    private static final int DEFAULT_MAX_POINTS = 60;

    private static final String METRIC_EVENT_IN_TOTAL = "EVENT_IN_TOTAL";
    private static final String METRIC_DECISION_TOTAL = "DECISION_TOTAL";
    private static final String METRIC_PASS_RATE = "PASS_RATE";
    private static final String METRIC_REVIEW_RATE = "REVIEW_RATE";
    private static final String METRIC_REJECT_RATE = "REJECT_RATE";
    private static final String METRIC_P95_LATENCY_MS = "P95_LATENCY_MS";

    private static final List<String> DASHBOARD_METRIC_CODES = List.of(
            METRIC_EVENT_IN_TOTAL,
            METRIC_DECISION_TOTAL,
            METRIC_PASS_RATE,
            METRIC_REVIEW_RATE,
            METRIC_REJECT_RATE,
            METRIC_P95_LATENCY_MS
    );

    @Resource
    private RiskMetricSnapshotMapper riskMetricSnapshotMapper;

    @Override
    public DashboardSummaryRespVO getDashboardSummary(DashboardQueryReqVO reqVO) {
        DashboardQueryReqVO queryReqVO = normalizeReqVO(reqVO);
        List<RiskMetricSnapshotDO> snapshots = riskMetricSnapshotMapper.selectListByQuery(queryReqVO, DASHBOARD_METRIC_CODES);
        DashboardSummaryRespVO respVO = createEmptySummary(queryReqVO);
        if (CollUtil.isEmpty(snapshots)) {
            return respVO;
        }

        Map<LocalDateTime, List<RiskMetricSnapshotDO>> groupedSnapshots = snapshots.stream()
                .collect(Collectors.groupingBy(RiskMetricSnapshotDO::getStatTime, LinkedHashMap::new, Collectors.toList()));
        List<LocalDateTime> orderedTimes = new ArrayList<>(groupedSnapshots.keySet());
        if (reqVO == null || reqVO.getStatTime() == null || reqVO.getStatTime().length == 0) {
            orderedTimes = trimLatestTimes(orderedTimes, DEFAULT_MAX_POINTS);
        }

        List<DashboardTrendPointRespVO> trends = new ArrayList<>();
        for (LocalDateTime statTime : orderedTimes) {
            Map<String, BigDecimal> metricMap = aggregateMetrics(groupedSnapshots.getOrDefault(statTime, Collections.emptyList()));
            DashboardTrendPointRespVO trendPoint = new DashboardTrendPointRespVO();
            trendPoint.setStatTime(statTime);
            trendPoint.setEventInTotal(getMetricValue(metricMap, METRIC_EVENT_IN_TOTAL));
            trendPoint.setDecisionTotal(getMetricValue(metricMap, METRIC_DECISION_TOTAL));
            trendPoint.setPassRate(getMetricValue(metricMap, METRIC_PASS_RATE));
            trendPoint.setReviewRate(getMetricValue(metricMap, METRIC_REVIEW_RATE));
            trendPoint.setRejectRate(getMetricValue(metricMap, METRIC_REJECT_RATE));
            trendPoint.setP95LatencyMs(getMetricValue(metricMap, METRIC_P95_LATENCY_MS));
            trends.add(trendPoint);
        }
        respVO.setTrends(trends);
        if (CollUtil.isEmpty(trends)) {
            return respVO;
        }

        DashboardTrendPointRespVO latest = trends.get(trends.size() - 1);
        respVO.setLatestStatTime(latest.getStatTime());
        respVO.setLatestEventInTotal(latest.getEventInTotal());
        respVO.setLatestDecisionTotal(latest.getDecisionTotal());
        respVO.setLatestPassRate(latest.getPassRate());
        respVO.setLatestReviewRate(latest.getReviewRate());
        respVO.setLatestRejectRate(latest.getRejectRate());
        respVO.setLatestP95LatencyMs(latest.getP95LatencyMs());
        return respVO;
    }

    private DashboardQueryReqVO normalizeReqVO(DashboardQueryReqVO reqVO) {
        DashboardQueryReqVO queryReqVO = reqVO == null ? new DashboardQueryReqVO() : reqVO;
        queryReqVO.setSceneCode(trimToNull(queryReqVO.getSceneCode()));
        queryReqVO.setStatGranularity(StrUtil.blankToDefault(trimToNull(queryReqVO.getStatGranularity()), DEFAULT_GRANULARITY));
        return queryReqVO;
    }

    private DashboardSummaryRespVO createEmptySummary(DashboardQueryReqVO reqVO) {
        DashboardSummaryRespVO respVO = new DashboardSummaryRespVO();
        respVO.setSceneCode(reqVO.getSceneCode());
        respVO.setStatGranularity(reqVO.getStatGranularity());
        respVO.setLatestEventInTotal(BigDecimal.ZERO);
        respVO.setLatestDecisionTotal(BigDecimal.ZERO);
        respVO.setLatestPassRate(BigDecimal.ZERO);
        respVO.setLatestReviewRate(BigDecimal.ZERO);
        respVO.setLatestRejectRate(BigDecimal.ZERO);
        respVO.setLatestP95LatencyMs(BigDecimal.ZERO);
        respVO.setTrends(Collections.emptyList());
        return respVO;
    }

    private List<LocalDateTime> trimLatestTimes(List<LocalDateTime> orderedTimes, int maxPoints) {
        if (CollUtil.isEmpty(orderedTimes) || orderedTimes.size() <= maxPoints) {
            return orderedTimes;
        }
        return new ArrayList<>(orderedTimes.subList(orderedTimes.size() - maxPoints, orderedTimes.size()));
    }

    private Map<String, BigDecimal> aggregateMetrics(List<RiskMetricSnapshotDO> snapshots) {
        Map<String, BigDecimal> metricMap = new LinkedHashMap<>();
        for (RiskMetricSnapshotDO snapshot : snapshots) {
            if (snapshot == null || StrUtil.isBlank(snapshot.getMetricCode())) {
                continue;
            }
            metricMap.merge(snapshot.getMetricCode().trim(),
                    snapshot.getMetricValue() == null ? BigDecimal.ZERO : snapshot.getMetricValue(),
                    BigDecimal::add);
        }
        return metricMap;
    }

    private BigDecimal getMetricValue(Map<String, BigDecimal> metricMap, String metricCode) {
        return metricMap.getOrDefault(metricCode, BigDecimal.ZERO);
    }

    private String trimToNull(String text) {
        return StrUtil.emptyToNull(StrUtil.trim(text));
    }

}
