<template>
  <Dialog v-model="dialogVisible" title="规则详情" width="920px">
    <el-descriptions v-loading="detailLoading" :column="2" border>
      <el-descriptions-item label="主键编号">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="所属场景">{{ detailData.sceneCode }}</el-descriptions-item>
      <el-descriptions-item label="规则编码">{{ detailData.ruleCode }}</el-descriptions-item>
      <el-descriptions-item label="规则名称">{{ detailData.ruleName }}</el-descriptions-item>
      <el-descriptions-item label="规则类型">{{ getRiskRuleTypeLabel(detailData.ruleType) }}</el-descriptions-item>
      <el-descriptions-item label="表达式引擎">{{ getRiskRuleEngineTypeLabel(detailData.engineType) }}</el-descriptions-item>
      <el-descriptions-item label="命中动作">
        <el-tag :type="getRiskRuleHitActionTag(detailData.hitAction)" effect="plain">
          {{ getRiskRuleHitActionLabel(detailData.hitAction) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="优先级 / 分值">{{ formatRiskRuleScore(detailData) }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="detailData.status" />
      </el-descriptions-item>
      <el-descriptions-item label="设计态版本">{{ detailData.version ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="规则表达式" :span="2">
        <div class="whitespace-pre-wrap leading-22px">{{ detailData.exprContent || '-' }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="命中原因模板" :span="2">
        <div class="whitespace-pre-wrap leading-22px">{{ detailData.hitReasonTemplate || '-' }}</div>
      </el-descriptions-item>
      <el-descriptions-item label="创建者">{{ detailData.creator || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">
        {{ detailData.createTime ? formatDate(detailData.createTime) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="更新者">{{ detailData.updater || '-' }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">
        {{ detailData.updateTime ? formatDate(detailData.updateTime) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="规则说明" :span="2">
        <div class="whitespace-pre-wrap leading-22px">{{ detailData.description || '-' }}</div>
      </el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>

<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as RuleApi from '@/api/risk/rule'
import {
  formatRiskRuleScore,
  getRiskRuleEngineTypeLabel,
  getRiskRuleHitActionLabel,
  getRiskRuleHitActionTag,
  getRiskRuleTypeLabel
} from './constants'

defineOptions({ name: 'RiskRuleDetail' })

const dialogVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<RuleApi.RuleVO>({
  id: undefined,
  sceneCode: '',
  ruleCode: '',
  ruleName: '',
  ruleType: 'NORMAL',
  engineType: 'AVIATOR',
  exprContent: '',
  priority: 0,
  hitAction: 'REVIEW',
  riskScore: 0,
  status: 0
})

const open = async (id: number) => {
  dialogVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await RuleApi.getRule(id)
  } finally {
    detailLoading.value = false
  }
}
defineExpose({ open })
</script>
