<template>
  <Dialog
    v-model="dialogVisible"
    title="仿真报告"
    width="90%"
    :fullscreen="false"
    :scroll="true"
    max-height="78vh"
  >
    <div v-loading="detailLoading">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="所属场景">{{ detailData.sceneCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用例编码">{{ detailData.caseCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用例名称">{{ detailData.caseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="执行版本">{{ formatVersion(detailData.versionNo) }}</el-descriptions-item>
        <el-descriptions-item label="快照 ID">{{ detailData.snapshotId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="链路号">{{ detailData.traceId || finalResult.traceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detailData.durationMs ?? '-' }} ms</el-descriptions-item>
        <el-descriptions-item label="使用版本">{{ formatVersion(detailData.usedVersion || detailData.versionNo) }}</el-descriptions-item>
        <el-descriptions-item label="事件数">{{ detailData.eventCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="覆盖注入">
          {{ detailData.overridesApplied == null ? '-' : detailData.overridesApplied ? '是' : '否' }}
        </el-descriptions-item>
        <el-descriptions-item label="最终动作">
          <el-tag :type="getRiskActionTag(finalAction)" effect="plain">
            {{ getRiskActionLabel(finalAction) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="断言结果">
          <el-tag :type="getSimulationPassTag(detailData.passFlag)" effect="plain">
            {{ getSimulationPassLabel(detailData.passFlag) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报告时间">
          {{ detailData.createTime ? formatDate(detailData.createTime) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="Checksum" :span="2">{{ detailData.checksum || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最终分数">{{ finalResult.finalScore ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="累计分数">{{ finalResult.totalScore ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="最终原因">{{ finalResult.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="命中规则">
          <el-space wrap>
            <el-tag
              v-for="item in hitRuleCodes"
              :key="item"
              :type="getRiskActionTag(finalAction)"
              effect="plain"
            >
              {{ item }}
            </el-tag>
            <span v-if="hitRuleCodes.length === 0">-</span>
          </el-space>
        </el-descriptions-item>
        <el-descriptions-item label="命中原因" :span="2">
          <div v-if="hitReasons.length > 0" class="whitespace-pre-wrap leading-22px">
            {{ hitReasons.join('；') }}
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>

      <ContentWrap class="mt-16px">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="特征快照" name="feature">
            <JsonEditor :model-value="featureSnapshot" mode="view" height="420px" />
          </el-tab-pane>
          <el-tab-pane label="命中链路" name="trace">
            <el-table :data="matchedRules" size="small" max-height="220">
              <el-table-column label="规则编码" align="center" prop="ruleCode" min-width="120" />
              <el-table-column label="规则名称" align="center" prop="ruleName" min-width="180" />
              <el-table-column label="动作" align="center" min-width="100">
                <template #default="scope">
                  <el-tag :type="getRiskActionTag(scope.row.action)" effect="plain">
                    {{ getRiskActionLabel(scope.row.action) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="分数" align="center" prop="score" width="90" />
              <el-table-column label="命中原因" align="center" prop="reason" min-width="220" show-overflow-tooltip />
            </el-table>

            <el-divider content-position="left">Trace</el-divider>
            <el-scrollbar height="300px">
              <div v-if="traceLines.length === 0" class="text-13px text-[var(--el-text-color-secondary)]">
                暂无命中链路
              </div>
              <div
                v-for="(item, index) in traceLines"
                :key="`${index}-${item}`"
                class="mb-8px rounded bg-[var(--el-fill-color-light)] px-12px py-8px font-mono text-12px leading-20px"
              >
                {{ item }}
              </div>
            </el-scrollbar>
          </el-tab-pane>
          <el-tab-pane label="逐事件结果" name="results">
            <JsonEditor :model-value="resultsJson" mode="view" height="420px" />
          </el-tab-pane>
          <el-tab-pane label="完整报告" name="raw">
            <JsonEditor :model-value="resultJson" mode="view" height="420px" />
          </el-tab-pane>
        </el-tabs>
      </ContentWrap>
    </div>
  </Dialog>
</template>

<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import * as SimulationApi from '@/api/risk/simulation'
import {
  extractSimulationFeatureSnapshot,
  extractSimulationFinalAction,
  extractSimulationFinalResult,
  extractSimulationHitRuleCodes,
  extractSimulationMatchedRules,
  extractSimulationResults,
  extractSimulationTrace,
  getRiskActionLabel,
  getRiskActionTag,
  getSimulationPassLabel,
  getSimulationPassTag
} from './constants'

defineOptions({ name: 'RiskSimulationReportDialog' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const activeTab = ref('feature')

const createDefaultDetailData = (): SimulationApi.SimulationReportVO => ({
  id: undefined,
  caseId: 0,
  caseCode: '',
  caseName: '',
  sceneCode: '',
  versionNo: 0,
  traceId: '',
  resultJson: {},
  passFlag: undefined,
  durationMs: undefined,
  creator: '',
  createTime: undefined,
  updater: '',
  updateTime: undefined
})

const ensureObject = (value: unknown): Record<string, any> => {
  return value && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, any>) : {}
}

const normalizeReportData = (data?: Partial<SimulationApi.SimulationReportVO>): SimulationApi.SimulationReportVO => ({
  ...createDefaultDetailData(),
  ...data,
  resultJson: ensureObject(data?.resultJson),
  finalResult: ensureObject(data?.finalResult),
  results: Array.isArray(data?.results) ? data?.results : [],
  hitRules: Array.isArray(data?.hitRules) ? data?.hitRules : [],
  hitReasons: Array.isArray(data?.hitReasons) ? data?.hitReasons : [],
  featureSnapshot: ensureObject(data?.featureSnapshot),
  trace: Array.isArray(data?.trace) ? data?.trace : []
})

const detailData = ref<SimulationApi.SimulationReportVO>(createDefaultDetailData())

const resultJson = computed(() => ensureObject(detailData.value.resultJson))
const finalResult = computed(() => extractSimulationFinalResult(detailData.value))
const finalAction = computed(() => extractSimulationFinalAction(detailData.value))
const matchedRules = computed(() => extractSimulationMatchedRules(detailData.value))
const hitRuleCodes = computed(() => extractSimulationHitRuleCodes(detailData.value))
const featureSnapshot = computed(() => extractSimulationFeatureSnapshot(detailData.value))
const traceLines = computed(() => extractSimulationTrace(detailData.value))
const resultsJson = computed(() => extractSimulationResults(detailData.value))
const hitReasons = computed(() => {
  const value = detailData.value.hitReasons?.length ? detailData.value.hitReasons : finalResult.value.hitReasons
  return Array.isArray(value) ? value.map((item) => String(item)).filter(Boolean) : []
})

const formatVersion = (version?: number) => {
  return version ? `v${version}` : '-'
}

const open = async (id: number) => {
  dialogVisible.value = true
  detailLoading.value = true
  activeTab.value = 'feature'
  try {
    const data = await SimulationApi.getSimulationReport(id)
    detailData.value = normalizeReportData(data)
  } finally {
    detailLoading.value = false
  }
}

const openWithData = (data: SimulationApi.SimulationReportVO) => {
  dialogVisible.value = true
  detailLoading.value = false
  activeTab.value = 'feature'
  detailData.value = normalizeReportData(data)
}

defineExpose({ open, openWithData })
</script>
