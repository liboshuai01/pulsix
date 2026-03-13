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
        <el-descriptions-item label="目标版本">{{ formatVersion(detailData.targetVersionNo) }}</el-descriptions-item>
        <el-descriptions-item label="输入源">{{ getReplayInputSourceTypeLabel(detailData.inputSourceType) }}</el-descriptions-item>
        <el-descriptions-item label="输入引用" :span="2">{{ detailData.inputRef || '-' }}</el-descriptions-item>
        <el-descriptions-item label="差异占比">{{ formatReplayRate(detailData.diffEventCount, detailData.eventTotalCount) }}</el-descriptions-item>
        <el-descriptions-item label="事件总数">{{ detailData.eventTotalCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="差异事件数">{{ detailData.diffEventCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detailData.startedAt ? formatDate(detailData.startedAt) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detailData.finishedAt ? formatDate(detailData.finishedAt) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">
          <div class="whitespace-pre-wrap leading-22px">{{ detailData.remark || '-' }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <div class="mt-16px grid grid-cols-4 gap-12px">
        <el-card shadow="hover">
          <div class="text-14px text-#909399">基线动作分布</div>
          <div class="mt-10px leading-28px">{{ formatActionCounts(baselineActionCounts) }}</div>
        </el-card>
        <el-card shadow="hover">
          <div class="text-14px text-#909399">目标动作分布</div>
          <div class="mt-10px leading-28px">{{ formatActionCounts(candidateActionCounts) }}</div>
        </el-card>
        <el-card shadow="hover">
          <div class="text-14px text-#909399">基线命中事件数</div>
          <div class="mt-12px text-28px font-700">{{ baselineMatchedEventCount }}</div>
        </el-card>
        <el-card shadow="hover">
          <div class="text-14px text-#909399">目标命中事件数</div>
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
            <el-table :data="sampleDiffList" max-height="340">
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
                  <el-tag :type="getRiskActionTag(String(scope.row.baselineAction || ''))" effect="plain">
                    {{ getRiskActionLabel(String(scope.row.baselineAction || '')) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="目标动作" align="center" width="120">
                <template #default="scope">
                  <el-tag :type="getRiskActionTag(String(scope.row.candidateAction || ''))" effect="plain">
                    {{ getRiskActionLabel(String(scope.row.candidateAction || '')) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="基线命中规则" align="center" min-width="180">
                <template #default="scope">
                  <el-space wrap>
                    <el-tag v-for="item in ensureStringArray(scope.row.baselineHitRules)" :key="item" effect="plain">{{ item }}</el-tag>
                    <span v-if="ensureStringArray(scope.row.baselineHitRules).length === 0">-</span>
                  </el-space>
                </template>
              </el-table-column>
              <el-table-column label="目标命中规则" align="center" min-width="180">
                <template #default="scope">
                  <el-space wrap>
                    <el-tag v-for="item in ensureStringArray(scope.row.candidateHitRules)" :key="item" effect="plain">{{ item }}</el-tag>
                    <span v-if="ensureStringArray(scope.row.candidateHitRules).length === 0">-</span>
                  </el-space>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="摘要 JSON" name="summary">
            <JsonEditor :model-value="summaryJson" mode="view" height="420px" />
          </el-tab-pane>
          <el-tab-pane label="差异 JSON" name="raw-diff">
            <JsonEditor :model-value="sampleDiffList" mode="view" height="420px" />
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
  sampleDiffJson: []
})

const detailData = ref<ReplayApi.ReplayJobDetailVO>(createDefaultDetailData())

const normalizeDetailData = (data?: Partial<ReplayApi.ReplayJobDetailVO>): ReplayApi.ReplayJobDetailVO => ({
  ...createDefaultDetailData(),
  ...data,
  summaryJson: ensureObject(data?.summaryJson),
  sampleDiffJson: ensureObjectArray(data?.sampleDiffJson)
})

const summaryJson = computed(() => ensureObject(detailData.value.summaryJson))
const baselineSummary = computed(() => ensureObject(summaryJson.value.baseline))
const candidateSummary = computed(() => ensureObject(summaryJson.value.candidate))
const baselineActionCounts = computed(() => ensureObject(baselineSummary.value.finalActionCounts))
const candidateActionCounts = computed(() => ensureObject(candidateSummary.value.finalActionCounts))
const baselineMatchedEventCount = computed(() => Number(baselineSummary.value.matchedEventCount || 0))
const candidateMatchedEventCount = computed(() => Number(candidateSummary.value.matchedEventCount || 0))
const sampleDiffList = computed(() => ensureObjectArray(detailData.value.sampleDiffJson))
const topChangeTypeList = computed(() => {
  const value = ensureObject(summaryJson.value.topChangeTypes)
  return Object.entries(value).map(([code, count]) => ({ code, count: Number(count || 0) }))
})

const formatVersion = (version?: number) => {
  return version ? `v${version}` : '-'
}

const formatActionCounts = (value: Record<string, any>) => {
  const entries = Object.entries(value || {})
  if (entries.length === 0) {
    return '-'
  }
  return entries.map(([action, count]) => `${action}: ${count}`).join(' / ')
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
