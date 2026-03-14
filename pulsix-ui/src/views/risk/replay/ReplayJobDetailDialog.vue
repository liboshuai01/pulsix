<template>
  <Dialog v-model="dialogVisible" title="回放任务详情" width="92%" :fullscreen="false" :scroll="true" max-height="80vh">
    <div v-loading="detailLoading">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="任务编码">{{ detailData.jobCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属场景">{{ detailData.sceneCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">
          <el-tag :type="getReplayJobStatusTag(detailData.jobStatus)" effect="plain">
            {{ getReplayJobStatusLabel(detailData.jobStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="基线版本">{{ formatVersion(detailData.baselineVersionNo) }}</el-descriptions-item>
        <el-descriptions-item label="候选版本">{{ formatVersion(detailData.targetVersionNo) }}</el-descriptions-item>
        <el-descriptions-item label="输入源">{{ getReplayInputSourceTypeLabel(detailData.inputSourceType) }}</el-descriptions-item>
        <el-descriptions-item label="基线快照">{{ formatSnapshot(baselineSnapshot, detailData.baselineVersionNo) }}</el-descriptions-item>
        <el-descriptions-item label="候选快照">{{ formatSnapshot(candidateSnapshot, detailData.targetVersionNo) }}</el-descriptions-item>
        <el-descriptions-item label="差异占比">{{ formatReplayRate(changedEventCount, eventCount) }}</el-descriptions-item>
        <el-descriptions-item label="输入引用" :span="2">{{ detailData.inputRef || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件总数">{{ eventCount }}</el-descriptions-item>
        <el-descriptions-item label="差异事件数">{{ changedEventCount }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detailData.startedAt ? formatDate(detailData.startedAt) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detailData.finishedAt ? formatDate(detailData.finishedAt) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">
          <div class="whitespace-pre-wrap leading-22px">{{ detailData.remark || '-' }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <div class="mt-16px grid grid-cols-4 gap-12px">
        <el-card shadow="hover">
          <div class="text-14px text-#909399">基线动作分布</div>
          <div class="mt-6px text-12px text-[var(--el-text-color-secondary)]">{{ formatSnapshot(baselineSnapshot, detailData.baselineVersionNo) }}</div>
          <div class="mt-10px leading-28px">{{ formatActionCounts(baselineActionCounts) }}</div>
        </el-card>
        <el-card shadow="hover">
          <div class="text-14px text-#909399">候选动作分布</div>
          <div class="mt-6px text-12px text-[var(--el-text-color-secondary)]">{{ formatSnapshot(candidateSnapshot, detailData.targetVersionNo) }}</div>
          <div class="mt-10px leading-28px">{{ formatActionCounts(candidateActionCounts) }}</div>
        </el-card>
        <el-card shadow="hover">
          <div class="text-14px text-#909399">基线命中事件数</div>
          <div class="mt-12px text-28px font-700">{{ baselineMatchedEventCount }}</div>
        </el-card>
        <el-card shadow="hover">
          <div class="text-14px text-#909399">候选命中事件数</div>
          <div class="mt-12px text-28px font-700">{{ candidateMatchedEventCount }}</div>
        </el-card>
      </div>

      <ContentWrap class="mt-16px">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="差异样例" name="diff">
            <div class="mb-12px flex flex-wrap gap-8px">
              <el-tag v-for="item in topChangeTypeList" :key="item.code" effect="plain">
                {{ getReplayChangeTypeLabel(item.code) }} × {{ item.count }}
              </el-tag>
              <span v-if="topChangeTypeList.length === 0" class="text-13px text-[var(--el-text-color-secondary)]">暂无差异类型统计</span>
            </div>
            <el-table :data="differenceRows" max-height="340">
              <el-table-column label="事件序号" align="center" prop="eventIndex" width="90" />
              <el-table-column label="事件编号" align="center" prop="eventId" min-width="170" show-overflow-tooltip />
              <el-table-column label="链路号" align="center" prop="traceId" min-width="170" show-overflow-tooltip />
              <el-table-column label="变化类型" align="center" min-width="220">
                <template #default="scope">
                  <el-space wrap>
                    <el-tag v-for="item in ensureStringArray(scope.row.changeTypes)" :key="item" effect="plain">
                      {{ getReplayChangeTypeLabel(item) }}
                    </el-tag>
                    <span v-if="ensureStringArray(scope.row.changeTypes).length === 0">-</span>
                  </el-space>
                </template>
              </el-table-column>
              <el-table-column label="基线动作" align="center" width="120">
                <template #default="scope">
                  <el-tag :type="getRiskActionTag(extractResultAction(scope.row, 'baseline'))" effect="plain">
                    {{ getRiskActionLabel(extractResultAction(scope.row, 'baseline')) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="候选动作" align="center" width="120">
                <template #default="scope">
                  <el-tag :type="getRiskActionTag(extractResultAction(scope.row, 'candidate'))" effect="plain">
                    {{ getRiskActionLabel(extractResultAction(scope.row, 'candidate')) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="基线分值" align="center" width="100">
                <template #default="scope">{{ extractResultScore(scope.row, 'baseline') }}</template>
              </el-table-column>
              <el-table-column label="候选分值" align="center" width="100">
                <template #default="scope">{{ extractResultScore(scope.row, 'candidate') }}</template>
              </el-table-column>
              <el-table-column label="基线命中规则" align="center" min-width="180">
                <template #default="scope">
                  <el-space wrap>
                    <el-tag v-for="item in extractResultHitRules(scope.row, 'baseline')" :key="item" effect="plain">{{ item }}</el-tag>
                    <span v-if="extractResultHitRules(scope.row, 'baseline').length === 0">-</span>
                  </el-space>
                </template>
              </el-table-column>
              <el-table-column label="候选命中规则" align="center" min-width="180">
                <template #default="scope">
                  <el-space wrap>
                    <el-tag v-for="item in extractResultHitRules(scope.row, 'candidate')" :key="item" effect="plain">{{ item }}</el-tag>
                    <span v-if="extractResultHitRules(scope.row, 'candidate').length === 0">-</span>
                  </el-space>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="Kernel Diff" name="kernel-diff">
            <JsonEditor :model-value="kernelDiffJson" mode="view" height="420px" />
          </el-tab-pane>
          <el-tab-pane label="摘要 JSON" name="summary">
            <JsonEditor :model-value="summaryJson" mode="view" height="420px" />
          </el-tab-pane>
          <el-tab-pane v-if="hasGoldenCase" label="Golden Case" name="golden-case">
            <JsonEditor :model-value="goldenCase" mode="view" height="420px" />
          </el-tab-pane>
          <el-tab-pane v-if="hasGoldenVerification" label="Golden 校验" name="golden-verify">
            <JsonEditor :model-value="goldenVerification" mode="view" height="420px" />
          </el-tab-pane>
        </el-tabs>
      </ContentWrap>
    </div>
  </Dialog>
</template>

<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import * as ReplayApi from '@/api/risk/replay'
import {
  ensureObject,
  ensureObjectArray,
  ensureStringArray,
  formatReplayRate,
  getReplayChangeTypeLabel,
  getReplayInputSourceTypeLabel,
  getReplayJobStatusLabel,
  getReplayJobStatusTag,
  getRiskActionLabel,
  getRiskActionTag
} from './constants'

defineOptions({ name: 'RiskReplayJobDetailDialog' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const activeTab = ref('diff')

const createDefaultDetailData = (): ReplayApi.ReplayJobDetailVO => ({
  id: undefined,
  jobCode: '',
  sceneCode: '',
  baselineVersionNo: 0,
  targetVersionNo: 0,
  inputSourceType: 'DECISION_LOG_EXPORT',
  inputRef: '',
  jobStatus: 'INIT',
  eventTotalCount: 0,
  diffEventCount: 0,
  remark: '',
  startedAt: undefined,
  finishedAt: undefined,
  creator: '',
  createTime: undefined,
  updater: '',
  updateTime: undefined,
  summaryJson: {},
  sampleDiffJson: [],
  baseline: {},
  candidate: {},
  baselineSummary: {},
  candidateSummary: {},
  differences: [],
  topChangeTypes: {},
  goldenCase: {},
  goldenVerification: {}
})

const detailData = ref<ReplayApi.ReplayJobDetailVO>(createDefaultDetailData())

const normalizeDetailData = (data?: Partial<ReplayApi.ReplayJobDetailVO>): ReplayApi.ReplayJobDetailVO => ({
  ...createDefaultDetailData(),
  ...data,
  summaryJson: ensureObject(data?.summaryJson),
  sampleDiffJson: ensureObjectArray(data?.sampleDiffJson),
  baseline: ensureObject(data?.baseline),
  candidate: ensureObject(data?.candidate),
  baselineSummary: ensureObject(data?.baselineSummary),
  candidateSummary: ensureObject(data?.candidateSummary),
  differences: ensureObjectArray(data?.differences),
  topChangeTypes: ensureObject(data?.topChangeTypes),
  goldenCase: ensureObject(data?.goldenCase),
  goldenVerification: ensureObject(data?.goldenVerification)
})

const summaryJson = computed(() => ensureObject(detailData.value.summaryJson))

const extractSnapshotRef = (value: Record<string, any>) => {
  const result: Record<string, any> = {}
  if (value.snapshotId) {
    result.snapshotId = value.snapshotId
  }
  if (value.version != null && value.version !== '') {
    result.version = Number(value.version)
  }
  if (value.checksum) {
    result.checksum = value.checksum
  }
  return result
}

const extractReplaySummary = (value: Record<string, any>) => {
  const result: Record<string, any> = {
    finalActionCounts: ensureObject(value.finalActionCounts)
  }
  if (value.matchedEventCount != null && value.matchedEventCount !== '') {
    result.matchedEventCount = Number(value.matchedEventCount)
  }
  return result
}

const buildLegacyResult = (row: Record<string, any>, prefix: 'baseline' | 'candidate') => {
  const current = ensureObject(row[`${prefix}Result`])
  if (Object.keys(current).length > 0) {
    return current
  }
  const result: Record<string, any> = {}
  const finalAction = String(row[`${prefix}Action`] || '')
  const finalScore = row[`${prefix}FinalScore`]
  const hitRules = ensureStringArray(row[`${prefix}HitRules`]).map((ruleCode) => ({ ruleCode }))
  const hitReasons = ensureStringArray(row[`${prefix}HitReasons`])
  if (finalAction) {
    result.finalAction = finalAction
  }
  if (finalScore != null && finalScore !== '') {
    result.finalScore = Number(finalScore)
  }
  if (hitRules.length > 0) {
    result.hitRules = hitRules
  }
  if (hitReasons.length > 0) {
    result.hitReasons = hitReasons
  }
  return result
}

const projectLegacyDifference = (row: Record<string, any>) => ({
  eventIndex: row.eventIndex,
  eventId: row.eventId,
  traceId: row.traceId,
  changeTypes: ensureStringArray(row.changeTypes),
  baselineResult: buildLegacyResult(row, 'baseline'),
  candidateResult: buildLegacyResult(row, 'candidate')
})

const eventCount = computed(() => Number(detailData.value.eventCount ?? summaryJson.value.eventCount ?? detailData.value.eventTotalCount ?? 0))
const changedEventCount = computed(() => Number(detailData.value.changedEventCount ?? summaryJson.value.changedEventCount ?? detailData.value.diffEventCount ?? 0))
const baselineSnapshot = computed(() => {
  const direct = extractSnapshotRef(ensureObject(detailData.value.baseline))
  return Object.keys(direct).length > 0 ? direct : extractSnapshotRef(ensureObject(summaryJson.value.baseline))
})
const candidateSnapshot = computed(() => {
  const direct = extractSnapshotRef(ensureObject(detailData.value.candidate))
  return Object.keys(direct).length > 0 ? direct : extractSnapshotRef(ensureObject(summaryJson.value.candidate))
})
const baselineSummary = computed(() => {
  const direct = extractReplaySummary(ensureObject(detailData.value.baselineSummary))
  return Object.keys(direct.finalActionCounts).length > 0 || direct.matchedEventCount != null
    ? direct
    : extractReplaySummary(ensureObject(summaryJson.value.baseline))
})
const candidateSummary = computed(() => {
  const direct = extractReplaySummary(ensureObject(detailData.value.candidateSummary))
  return Object.keys(direct.finalActionCounts).length > 0 || direct.matchedEventCount != null
    ? direct
    : extractReplaySummary(ensureObject(summaryJson.value.candidate))
})
const baselineActionCounts = computed(() => ensureObject(baselineSummary.value.finalActionCounts))
const candidateActionCounts = computed(() => ensureObject(candidateSummary.value.finalActionCounts))
const baselineMatchedEventCount = computed(() => Number(baselineSummary.value.matchedEventCount || 0))
const candidateMatchedEventCount = computed(() => Number(candidateSummary.value.matchedEventCount || 0))
const differenceRows = computed(() => {
  const direct = ensureObjectArray(detailData.value.differences)
  return direct.length > 0 ? direct : ensureObjectArray(detailData.value.sampleDiffJson).map(projectLegacyDifference)
})
const topChangeTypeList = computed(() => {
  const value = Object.keys(ensureObject(detailData.value.topChangeTypes)).length > 0
    ? ensureObject(detailData.value.topChangeTypes)
    : ensureObject(summaryJson.value.topChangeTypes)
  return Object.entries(value).map(([code, count]) => ({ code, count: Number(count || 0) }))
})
const kernelDiffJson = computed(() => ({
  sceneCode: detailData.value.sceneCode || summaryJson.value.sceneCode || undefined,
  eventCount: eventCount.value,
  baseline: baselineSnapshot.value,
  candidate: candidateSnapshot.value,
  baselineSummary: baselineSummary.value,
  candidateSummary: candidateSummary.value,
  changedEventCount: changedEventCount.value,
  differences: differenceRows.value
}))
const goldenCase = computed(() => ensureObject(detailData.value.goldenCase))
const goldenVerification = computed(() => ensureObject(detailData.value.goldenVerification))
const hasGoldenCase = computed(() => Object.keys(goldenCase.value).length > 0)
const hasGoldenVerification = computed(() => Object.keys(goldenVerification.value).length > 0)

const formatVersion = (version?: number) => {
  return version ? `v${version}` : '-'
}

const formatSnapshot = (snapshot: Record<string, any>, fallbackVersion?: number) => {
  const parts: string[] = []
  if (snapshot.snapshotId) {
    parts.push(String(snapshot.snapshotId))
  }
  const version = Number(snapshot.version || fallbackVersion || 0)
  if (version > 0) {
    parts.push(formatVersion(version))
  }
  return parts.length > 0 ? parts.join(' / ') : '-'
}

const formatActionCounts = (value: Record<string, any>) => {
  const entries = Object.entries(value || {})
  if (entries.length === 0) {
    return '-'
  }
  return entries.map(([action, count]) => `${action}: ${count}`).join(' / ')
}

const extractResult = (row: Record<string, any>, prefix: 'baseline' | 'candidate') => {
  return buildLegacyResult(row, prefix)
}

const extractResultAction = (row: Record<string, any>, prefix: 'baseline' | 'candidate') => {
  return String(extractResult(row, prefix).finalAction || '')
}

const extractResultScore = (row: Record<string, any>, prefix: 'baseline' | 'candidate') => {
  const value = extractResult(row, prefix).finalScore
  return value == null || value === '' ? '-' : value
}

const extractResultHitRules = (row: Record<string, any>, prefix: 'baseline' | 'candidate') => {
  const hitRules = ensureObjectArray(extractResult(row, prefix).hitRules)
    .map((item) => String(item.ruleCode || item.code || ''))
    .filter(Boolean)
  if (hitRules.length > 0) {
    return hitRules
  }
  return ensureStringArray(row[`${prefix}HitRules`] ?? row[`${prefix}HitRuleCodes`])
}

const open = async (id: number) => {
  dialogVisible.value = true
  detailLoading.value = true
  activeTab.value = 'diff'
  try {
    detailData.value = normalizeDetailData(await ReplayApi.getReplayJob(id))
  } finally {
    detailLoading.value = false
  }
}

const openWithData = (data: ReplayApi.ReplayJobDetailVO) => {
  dialogVisible.value = true
  detailLoading.value = false
  activeTab.value = 'diff'
  detailData.value = normalizeDetailData(data)
}

defineExpose({ open, openWithData, ensureStringArray })
</script>
