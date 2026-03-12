<template>
  <Dialog v-model="dialogVisible" title="决策日志详情" width="90%" :fullscreen="false" :scroll="true" max-height="78vh">
    <div v-loading="detailLoading">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="所属场景">{{ detailData.sceneCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="链路号">{{ detailData.traceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件编号">{{ detailData.eventId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="接入源">{{ detailData.sourceCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件编码">{{ detailData.eventCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="主体编号">{{ detailData.entityId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="策略编码">{{ detailData.policyCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="执行版本">{{ detailData.versionNo ? `v${detailData.versionNo}` : '-' }}</el-descriptions-item>
        <el-descriptions-item label="决策耗时">{{ detailData.latencyMs ?? '-' }} ms</el-descriptions-item>
        <el-descriptions-item label="最终动作">
          <el-tag :type="getRiskActionTag(detailData.finalAction)" effect="plain">
            {{ getRiskActionLabel(detailData.finalAction) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最终分数">{{ detailData.finalScore ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件时间">{{ detailData.eventTime ? formatDate(detailData.eventTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="命中规则" :span="3">
          <el-space wrap>
            <el-tag v-for="item in detailData.hitRuleCodes || []" :key="item" effect="plain">{{ item }}</el-tag>
            <span v-if="!detailData.hitRuleCodes || detailData.hitRuleCodes.length === 0">-</span>
          </el-space>
        </el-descriptions-item>
      </el-descriptions>

      <ContentWrap class="mt-16px">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="命中明细" name="hit">
            <el-table v-loading="hitLogLoading" :data="hitLogList" max-height="280" @row-click="handleHitRowClick">
              <el-table-column label="顺序" align="center" prop="ruleOrderNo" width="80" />
              <el-table-column label="规则编码" align="center" prop="ruleCode" width="120" />
              <el-table-column label="规则名称" align="center" prop="ruleName" min-width="180" />
              <el-table-column label="结果" align="center" width="100">
                <template #default="scope">
                  <el-tag :type="getHitFlagTag(scope.row.hitFlag)" effect="plain">
                    {{ getHitFlagLabel(scope.row.hitFlag) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="分值" align="center" prop="score" width="90" />
              <el-table-column label="命中原因" align="center" prop="hitReason" min-width="260" show-overflow-tooltip />
            </el-table>
            <el-divider content-position="left">关键值快照</el-divider>
            <JsonEditor :model-value="currentHitValueJson" mode="view" height="260px" />
          </el-tab-pane>
          <el-tab-pane label="标准事件" name="input">
            <JsonEditor :model-value="detailData.inputJson || {}" mode="view" height="420px" />
          </el-tab-pane>
          <el-tab-pane label="特征快照" name="feature">
            <JsonEditor :model-value="detailData.featureSnapshotJson || {}" mode="view" height="420px" />
          </el-tab-pane>
          <el-tab-pane label="决策明细" name="detail">
            <JsonEditor :model-value="detailData.decisionDetailJson || {}" mode="view" height="420px" />
          </el-tab-pane>
        </el-tabs>
      </ContentWrap>
    </div>
  </Dialog>
</template>

<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import * as DecisionLogApi from '@/api/risk/decision-log'
import { ensureObject, getHitFlagLabel, getHitFlagTag, getRiskActionLabel, getRiskActionTag } from './constants'

defineOptions({ name: 'RiskDecisionLogDetailDialog' })

const message = useMessage()

const dialogVisible = ref(false)
const detailLoading = ref(false)
const hitLogLoading = ref(false)
const activeTab = ref('hit')

const createDefaultDetailData = (): DecisionLogApi.DecisionLogDetailVO => ({
  id: undefined,
  traceId: '',
  eventId: '',
  sceneCode: '',
  sourceCode: '',
  eventCode: '',
  entityId: '',
  policyCode: '',
  finalAction: '',
  finalScore: undefined,
  versionNo: 0,
  latencyMs: undefined,
  eventTime: undefined,
  createTime: undefined,
  hitRuleCodes: [],
  inputJson: {},
  featureSnapshotJson: {},
  hitRulesJson: [],
  decisionDetailJson: {}
})

const detailData = ref<DecisionLogApi.DecisionLogDetailVO>(createDefaultDetailData())
const hitLogList = ref<DecisionLogApi.RuleHitLogVO[]>([])
const currentHitLog = ref<DecisionLogApi.RuleHitLogVO>()

const currentHitValueJson = computed(() => ensureObject(currentHitLog.value?.hitValueJson))

const normalizeDetailData = (data?: Partial<DecisionLogApi.DecisionLogDetailVO>): DecisionLogApi.DecisionLogDetailVO => ({
  ...createDefaultDetailData(),
  ...data,
  hitRuleCodes: Array.isArray(data?.hitRuleCodes) ? data?.hitRuleCodes : [],
  inputJson: ensureObject(data?.inputJson),
  featureSnapshotJson: ensureObject(data?.featureSnapshotJson),
  decisionDetailJson: ensureObject(data?.decisionDetailJson)
})

const open = async (id: number, tab: 'hit' | 'input' | 'feature' | 'detail' = 'hit') => {
  dialogVisible.value = true
  detailLoading.value = true
  hitLogLoading.value = true
  activeTab.value = tab
  hitLogList.value = []
  currentHitLog.value = undefined
  try {
    const [detail, hitLogs] = await Promise.all([
      DecisionLogApi.getDecisionLog(id),
      DecisionLogApi.getRuleHitLogList(id).catch(() => [])
    ])
    detailData.value = normalizeDetailData(detail)
    hitLogList.value = Array.isArray(hitLogs) ? hitLogs : []
    currentHitLog.value = hitLogList.value[0]
    if (tab === 'hit' && hitLogList.value.length === 0) {
      message.warning('当前决策暂无规则命中明细')
    }
  } finally {
    detailLoading.value = false
    hitLogLoading.value = false
  }
}

const handleHitRowClick = (row: DecisionLogApi.RuleHitLogVO) => {
  currentHitLog.value = row
}

defineExpose({ open })
</script>
