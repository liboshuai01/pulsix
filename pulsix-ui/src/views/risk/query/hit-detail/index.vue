<template>
  <RiskPageShell v-bind="pageMeta" />
</template>

<script setup lang="ts">
import RiskPageShell from '../../components/RiskPageShell.vue'
import { RISK_DEFAULT_SAMPLE_SCENES, RISK_DEFAULT_STATUS_TEXT } from '../../components/pageMeta'

defineOptions({ name: 'RiskHitDetail' })

const pageMeta = {
  groupTitle: '查询分析',
  pageTitle: '命中明细',
  pageDescription: '查看某次决策命中了哪些规则、命中值和命中原因。',
  nextStep: '对接 rule_hit_log 与决策详情联查，优先展示规则编码、命中动作、命中原因和命中值快照。',
  sampleScenes: RISK_DEFAULT_SAMPLE_SCENES,
  statusText: RISK_DEFAULT_STATUS_TEXT
}
</script>
